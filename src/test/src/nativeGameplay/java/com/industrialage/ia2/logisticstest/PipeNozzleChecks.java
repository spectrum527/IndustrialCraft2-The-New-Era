package com.industrialage.ia2.logisticstest;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.logistics.TransportBlock;
import com.industrialage.ia2.logistics.TransportNode;
import com.industrialage.ia2.logistics.TransportNode.Nozzle;
import com.industrialage.ia2.wrenchtest.WrenchHits;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Dedicated-server fixture for every native pipe between placed machines. */
public final class PipeNozzleChecks {
    /** Access to the loader's actual transport capabilities for each test medium. */
    public interface Automation {
        /** Reads the cargo from the native item capability. */
        ItemStack itemCargo(ServerLevel level, BlockPos pos);

        /** Seeds the bottler through an unmodified adjacent fluid capability. */
        int seedWater(ServerLevel level, BlockPos pos, int amount);

        /** Attempts reverse fill and drain against both ends of a loaded buffer. */
        boolean reverseRejected(ServerLevel level, BlockPos pos, boolean fluid, ItemStack sample);
    }

    private record Line(String id, BlockPos pos, boolean fluid, int rate, int total) {
        BlockPos source() {
            return pos.north();
        }

        BlockPos sink() {
            return pos.south();
        }

        int buffered() {
            return fluid ? total : rate;
        }
    }

    private static final int WORKSHOP_OUTPUT_SLOT = 9;
    private final Map<BlockPos, Container> inventories = new HashMap<>();
    private final List<Line> lines = new ArrayList<>();
    private final Set<String> measured = new HashSet<>();
    private final Function<String, Block> blocks;
    private final Function<String, ItemStack> items;
    private final Automation automation;
    private boolean warm;
    private boolean complete;
    private ServerPlayer operator;
    private long droppedAt = Long.MIN_VALUE;

    /** Builds the native sixteen-pipe matrix. Its source and sink blocks are idle IC2 machines. */
    public PipeNozzleChecks(
            BlockPos origin,
            Function<String, Block> blocks,
            Function<String, ItemStack> items,
            Automation automation) {
        this.blocks = blocks;
        this.items = items;
        this.automation = automation;
        int[] itemRates = {1, 4, 16, 64};
        int[][] fluidRates = {{20, 40, 120, 240}, {40, 80, 240, 480}};
        String[] metals = {"bronze", "steel"};
        String[] sizes = {"tiny", "small", "medium", "large"};
        for (int kind = 0; kind < 2; kind++) {
            for (int metal = 0; metal < 2; metal++) {
                for (int size = 0; size < 4; size++) {
                    boolean fluid = kind == 1;
                    int rate = fluid ? fluidRates[metal][size] : itemRates[size];
                    var pos = origin.offset(size * 3, 0, (kind * 2 + metal) * 3);
                    String id =
                            metals[metal] + (fluid ? "_fluid_pipe_" : "_item_pipe_") + sizes[size];
                    lines.add(
                            new Line(
                                    id,
                                    pos,
                                    fluid,
                                    rate,
                                    fluid ? rate * 3 + 7 : Math.min(64, rate + 3)));
                }
            }
        }
    }

    /** Fits both ends by normal player interaction, then leaves the machines under backpressure. */
    public void place(ServerLevel level, ServerPlayer player, BiConsumer<String, Boolean> check) {
        operator = player;
        for (var line : lines) {
            var pos = line.pos;
            var state = blocks.apply(line.id).defaultBlockState();
            if (!line.fluid) state = state.setValue(BlockStateProperties.FACING, Direction.EAST);
            level.setBlockAndUpdate(pos, state);
            level.setBlockAndUpdate(
                    line.source(),
                    blocks.apply(line.fluid ? "fluid_bottler" : "induction_furnace")
                            .defaultBlockState());
            level.setBlockAndUpdate(
                    line.sink(),
                    blocks.apply(line.fluid ? "ore_washing_plant" : "metal_former")
                            .defaultBlockState());
            var source = device(level, line.source());
            var sink = device(level, line.sink());
            face(source, Direction.SOUTH, MachineAccess.Mode.OUTPUT);
            face(sink, Direction.NORTH, MachineAccess.Mode.BLOCKED);
            // The source occupies the adjacent block, so hit the exposed top of its arm.
            check.accept(
                    "connected-source-arm-" + line.id,
                    TransportBlock.connected(level.getBlockState(pos), Direction.NORTH));
            var northArm = northArm(level, pos);
            var extracted = click(level, player, northArm, Nozzle.EXTRACTION.path(), 2);
            var injected =
                    click(
                            level,
                            player,
                            WrenchHits.center(level, pos, Direction.SOUTH),
                            Nozzle.INJECTION.path(),
                            2);
            var rejected = click(level, player, northArm, Nozzle.INJECTION.path(), 1);
            var pipe = (TransportNode) level.getBlockEntity(pos);
            check.accept(
                    "install-and-exclusive-" + line.id,
                    extracted == 1
                            && injected == 1
                            && rejected == 1
                            && pipe.nozzle(Direction.NORTH) == Nozzle.EXTRACTION
                            && pipe.nozzle(Direction.SOUTH) == Nozzle.INJECTION
                            && pipe.nozzle(Direction.UP) == Nozzle.NONE);
            check.accept(
                    "pipe-injection-face-rejects-input-while-empty-" + line.id,
                    automation.reverseRejected(
                            level, pos, line.fluid, new ItemStack(Items.COPPER_INGOT)));
            if (line.fluid) {
                check.accept(
                        "nozzle-and-pump-exclusive-" + line.id,
                        click(level, player, northArm, "pump_cover_lv", 1) == 1);
                check.accept(
                        "seed-fluid-machine-" + line.id,
                        automation.seedWater(level, line.source(), line.total) == line.total);
            } else {
                linkInventories(level, player, line);
                inventories
                        .get(line.source())
                        .setItem(
                                WORKSHOP_OUTPUT_SLOT,
                                new ItemStack(Items.COPPER_INGOT, line.total));
            }
            check.accept(
                    "blocked-face-no-arm-" + line.id,
                    !TransportBlock.connected(level.getBlockState(pos), Direction.SOUTH)
                            && level.getBlockState(pos)
                                            .getShape(level, pos, CollisionContext.empty())
                                            .max(Direction.Axis.Z)
                                    < 1);
        }
    }

    /** Waits for bounded first-tick draw and exact blocked contents. */
    public boolean waitForBackpressure(ServerLevel level, BiConsumer<String, Boolean> check) {
        if (warm) return true;
        boolean all = true;
        for (var line : lines) {
            int cargo = buffered(level, line);
            if (cargo > 0 && measured.add(line.id)) {
                check.accept(
                        "native-first-tick-rate-" + line.id,
                        cargo == line.rate && sink(level, line) == 0);
            }
            all &= cargo == line.buffered();
        }
        if (!all) return false;
        verifyBackpressure(level, "warm", check);
        warm = true;
        return true;
    }

    /** Verifies persisted faces and cargo, then opens the adjacent machines for injection. */
    public void letDeliver(
            ServerLevel level,
            ServerPlayer player,
            String phase,
            BiConsumer<String, Boolean> check) {
        operator = player;
        for (var line : lines) {
            if (!line.fluid && !inventories.containsKey(line.source()))
                linkInventories(level, player, line);
        }
        verifyBackpressure(level, phase, check);
        for (var line : lines) {
            face(device(level, line.sink()), Direction.NORTH, MachineAccess.Mode.INPUT);
            check.accept(
                    phase + "-arm-restored-" + line.id,
                    TransportBlock.connected(level.getBlockState(line.pos), Direction.SOUTH)
                            && level.getBlockState(line.pos)
                                            .getShape(level, line.pos, CollisionContext.empty())
                                            .max(Direction.Axis.Z)
                                    == 1);
        }
    }

    /** Verifies every unit reaches the real adjacent machine after the blocked face opens. */
    public boolean delivered(ServerLevel level, BiConsumer<String, Boolean> check) {
        if (complete) return true;
        var detached = lines.get(8);
        var destroyed = lines.get(3);
        if (droppedAt == Long.MIN_VALUE) {
            if (lines.stream().anyMatch(line -> sink(level, line) != line.total)) return false;
            for (var line : lines) {
                check.accept(
                        "successful-machine-injection-" + line.id,
                        sink(level, line) == line.total
                                && source(level, line) == 0
                                && buffered(level, line) == 0);
            }
            detachLoadedNozzle(level, detached, check);
            level.setBlockAndUpdate(destroyed.pos.below(), Blocks.STONE.defaultBlockState());
            check.accept(
                    "ordinary-pipe-removal", level.destroyBlock(destroyed.pos, true, operator));
            droppedAt = level.getGameTime();
            return false;
        }
        if (level.getGameTime() <= droppedAt + 1) return false;
        check.accept(
                "crowbar-only-drops-one-nozzle-and-keeps-contents",
                countDrops(level, detached.pos, Nozzle.EXTRACTION.path()) == 1
                        && countDrops(level, detached.pos, Nozzle.INJECTION.path()) == 0
                        && buffered(level, detached) == 13);
        check.accept(
                "pipe-removal-drops-one-of-each-nozzle",
                countDrops(level, destroyed.pos, Nozzle.EXTRACTION.path()) == 1
                        && countDrops(level, destroyed.pos, Nozzle.INJECTION.path()) == 1);
        complete = true;
        return true;
    }

    private void detachLoadedNozzle(
            ServerLevel level, Line line, BiConsumer<String, Boolean> check) {
        var pos = line.pos;
        var pipe = (TransportNode) level.getBlockEntity(pos);
        var source = level.getBlockState(line.source());
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(line.source(), Blocks.AIR.defaultBlockState());
        check.accept(
                "nozzle-remains-after-neighbor-disconnect",
                pipe.nozzle(Direction.NORTH) == Nozzle.EXTRACTION
                        && !TransportBlock.connected(level.getBlockState(pos), Direction.NORTH));
        level.setBlockAndUpdate(line.source(), source);
        face(device(level, line.source()), Direction.SOUTH, MachineAccess.Mode.OUTPUT);
        check.accept(
                "attached-nozzle-reconnects-after-neighbor-replacement",
                TransportBlock.connected(level.getBlockState(pos), Direction.NORTH));
        check.accept("fill-detachment-sample", automation.seedWater(level, pos, 13) == 13);
        var sink = device(level, line.sink());
        face(sink, Direction.NORTH, MachineAccess.Mode.BLOCKED);
        var hit = northArm(level, pos);
        click(level, operator, hit, "crowbar", 1);
        click(level, operator, hit, "crowbar", 1);
        check.accept(
                "crowbar-detaches-exact-world-face",
                pipe.nozzle(Direction.NORTH) == Nozzle.NONE
                        && pipe.nozzle(Direction.SOUTH) == Nozzle.INJECTION
                        && buffered(level, line) == 13);
        var west = WrenchHits.center(level, pos, Direction.WEST);
        check.accept(
                "existing-pump-prevents-nozzle-install",
                click(level, operator, west, "pump_cover_lv", 1) == 0
                        && click(level, operator, west, Nozzle.INJECTION.path(), 1) == 1
                        && pipe.nozzle(Direction.WEST) == Nozzle.NONE);
    }

    private static BlockHitResult northArm(ServerLevel level, BlockPos pos) {
        double top =
                level.getBlockState(pos)
                        .getShape(level, pos, CollisionContext.empty())
                        .max(Direction.Axis.Y);
        var point = new Vec3(pos.getX() + .5, pos.getY() + top, pos.getZ() + .125);
        return new BlockHitResult(point, Direction.UP, pos, false);
    }

    private int countDrops(ServerLevel level, BlockPos pos, String id) {
        var item = items.apply(id).getItem();
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.5)).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private void verifyBackpressure(
            ServerLevel level, String phase, BiConsumer<String, Boolean> check) {
        for (var line : lines) {
            var pipe = (TransportNode) level.getBlockEntity(line.pos);
            check.accept(
                    phase + "-nozzle-cargo-and-modes-" + line.id,
                    pipe.nozzle(Direction.NORTH) == Nozzle.EXTRACTION
                            && pipe.nozzle(Direction.SOUTH) == Nozzle.INJECTION
                            && !TransportBlock.connected(
                                    level.getBlockState(line.pos), Direction.SOUTH)
                            && buffered(level, line) == line.buffered()
                            && source(level, line) == line.total - line.buffered()
                            && sink(level, line) == 0);
            check.accept(
                    phase + "-rejects-reverse-native-capabilities-" + line.id,
                    automation.reverseRejected(
                            level, line.pos, line.fluid, new ItemStack(Items.COPPER_INGOT)));
        }
    }

    private int buffered(ServerLevel level, Line line) {
        return line.fluid
                ? ((TransportNode) level.getBlockEntity(line.pos))
                        .fluidContents()
                        .orElseThrow()
                        .amount()
                : automation.itemCargo(level, line.pos).getCount();
    }

    private static WorkshopNode device(ServerLevel level, BlockPos pos) {
        return (WorkshopNode) level.getBlockEntity(pos);
    }

    private void linkInventories(ServerLevel level, ServerPlayer player, Line line) {
        for (var pos : new BlockPos[] {line.source(), line.sink()}) {
            inventories.put(
                    pos,
                    device(level, pos)
                            .createMenu(0, player.getInventory(), player)
                            .getSlot(0)
                            .container);
        }
    }

    private int source(ServerLevel level, Line line) {
        return line.fluid
                ? device(level, line.source()).fluidContents().get(0).amount()
                : inventories.get(line.source()).getItem(WORKSHOP_OUTPUT_SLOT).getCount();
    }

    private int sink(ServerLevel level, Line line) {
        return line.fluid
                ? device(level, line.sink()).fluidContents().get(0).amount()
                : inventories.get(line.sink()).getItem(0).getCount();
    }

    private static void face(WorkshopNode device, Direction direction, MachineAccess.Mode mode) {
        for (int localFace = 0; localFace < 6; localFace++) {
            if (MachineAccess.worldSide(MachineAccess.front(device), localFace) == direction) {
                if (!MachineAccess.set(device, localFace, mode.ordinal()))
                    throw new AssertionError(
                            "Native mode rejected for "
                                    + direction
                                    + " on "
                                    + device.getBlockPos());
                return;
            }
        }
        throw new AssertionError("No native orientation for " + direction);
    }

    private int click(
            ServerLevel level, ServerPlayer player, BlockHitResult hit, String item, int count) {
        var held = items.apply(item);
        held.setCount(count);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var pos = hit.getBlockPos();
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        player.gameMode.useItemOn(player, level, held, InteractionHand.MAIN_HAND, hit);
        return player.getItemInHand(InteractionHand.MAIN_HAND).getCount();
    }
}
