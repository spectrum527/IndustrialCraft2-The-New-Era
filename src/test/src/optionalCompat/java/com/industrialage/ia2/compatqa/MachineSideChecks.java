package com.industrialage.ia2.compatqa;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.logistics.CellItem;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Installed server checks for real native capabilities, vanilla hoppers and player-held fluids. */
final class MachineSideChecks {
    interface Transfer {
        int insert(int amount, boolean simulate);

        int extract(int amount, boolean simulate);
    }

    private MachineSideChecks() {}

    static void run(ServerLevel level, ServerPlayer player, List<String> report) {
        var pos = new BlockPos(6, 105, 6);
        player.setPos(6.5, 105, 7.5);
        level.setBlockAndUpdate(pos, CompatHost.block("iron_storage_box").defaultBlockState());
        directions(level, pos, false);
        hoppers(level, pos);
        level.setBlockAndUpdate(pos, CompatHost.block("tank").defaultBlockState());
        directions(level, pos, true);
        var tank = level.getBlockEntity(pos);
        for (int face = 0; face < 6; face++)
            require(MachineAccess.set(tank, face, 1), "block tank face");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        require(
                CellItem.interactWithBlock(
                                level, pos, Direction.UP, player, InteractionHand.MAIN_HAND)
                        && player.getMainHandItem().is(Items.BUCKET),
                "hand pours through blocked automation face");
        player.setItemInHand(
                InteractionHand.MAIN_HAND, new ItemStack(CompatHost.item("empty_cell")));
        require(
                CellItem.interactWithBlock(
                                level, pos, Direction.DOWN, player, InteractionHand.MAIN_HAND)
                        && player.getMainHandItem().is(CompatHost.item("basic_water_cell")),
                "hand collects exact bucket through blocked face");
        level.setBlockAndUpdate(pos, CompatHost.block("mfsu").defaultBlockState());
        var storage = level.getBlockEntity(pos);
        require(
                MachineAccess.packed(storage) == 0 && MachineAccess.mayConfigure(storage, player),
                "fresh machine in reach");
        require(
                !MachineAccess.set(storage, -1, 0)
                        && !MachineAccess.set(storage, 6, 1)
                        && !MachineAccess.set(storage, 2, 4)
                        && MachineAccess.packed(storage) == 0,
                "invalid selections keep existing modes");
        MachineAccess.set(storage, 4, 1);
        MachineAccess.set(storage, 2, 2);
        for (var facing : Direction.values()) {
            level.setBlockAndUpdate(
                    pos, level.getBlockState(pos).setValue(BlockStateProperties.FACING, facing));
            var right = facing.getAxis().isVertical() ? Direction.EAST : facing.getClockWise();
            var left = right.getOpposite();
            require(
                    MachineAccess.front(storage) == facing
                            && !MachineAccess.connects(storage, facing)
                            && MachineAccess.allowsOutput(storage, left)
                            && !MachineAccess.allowsInput(storage, left),
                    "relative face after turn " + facing);
            Set<Direction> faces = new HashSet<>();
            for (int side = 0; side < 6; side++) faces.add(MachineAccess.worldSide(facing, side));
            require(faces.size() == 6, "six unique world directions " + facing);
        }
        player.setPos(6.5, 105, 18.5);
        require(!MachineAccess.mayConfigure(storage, player), "distant viewer denied");
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        require(!MachineAccess.set(storage, 4, 0), "removed machine cannot be configured");
        report.add(
                "Machine access: six faces and four modes on native item and fluid ports, cached handles, vanilla hopper insertion and extraction, manual bucket and cell, six rotations and stale target checks.");
    }

    private static void directions(ServerLevel level, BlockPos pos, boolean fluid) {
        var node = level.getBlockEntity(pos);
        for (int side = 0; side < 6; side++) {
            Direction face = MachineAccess.worldSide(MachineAccess.front(node), side);
            Transfer port = nativePort(level, pos, face, fluid);
            require(
                    port != null && port.insert(7, true) == 7 && port.extract(7, true) == 0,
                    "default full permits simulation " + face);
            require(port.insert(7, false) == 7, "seed " + face);
            MachineAccess.set(node, side, 1);
            require(
                    !MachineAccess.connects(node, face)
                            && nativePort(level, pos, face, fluid) == null
                            && port.insert(1, false) == 0
                            && port.extract(1, false) == 0,
                    "blocked cached and new handles " + face);
            MachineAccess.set(node, side, 2);
            var output = nativePort(level, pos, face, fluid);
            require(
                    output != null
                            && output.insert(1, false) == 0
                            && output.extract(3, true) == 3
                            && output.extract(3, false) == 3,
                    "extraction only " + face);
            MachineAccess.set(node, side, 3);
            var input = nativePort(level, pos, face, fluid);
            require(
                    input != null && input.extract(1, false) == 0 && input.insert(2, false) == 2,
                    "insertion only " + face);
            MachineAccess.set(node, side, 0);
            require(
                    nativePort(level, pos, face, fluid).extract(20, false) == 6,
                    "conservation after edits " + face);
        }
    }

    private static Transfer nativePort(
            ServerLevel level, BlockPos pos, Direction side, boolean fluid) {
        return fluid
                ? MachineSideHost.fluid(level, pos, side)
                : MachineSideHost.item(level, pos, side);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void hoppers(ServerLevel level, BlockPos pos) {
        var box = level.getBlockEntity(pos);
        var upper = pos.above();
        level.setBlockAndUpdate(upper, Blocks.HOPPER.defaultBlockState());
        var source = (HopperBlockEntity) level.getBlockEntity(upper);
        source.setItem(0, new ItemStack(Items.COBBLESTONE));
        BlockEntityTicker tick =
                ((EntityBlock) Blocks.HOPPER)
                        .getTicker(level, source.getBlockState(), source.getType());
        MachineAccess.set(box, 0, 2);
        for (int i = 0; i < 16; i++) tick.tick(level, upper, source.getBlockState(), source);
        require(source.getItem(0).getCount() == 1, "output-only top holds hopper input");
        MachineAccess.set(box, 0, 3);
        for (int i = 0; i < 16; i++) tick.tick(level, upper, source.getBlockState(), source);
        require(source.isEmpty(), "input-only top admits hopper input");
        level.setBlockAndUpdate(upper, Blocks.AIR.defaultBlockState());
        var lower = pos.below();
        level.setBlockAndUpdate(lower, Blocks.HOPPER.defaultBlockState());
        var sink = (HopperBlockEntity) level.getBlockEntity(lower);
        MachineAccess.set(box, 1, 3);
        for (int i = 0; i < 16; i++) tick.tick(level, lower, sink.getBlockState(), sink);
        require(sink.isEmpty(), "input-only bottom blocks hopper extraction");
        MachineAccess.set(box, 1, 2);
        for (int i = 0; i < 16; i++) tick.tick(level, lower, sink.getBlockState(), sink);
        require(
                sink.getItem(0).is(Items.COBBLESTONE) && sink.getItem(0).getCount() == 1,
                "output-only bottom releases exactly one");
        level.setBlockAndUpdate(lower, Blocks.AIR.defaultBlockState());
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
