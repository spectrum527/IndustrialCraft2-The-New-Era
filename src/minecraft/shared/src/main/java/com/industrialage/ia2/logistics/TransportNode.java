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
        Direction output = getBlockState().getValue(TransportBlock.OUT);
        long time = world.getGameTime();
        if (!cargo.isEmpty() && receivedItemAt != time) {
            for (int i = 0; i < SIDES.length && !cargo.isEmpty(); i++) {
                var side = SIDES[Math.floorMod(time + i, SIDES.length)];
                var mode = nozzle(side);
                if (mode == Nozzle.INJECTION || (mode == Nozzle.NONE && side == output)) {
                    sendItems(world, side, time);
                }
            }
        }
        for (int i = 0; i < SIDES.length && cargo.isEmpty(); i++) {
            var side = SIDES[Math.floorMod(time + i, SIDES.length)];
            var mode = nozzle(side);
            if (mode != Nozzle.EXTRACTION && (mode != Nozzle.NONE || side != output.getOpposite()))
                continue;
            var pos = worldPosition.relative(side);
            if (!world.hasChunkAt(pos)) continue;
            var source = world.getBlockEntity(pos);
            var face = side.getOpposite();
            if (!isPipe(source) && machineOutput(source, face)) {
                var extracted = TransportPlatform.takeItem(world, pos, face, spec.rate());
                if (!extracted.isEmpty()) {
                    cargo = extracted;
                    receivedItemAt = time;
                    setChanged();
                }
            }
        }
    }

    private void sendItems(ServerLevel world, Direction side, long time) {
        var pos = worldPosition.relative(side);
        if (!world.hasChunkAt(pos)) return;
        var target = world.getBlockEntity(pos);
        var face = side.getOpposite();
        int sent;
        if (target instanceof TransportNode next) {
            if (!linksTo(side, next)) return;
            sent = next.insertItem(face, cargo, false, time);
        } else {
            if (!machineInput(target, face)) return;
            sent = TransportPlatform.offerItem(world, pos, face, cargo);
        }
        if (sent > 0) {
            cargo.shrink(sent);
            if (cargo.isEmpty()) cargo = ItemStack.EMPTY;
            setChanged();
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
            case NONE -> acceptsDefaultOutput(getBlockState(), side);
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
        if (spec.kind() != Kind.ITEM_PIPE
                || incoming.isEmpty()
                || !permitsInbound(side)
                || (!cargo.isEmpty() && !TransportPlatform.sameItem(cargo, incoming))) return 0;
        int space = Math.min(spec.capacity(), incoming.getMaxStackSize()) - cargo.getCount();
        int accepted = Math.min(incoming.getCount(), Math.max(0, space));
        if (!simulate && accepted > 0) {
            if (cargo.isEmpty()) {
                cargo = incoming.copy();
                cargo.setCount(accepted);
            } else {
                cargo.grow(accepted);
            }
            receivedItemAt = time;
            setChanged();
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
        if (!simulate) debit(drained);
        return drained;
    }

    void debit(int transferred) {
        if (transferred == 0) return;
        amount -= transferred;
        if (amount == 0) liquid = null;
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
