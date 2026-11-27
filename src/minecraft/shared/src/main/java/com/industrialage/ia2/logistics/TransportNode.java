package com.industrialage.ia2.logistics;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.content.TransportSpec.Kind;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Physical contents, nozzles, pump covers and side valves belonging to one transport block. */
public final class TransportNode extends TransportSaveBase
        implements com.industrialage.ia2.access.MachineAccessTarget,
                net.minecraft.world.MenuProvider {
    static final Direction[] SIDES = Direction.values();

    /** Number of server ticks an item batch glides across one pipe block. */
    public static final int ITEM_TRANSIT_TICKS = 3;

    private static final int SIDE_MASK = (1 << SIDES.length) - 1;

    /** A directional attachment viewed from the pipe. */
    public enum Nozzle {
        NONE(""),
        EXTRACTION("pipe_extraction_nozzle"),
        INJECTION("pipe_injection_nozzle");

        private final String path;

        Nozzle(String path) {
            this.path = path;
        }

        /** Item registry path, or the empty string for an unmodified face. */
        public String path() {
            return path;
        }
    }

    final TransportSpec spec;
    ItemStack cargo = ItemStack.EMPTY;
    final net.minecraft.world.SimpleContainer upgrades;
    FluidKey liquid;
    int amount;
    int closed;
    int lv;
    int mv;
    int extractionNozzles;
    int injectionNozzles;
    private boolean pendingConnections = true;
    long lastFluidTick = Long.MIN_VALUE;
    long receivedItemAt = Long.MIN_VALUE;
    Direction cargoFrom;
    Direction cargoTo;
    Direction externalIntakeFace;
    boolean cargoStalled;
    int cargoSeq;
    Direction fluidFlowDir;
    boolean pendingItemVisualSync;
    boolean pendingFluidVisualSync;
    private FluidKey syncedFluid;
    private int syncedFluidBucket = -1;
    private Direction syncedFlowDir;
    int clientCargoSeq = -1;
    long clientCargoStartTick;
    Direction clientCargoFrom;
    Direction clientCargoTo;
    boolean clientCargoStalled;
    float clientFluidFill;
    long clientFluidRenderNanos;

    /** Constructs an empty node. Native load restores its complete contents and sides together. */
    public TransportNode(BlockPos pos, BlockState state) {
        super(TransportPlatform.type(), pos, state);
        spec = ((TransportBlock) state.getBlock()).spec;
        upgrades =
                new net.minecraft.world.SimpleContainer(spec.path().equals("tank") ? 4 : 0) {
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        TransportNode.this.setChanged();
                    }
                };
    }

    @Override
    public boolean exposesMachineAccess() {
        return spec.kind() == Kind.TANK;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public boolean exchangeHeldFluid(Player player, InteractionHand hand, Direction side) {
        return spec.kind() != Kind.ITEM_PIPE
                && TransportPlatform.containerInteraction(this, side, player, hand);
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
            int id,
            net.minecraft.world.entity.player.Inventory player,
            net.minecraft.world.entity.player.Player who) {
        return spec.kind() == Kind.TANK
                ? new TankMenu(id, player, this, upgrades.getContainerSize() > 0)
                : null;
    }

    /** Live capacity and fluid name for a tank or a fluid pipe, if present. */
    public java.util.Optional<FluidContents> fluidContents() {
        if (spec.kind() == Kind.ITEM_PIPE) return java.util.Optional.empty();
        return java.util.Optional.of(
                liquid == null
                        ? FluidContents.empty(spec.capacity())
                        : new FluidContents(
                                WorkshopPlatform.previewFluid(liquid.registryId()),
                                amount,
                                spec.capacity()));
    }

    void tick(ServerLevel world) {
        if (upgrades.getContainerSize() > 0) {
            if (world.getGameTime() % 8 == 0 && !upgrades.isEmpty()) runUpgrades(world);
            return;
        }
        if (pendingConnections) {
            pendingConnections = false;
            TransportBlock.refreshConnections(world, worldPosition);
        }
        if (spec.kind() == Kind.ITEM_PIPE) {
            tickItems(world);
        } else if (spec.kind() == Kind.FLUID_PIPE && lastFluidTick != world.getGameTime()) {
            LiquidNetwork.tick(world, this);
        }
    }

    private void tickItems(ServerLevel world) {
        Direction output = alignSingleSegmentOutput(world);
        long time = world.getGameTime();
        if (pendingItemVisualSync) {
            pendingItemVisualSync = false;
            if (!cargo.isEmpty()) {
                cargoTo = previewNextHop(world, time);
            }
            syncVisual();
        }
        if (!cargo.isEmpty() && itemReadyToAdvance(time, receivedItemAt)) {
            boolean allowDefaultPush = injectionNozzles == 0;
            for (int i = 0; i < SIDES.length && !cargo.isEmpty(); i++) {
                var side = SIDES[Math.floorMod(time + i, SIDES.length)];
                var mode = nozzle(side);
                if (mode == Nozzle.INJECTION
                        || (allowDefaultPush && mode == Nozzle.NONE && side == output)) {
                    sendToNonPipe(world, side);
                }
            }
            if (!cargo.isEmpty()
                    && canUseImplicitOutlet(world)
                    && hasReadyImplicitOutlet(world, cargo, true)
                    && !ItemPipeRouter.networkHasExplicitOutlet(world, this)) {
                for (int i = 0; i < SIDES.length && !cargo.isEmpty(); i++) {
                    var side = SIDES[Math.floorMod(time + i, SIDES.length)];
                    if (!isReadyImplicitOutlet(world, side, cargo, true)) continue;
                    if (extractionNozzles == 0 && side == output.getOpposite()) {
                        output = redirectOutputFace(world, side);
                    }
                    sendToNonPipe(world, side);
                }
            }
            if (!cargo.isEmpty()) {
                var decision = ItemPipeRouter.decideHop(world, this, cargo, time);
                if (decision.hop() != null) {
                    sendToRoutedPipe(world, decision.hop(), time);
                } else if (decision.allowDirectedFallback()
                        && nozzle(output) == Nozzle.NONE
                        && output != cargoFrom) {
                    sendToDirectedPipe(world, output, time);
                }
            }
            if (!cargo.isEmpty()) {
                Direction nextTarget = previewNextHop(world, time);
                if (!cargoStalled || nextTarget != cargoTo) {
                    cargoStalled = true;
                    cargoTo = nextTarget;
                    syncVisual();
                }
            }
        }
        boolean allowDefaultPull = extractionNozzles == 0;
        for (int i = 0; i < SIDES.length && cargo.isEmpty(); i++) {
            var side = SIDES[Math.floorMod(time + i, SIDES.length)];
            var mode = nozzle(side);
            if (mode != Nozzle.EXTRACTION
                    && (!allowDefaultPull || mode != Nozzle.NONE || side != output.getOpposite()))
                continue;
            var pos = worldPosition.relative(side);
            if (!world.hasChunkAt(pos)) continue;
            var source = world.getBlockEntity(pos);
            var face = side.getOpposite();
            if (!isPipe(source) && machineOutput(source, face)) {
                var extracted = TransportPlatform.takeItem(world, pos, face, spec.rate());
                if (!extracted.isEmpty()) {
                    externalIntakeFace = side;
                    cargo = extracted;
                    onCargoEntered(side, time, true);
                }
            }
        }
    }

    private Direction alignSingleSegmentOutput(ServerLevel world) {
        var state = getBlockState();
        Direction output = state.getValue(TransportBlock.OUT);
        if (extractionNozzles == 0 || injectionNozzles != 0 || hasLinkedItemPipe(world)) {
            return output;
        }
        if (nozzle(output) == Nozzle.NONE && isValidNonPipeItemTarget(world, output)) {
            return output;
        }
        Direction target =
                cargo.isEmpty()
                        ? null
                        : ItemPipeRouter.preferredTarget(
                                candidate -> nozzle(candidate) == Nozzle.EXTRACTION,
                                candidate ->
                                        isValidNonPipeItemTarget(world, candidate)
                                                && TransportPlatform.canAcceptItem(
                                                        world,
                                                        worldPosition.relative(candidate),
                                                        candidate.getOpposite(),
                                                        cargo));
        if (target == null) {
            target =
                    ItemPipeRouter.preferredTarget(
                            candidate -> nozzle(candidate) == Nozzle.EXTRACTION,
                            candidate -> isValidNonPipeItemTarget(world, candidate));
        }
        if (target != null && target != output) {
            return redirectOutputFace(world, target);
        }
        return output;
    }

    private Direction redirectOutputFace(ServerLevel world, Direction target) {
        var state = getBlockState();
        if (!state.hasProperty(TransportBlock.OUT)
                || state.getValue(TransportBlock.OUT) == target) {
            return target;
        }
        world.setBlock(
                worldPosition, state.setValue(TransportBlock.OUT, target), Block.UPDATE_CLIENTS);
        TransportBlock.refreshConnections(world, worldPosition);
        if (!cargo.isEmpty()) {
            cargoTo = target;
            cargoStalled = false;
            syncVisual();
        }
        return target;
    }

    void clearExternalIntake(Direction side) {
        if (side == null || externalIntakeFace == side) {
            externalIntakeFace = null;
        }
    }

    boolean hasConfiguredExplicitOutlet(ServerLevel world, Direction side) {
        if (spec.kind() != Kind.ITEM_PIPE || !open(side)) return false;
        var mode = nozzle(side);
        var state = getBlockState();
        if (mode != Nozzle.INJECTION
                && (injectionNozzles != 0
                        || mode != Nozzle.NONE
                        || !state.hasProperty(TransportBlock.OUT)
                        || side != state.getValue(TransportBlock.OUT))) {
            return false;
        }
        var pos = worldPosition.relative(side);
        if (!world.hasChunkAt(pos)) return false;
        var other = world.getBlockEntity(pos);
        if (isPipe(other)) return false;
        var face = side.getOpposite();
        return machineInput(other, face) && TransportPlatform.hasItemPort(world, pos, face);
    }

    boolean hasConfiguredExplicitOutlet(ServerLevel world) {
        for (var side : SIDES) {
            if (hasConfiguredExplicitOutlet(world, side)) return true;
        }
        return false;
    }

    boolean isReadyExplicitOutlet(ServerLevel world, Direction side, ItemStack stack) {
        if (spec.kind() != Kind.ITEM_PIPE || !open(side) || stack.isEmpty()) return false;
        var mode = nozzle(side);
        var state = getBlockState();
        if (mode != Nozzle.INJECTION
                && (injectionNozzles != 0
                        || mode != Nozzle.NONE
                        || !state.hasProperty(TransportBlock.OUT)
                        || side != state.getValue(TransportBlock.OUT))) {
            return false;
        }
        var pos = worldPosition.relative(side);
        if (!world.hasChunkAt(pos)) return false;
        var other = world.getBlockEntity(pos);
        if (isPipe(other)) return false;
        var face = side.getOpposite();
        return machineInput(other, face)
                && TransportPlatform.canAcceptItem(world, pos, face, stack);
    }

    boolean hasReadyExplicitOutlet(ServerLevel world, ItemStack stack) {
        for (var side : SIDES) {
            if (isReadyExplicitOutlet(world, side, stack)) return true;
        }
        return false;
    }

    boolean canUseImplicitOutlet(ServerLevel world) {
        if (spec.kind() != Kind.ITEM_PIPE || injectionNozzles != 0) return false;
        var state = getBlockState();
        if (!state.hasProperty(TransportBlock.OUT)) return false;
        int linkedPipes = 0;
        for (var side : SIDES) {
            if (isLinkedItemPipe(world, side)) linkedPipes++;
        }
        if (linkedPipes > 1) return false;
        Direction output = state.getValue(TransportBlock.OUT);
        return !hasConfiguredExplicitOutlet(world, output);
    }

    boolean hasConfiguredImplicitOutlet(ServerLevel world) {
        for (var side : SIDES) {
            if (isImplicitOutletFace(world, side, false)) return true;
        }
        return false;
    }

    boolean isImplicitOutletFace(ServerLevel world, Direction side, boolean localDelivery) {
        if (spec.kind() != Kind.ITEM_PIPE || !open(side) || nozzle(side) != Nozzle.NONE) {
            return false;
        }
        if (side == cargoFrom || side == externalIntakeFace) return false;
        var state = getBlockState();
        if (localDelivery
                && extractionNozzles == 0
                && state.hasProperty(TransportBlock.OUT)
                && side == state.getValue(TransportBlock.OUT).getOpposite()
                && cargoFrom == null) {
            return false;
        }
        var pos = worldPosition.relative(side);
        if (!world.hasChunkAt(pos)) return false;
        var other = world.getBlockEntity(pos);
        if (isPipe(other)) return false;
        var face = side.getOpposite();
        return machineInput(other, face) && TransportPlatform.hasItemPort(world, pos, face);
    }

    boolean isReadyImplicitOutlet(
            ServerLevel world, Direction side, ItemStack stack, boolean localDelivery) {
        if (stack.isEmpty() || !isImplicitOutletFace(world, side, localDelivery)) return false;
        var pos = worldPosition.relative(side);
        return TransportPlatform.canAcceptItem(world, pos, side.getOpposite(), stack);
    }

    boolean hasReadyImplicitOutlet(ServerLevel world, ItemStack stack) {
        return hasReadyImplicitOutlet(world, stack, false);
    }

    boolean hasReadyImplicitOutlet(ServerLevel world, ItemStack stack, boolean localDelivery) {
        for (var side : SIDES) {
            if (isReadyImplicitOutlet(world, side, stack, localDelivery)) return true;
        }
        return false;
    }

    boolean isLinkedItemPipe(ServerLevel world, Direction side) {
        if (spec.kind() != Kind.ITEM_PIPE) return false;
        var adjacent = worldPosition.relative(side);
        return world.hasChunkAt(adjacent)
                && world.getBlockEntity(adjacent) instanceof TransportNode next
                && linksTo(side, next);
    }

    boolean hasLinkedItemPipe(ServerLevel world) {
        for (var side : SIDES) {
            if (isLinkedItemPipe(world, side)) return true;
        }
        return false;
    }

    boolean isValidNonPipeItemTarget(ServerLevel world, Direction side) {
        if (spec.kind() != Kind.ITEM_PIPE || !open(side) || nozzle(side) != Nozzle.NONE) {
            return false;
        }
        var adjacent = worldPosition.relative(side);
        if (!world.hasChunkAt(adjacent)) return false;
        var other = world.getBlockEntity(adjacent);
        if (isPipe(other)) return false;
        var face = side.getOpposite();
        return machineInput(other, face) && TransportPlatform.hasItemPort(world, adjacent, face);
    }

    static boolean itemReadyToAdvance(long time, long receivedAt) {
        return receivedAt == Long.MIN_VALUE || time - receivedAt >= ITEM_TRANSIT_TICKS;
    }

    private void onCargoEntered(Direction fromSide, long time, boolean syncNow) {
        receivedItemAt = time;
        cargoFrom = fromSide;
        cargoStalled = false;
        cargoSeq++;
        if (syncNow) {
            cargoTo =
                    level instanceof ServerLevel world
                            ? previewNextHop(world, time)
                            : defaultExit(fromSide);
            pendingItemVisualSync = false;
            setChanged();
            syncVisual();
        } else {
            cargoTo = defaultExit(fromSide);
            pendingItemVisualSync = true;
            setChanged();
        }
    }

    void flushCommittedVisualSync() {
        if (!(level instanceof ServerLevel world)) return;
        if (pendingItemVisualSync) {
            pendingItemVisualSync = false;
            if (!cargo.isEmpty()) {
                cargoTo = previewNextHop(world, world.getGameTime());
            }
            syncVisual();
        }
        if (pendingFluidVisualSync) {
            pendingFluidVisualSync = false;
            syncFluidVisualIfNeeded();
        }
    }

    private Direction previewNextHop(ServerLevel world, long time) {
        var state = getBlockState();
        Direction output =
                state.hasProperty(TransportBlock.OUT)
                        ? state.getValue(TransportBlock.OUT)
                        : Direction.NORTH;
        for (var side : SIDES) {
            if (isReadyExplicitOutlet(world, side, cargo)) {
                return side;
            }
        }
        if (canUseImplicitOutlet(world)
                && hasReadyImplicitOutlet(world, cargo, true)
                && !ItemPipeRouter.networkHasExplicitOutlet(world, this)) {
            for (var side : SIDES) {
                if (isReadyImplicitOutlet(world, side, cargo, true)) {
                    return side;
                }
            }
        }
        Direction routed = ItemPipeRouter.previewHop(world, this, cargo, time);
        if (routed != null) return routed;
        if (cargoFrom != null) {
            Direction straight = cargoFrom.getOpposite();
            if (isVisualExitFace(world, state, straight, output)) return straight;
        }
        if (output != cargoFrom && isVisualExitFace(world, state, output, output)) return output;
        for (var side : SIDES) {
            if (side == cargoFrom) continue;
            if (isVisualExitFace(world, state, side, output)) return side;
        }
        return null;
    }

    private boolean isVisualExitFace(
            ServerLevel world, BlockState state, Direction side, Direction output) {
        var pos = worldPosition.relative(side);
        if (!world.hasChunkAt(pos)) return false;
        var other = world.getBlockEntity(pos);
        if (other instanceof TransportNode next && next.spec.kind() != Kind.TANK) {
            return linksTo(side, next);
        }
        var mode = nozzle(side);
        return (mode == Nozzle.INJECTION
                        || (injectionNozzles == 0 && mode == Nozzle.NONE && side == output))
                && TransportBlock.connected(state, side);
    }

    private Direction defaultExit(Direction fromSide) {
        var state = getBlockState();
        if (fromSide != null) {
            Direction straight = fromSide.getOpposite();
            if (TransportBlock.connected(state, straight)) return straight;
        }
        if (state.hasProperty(TransportBlock.OUT)) {
            Direction output = state.getValue(TransportBlock.OUT);
            if (output != fromSide && TransportBlock.connected(state, output)) return output;
        }
        for (var side : SIDES) {
            if (side != fromSide && TransportBlock.connected(state, side)) return side;
        }
        return null;
    }

    void syncVisual() {
        if (level instanceof ServerLevel world && spec.kind() != Kind.TANK) {
            var state = getBlockState();
            world.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    void noteFluidFlow(Direction direction) {
        fluidFlowDir = direction;
    }

    void syncFluidVisualIfNeeded() {
        if (spec.kind() != Kind.FLUID_PIPE || !(level instanceof ServerLevel)) return;
        int bucket =
                (liquid == null || amount <= 0)
                        ? 0
                        : (amount >= spec.capacity()
                                ? 12
                                : Math.max(1, (amount * 11) / spec.capacity()));
        Direction dir = (liquid == null || amount <= 0) ? null : fluidFlowDir;
        if (!java.util.Objects.equals(liquid, syncedFluid)
                || bucket != syncedFluidBucket
                || dir != syncedFlowDir) {
            syncedFluid = liquid;
            syncedFluidBucket = bucket;
            syncedFlowDir = dir;
            syncVisual();
        }
    }

    void applyClientSync(
            int extraction,
            int injection,
            ItemStack syncedCargo,
            Direction from,
            Direction to,
            int seq,
            boolean stalled,
            FluidKey syncedLiquid,
            int syncedAmount,
            Direction flowDir) {
        restoreNozzles(extraction, injection);
        if (spec.kind() == Kind.ITEM_PIPE) {
            if (syncedCargo.isEmpty()) {
                cargo = ItemStack.EMPTY;
                clientCargoFrom = null;
                clientCargoTo = null;
                clientCargoStalled = false;
            } else {
                cargo = syncedCargo;
                if (seq != clientCargoSeq) {
                    clientCargoSeq = seq;
                    clientCargoStartTick = level != null ? level.getGameTime() : 0L;
                }
                clientCargoFrom = from;
                clientCargoTo = to;
                clientCargoStalled = stalled;
            }
        } else if (spec.kind() == Kind.FLUID_PIPE) {
            liquid = syncedLiquid;
            amount = syncedAmount;
            fluidFlowDir = flowDir;
        }
    }

    /** Whether this block entity has nozzles, moving cargo, or visible fluid to render. */
    public boolean hasVisualRender() {
        if (hasNozzles()) return true;
        if (spec.kind() == Kind.ITEM_PIPE) return !cargo.isEmpty();
        if (spec.kind() == Kind.FLUID_PIPE) {
            return (liquid != null && amount > 0) || clientFluidFill > 0.005F;
        }
        return false;
    }

    /** Live synced cargo stack visible inside this item pipe on the client. */
    public ItemStack clientCargo() {
        return cargo;
    }

    /** Smooth normalized transit progress in [0, 1] for the item moving through this pipe. */
    public float clientCargoProgress(float partialTick) {
        return PipeVisuals.itemProgress(this, partialTick);
    }

    /** Entry face of the synced cargo moving through this item pipe, if known. */
    public Direction clientCargoFrom() {
        return clientCargoFrom;
    }

    /** Exit face of the synced cargo moving through this item pipe, if known. */
    public Direction clientCargoTo() {
        return clientCargoTo;
    }

    /** Monotonic transit sequence number for client interpolation handoffs. */
    public int clientCargoSeq() {
        return clientCargoSeq;
    }

    /** Whether this fluid pipe currently holds visible synced fluid. */
    public boolean hasFluidVisual() {
        return spec.kind() == Kind.FLUID_PIPE && liquid != null && amount > 0;
    }

    /** Target fluid fill fraction in [0, 1] for this fluid pipe. */
    public float clientFluidFillFraction() {
        if (spec.kind() != Kind.FLUID_PIPE || liquid == null || amount <= 0) return 0F;
        return Math.min(1F, (float) amount / Math.max(1, spec.capacity()));
    }

    /** Dominant synced fluid flow direction, if known. */
    public Direction clientFluidFlowDirection() {
        return fluidFlowDir;
    }

    private void sendToNonPipe(ServerLevel world, Direction side) {
        var pos = worldPosition.relative(side);
        if (!world.hasChunkAt(pos)) return;
        var target = world.getBlockEntity(pos);
        if (target instanceof TransportNode next && next.spec.kind() != Kind.TANK) return;
        var face = side.getOpposite();
        if (!machineInput(target, face)) return;
        int sent = TransportPlatform.offerItem(world, pos, face, cargo);
        consumeSent(sent);
    }

    private void sendToRoutedPipe(ServerLevel world, Direction side, long time) {
        var pos = worldPosition.relative(side);
        if (!world.hasChunkAt(pos)) return;
        if (!(world.getBlockEntity(pos) instanceof TransportNode next) || !linksTo(side, next)) {
            return;
        }
        int sent = next.receiveFromPipe(side.getOpposite(), cargo, time);
        consumeSent(sent);
    }

    private void sendToDirectedPipe(ServerLevel world, Direction side, long time) {
        var pos = worldPosition.relative(side);
        if (!world.hasChunkAt(pos)) return;
        if (!(world.getBlockEntity(pos) instanceof TransportNode next) || !linksTo(side, next)) {
            return;
        }
        int sent = next.insertItem(side.getOpposite(), cargo, false, time, false);
        if (sent > 0) {
            next.flushCommittedVisualSync();
        }
        consumeSent(sent);
    }

    private void consumeSent(int sent) {
        if (sent > 0) {
            cargo.shrink(sent);
            if (cargo.isEmpty()) {
                cargo = ItemStack.EMPTY;
                cargoFrom = null;
                cargoTo = null;
                cargoStalled = false;
            } else {
                cargoStalled = false;
            }
            setChanged();
            syncVisual();
        }
    }

    static boolean isPipe(BlockEntity entity) {
        return entity instanceof TransportNode node && node.spec.kind() != Kind.TANK;
    }

    static boolean machineInput(BlockEntity machine, Direction face) {
        return machine == null || MachineAccess.allowsInput(machine, face);
    }

    static boolean machineOutput(BlockEntity machine, Direction face) {
        return machine == null || MachineAccess.allowsOutput(machine, face);
    }

    boolean linksTo(Direction side, TransportNode other) {
        return spec.kind() == other.spec.kind()
                && exposesPipeFace(side)
                && other.exposesPipeFace(side.getOpposite());
    }

    boolean exposesPipeFace(Direction side) {
        return open(side) && nozzle(side) == Nozzle.NONE && pump(side) == 0;
    }

    /** Returns the configured attachment on a world face. */
    public Nozzle nozzle(Direction side) {
        if (side == null) return Nozzle.NONE;
        int mask = faceMask(side);
        if ((extractionNozzles & mask) != 0) return Nozzle.EXTRACTION;
        return (injectionNozzles & mask) != 0 ? Nozzle.INJECTION : Nozzle.NONE;
    }

    /** Whether this pipe has any attached nozzles. */
    public boolean hasNozzles() {
        return (extractionNozzles | injectionNozzles) != 0;
    }

    /** Whether automation may put the transported medium into this world face of the pipe. */
    public boolean permitsInbound(Direction side) {
        if (side == null) return spec.kind() == Kind.TANK;
        if (!open(side)) return false;
        return switch (nozzle(side)) {
            case EXTRACTION -> true;
            case INJECTION -> false;
            case NONE -> acceptsDefaultInput(getBlockState(), side);
        };
    }

    /** Whether automation may remove the transported medium from this world face of the pipe. */
    public boolean permitsOutbound(Direction side) {
        if (side == null) return spec.kind() == Kind.TANK;
        if (!open(side) || pump(side) > 0) return false;
        return switch (nozzle(side)) {
            case EXTRACTION -> false;
            case INJECTION -> true;
            case NONE ->
                    (spec.kind() != Kind.ITEM_PIPE || injectionNozzles == 0)
                            && acceptsDefaultOutput(getBlockState(), side);
        };
    }

    /** Whether the fitting forbids an adjacent machine upgrade in its reverse direction. */
    public boolean blocksUpgrade(Direction pipeFace, boolean ejectFromMachine, boolean fluid) {
        if (spec.kind() != (fluid ? Kind.FLUID_PIPE : Kind.ITEM_PIPE)) return false;
        return switch (nozzle(pipeFace)) {
            case EXTRACTION -> !ejectFromMachine;
            case INJECTION -> ejectFromMachine;
            case NONE -> false;
        };
    }

    static boolean acceptsDefaultInput(BlockState state, Direction side) {
        return !state.hasProperty(TransportBlock.OUT) || side != state.getValue(TransportBlock.OUT);
    }

    static boolean acceptsDefaultOutput(BlockState state, Direction side) {
        return !state.hasProperty(TransportBlock.OUT) || side == state.getValue(TransportBlock.OUT);
    }

    boolean installNozzle(Direction face, Nozzle mode) {
        if (spec.kind() == Kind.TANK
                || !open(face)
                || pump(face) != 0
                || nozzle(face) != Nozzle.NONE) return false;
        if (mode == Nozzle.EXTRACTION) extractionNozzles |= faceMask(face);
        else if (mode == Nozzle.INJECTION) injectionNozzles |= faceMask(face);
        else return false;
        setChanged();
        return true;
    }

    Nozzle removeNozzle(Direction face) {
        var mode = nozzle(face);
        if (mode != Nozzle.NONE) {
            int keep = ~faceMask(face);
            extractionNozzles &= keep;
            injectionNozzles &= keep;
            setChanged();
        }
        return mode;
    }

    void restoreNozzles(int extraction, int injection) {
        if (spec.kind() == Kind.TANK) return;
        int free = SIDE_MASK & ~(lv | mv);
        extractionNozzles = extraction & free;
        injectionNozzles = injection & free & ~extractionNozzles;
        pendingConnections = true;
    }

    private static int faceMask(Direction side) {
        return 1 << side.ordinal();
    }

    int insertItem(Direction side, ItemStack incoming, boolean simulate, long time) {
        return insertItem(side, incoming, simulate, time, true);
    }

    int insertItem(
            Direction side, ItemStack incoming, boolean simulate, long time, boolean external) {
        if (spec.kind() != Kind.ITEM_PIPE
                || incoming.isEmpty()
                || !permitsInbound(side)
                || (!cargo.isEmpty() && !TransportPlatform.sameItem(cargo, incoming))) return 0;
        int space = Math.min(spec.capacity(), incoming.getMaxStackSize()) - cargo.getCount();
        int accepted = Math.min(incoming.getCount(), Math.max(0, space));
        if (!simulate && accepted > 0) {
            if (external && side != null) {
                externalIntakeFace = side;
            }
            boolean wasEmpty = cargo.isEmpty();
            if (wasEmpty) {
                cargo = incoming.copy();
                cargo.setCount(accepted);
                onCargoEntered(side, time, false);
            } else {
                cargo.grow(accepted);
                pendingItemVisualSync = true;
                setChanged();
            }
        }
        return accepted;
    }

    boolean canAcceptRoutedPipeItem(Direction side, ItemStack incoming) {
        if (spec.kind() != Kind.ITEM_PIPE
                || incoming.isEmpty()
                || !exposesPipeFace(side)
                || (!cargo.isEmpty() && !TransportPlatform.sameItem(cargo, incoming))) return false;
        return Math.min(spec.capacity(), incoming.getMaxStackSize()) - cargo.getCount() > 0;
    }

    int receiveFromPipe(Direction side, ItemStack incoming, long time) {
        if (!canAcceptRoutedPipeItem(side, incoming)) return 0;
        int space = Math.min(spec.capacity(), incoming.getMaxStackSize()) - cargo.getCount();
        int accepted = Math.min(incoming.getCount(), Math.max(0, space));
        if (accepted > 0) {
            boolean wasEmpty = cargo.isEmpty();
            if (wasEmpty) {
                cargo = incoming.copy();
                cargo.setCount(accepted);
                onCargoEntered(side, time, true);
            } else {
                cargo.grow(accepted);
                setChanged();
                syncVisual();
            }
        }
        return accepted;
    }

    int pump(Direction side) {
        if (side == null) return 0;
        int mask = 1 << side.ordinal();
        return (lv & mask) != 0 ? 32 : (mv & mask) != 0 ? 128 : 0;
    }

    boolean open(Direction side) {
        return side == null || (closed & (1 << side.ordinal())) == 0;
    }

    int fill(Direction side, FluidKey key, int offered, boolean simulate) {
        if (offered <= 0
                || spec.kind() == Kind.ITEM_PIPE
                || !open(side)
                || (liquid != null && !liquid.equals(key))) return 0;
        int accepted = Math.min(offered, spec.capacity() - amount);
        if (!simulate && accepted > 0) {
            liquid = key;
            amount += accepted;
            if (side != null) {
                noteFluidFlow(side.getOpposite());
                pendingFluidVisualSync = true;
            }
            setChanged();
        }
        return accepted;
    }

    int drain(Direction side, FluidKey key, int requested, boolean simulate) {
        if (requested <= 0
                || liquid == null
                || !open(side)
                || pump(side) != 0
                || (key != null && !liquid.equals(key))) return 0;
        int drained = Math.min(amount, requested);
        if (!simulate && drained > 0) {
            if (side != null) {
                noteFluidFlow(side);
                pendingFluidVisualSync = true;
            }
            debit(drained);
        }
        return drained;
    }

    void debit(int transferred) {
        if (transferred == 0) return;
        amount -= transferred;
        if (amount == 0) {
            liquid = null;
            fluidFlowDir = null;
        }
        setChanged();
    }

    boolean cover(Direction face, int rate) {
        if (spec.kind() != Kind.FLUID_PIPE
                || pump(face) != 0
                || nozzle(face) != Nozzle.NONE
                || !open(face)) return false;
        int mask = 1 << face.ordinal();
        if (rate == 32) lv |= mask;
        else if (rate == 128) mv |= mask;
        else return false;
        setChanged();
        return true;
    }

    int uncover(Direction side) {
        int rate = pump(side);
        if (rate != 0) {
            int mask = ~(1 << side.ordinal());
            lv &= mask;
            mv &= mask;
            setChanged();
        }
        return rate;
    }

    boolean toggle(Direction face) {
        if (spec.kind() != Kind.FLUID_PIPE) return false;
        if (open(face) && (nozzle(face) != Nozzle.NONE || pump(face) != 0)) return false;
        closed ^= 1 << face.ordinal();
        setChanged();
        return true;
    }

    static boolean isFluidUpgrade(ItemStack item) {
        String id = TransportPlatform.itemId(item);
        return id.equals("ic2:fluid_ejector_upgrade") || id.equals("ic2:fluid_pulling_upgrade");
    }

    private void runUpgrades(ServerLevel level) {
        for (int i = 0; i < upgrades.getContainerSize(); i++) {
            var upgrade = upgrades.getItem(i);
            if (!isFluidUpgrade(upgrade)) continue;
            boolean eject = TransportPlatform.itemId(upgrade).equals("ic2:fluid_ejector_upgrade");
            int remaining =
                    Math.min(
                            1000,
                            100
                                    * com.industrialage.ia2.content.ProcessingUpgrades
                                            .transferLimit(upgrade.getCount()));
            for (var side : com.industrialage.ia2.world.MachineAutomation.faces(upgrade)) {
                var neighbor = worldPosition.relative(side);
                if (remaining == 0) break;
                if (!level.hasChunkAt(neighbor)
                        || !MachineAccess.allowsUpgrade(this, side, eject, true)) continue;
                if (eject && liquid != null) {
                    int moved =
                            TransportPlatform.offerFluid(
                                    level,
                                    neighbor,
                                    side.getOpposite(),
                                    liquid,
                                    Math.min(amount, remaining),
                                    false);
                    debit(moved);
                    remaining -= moved;
                } else if (!eject && amount < spec.capacity()) {
                    var batch =
                            TransportPlatform.takeFluid(
                                    level,
                                    neighbor,
                                    side.getOpposite(),
                                    liquid,
                                    Math.min(remaining, spec.capacity() - amount));
                    if (batch != null) remaining -= fill(null, batch.key(), batch.amount(), false);
                }
            }
        }
    }

    void dropContents(ServerLevel level) {
        if (!upgrades.isEmpty())
            net.minecraft.world.Containers.dropContents(level, worldPosition, upgrades);
        if (!cargo.isEmpty()) {
            Block.popResource(level, worldPosition, cargo);
            cargo = ItemStack.EMPTY;
        }
        for (Direction side : SIDES) {
            int rate = uncover(side);
            if (rate > 0)
                Block.popResource(level, worldPosition, TransportPlatform.coverItem(rate));
            var nozzle = removeNozzle(side);
            if (nozzle != Nozzle.NONE)
                Block.popResource(level, worldPosition, TransportPlatform.nozzleItem(nozzle));
        }
    }
}
