package com.industrialage.ia2.compatqa;

import com.industrialage.ia2.logistics.TransportNode;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Native hoppers, a fitted item pipe and real processing or charging in the installed mod. */
final class MachineSlotFlowChecks {
    private MachineSlotFlowChecks() {}

    static void run(ServerLevel level, ServerPlayer player, List<String> report) {
        BlockPos pos = new BlockPos(6, 137, 6);
        processor(level, player, pos);
        chargeStation(level, player, pos);
        report.add(
                "Machine slot rules: a hopper delivered two coal and one compatible battery, while bedrock and an upgrade stayed outside. A hopper and extraction nozzle collected two processed dust, leaving the input, power cell and installed upgrade. A real charge slot released its battery only at full charge.");
    }

    private static void processor(ServerLevel level, ServerPlayer player, BlockPos pos) {
        Item battery = CompatHost.item("re_battery");
        Item upgrade = CompatHost.item("energy_storage_upgrade");
        Item product = CompatHost.item("coal_dust");
        Container stock = placeDevice(level, player, pos, "macerator");
        var upper = placeHopper(level, pos.above());
        var lower = placeHopper(level, pos.below());
        var pipe = pos.south();
        level.setBlockAndUpdate(
                pipe,
                CompatHost.block("bronze_item_pipe_small")
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.SOUTH));
        var nozzle = new ItemStack(CompatHost.item("pipe_extraction_nozzle"));
        player.setItemInHand(InteractionHand.MAIN_HAND, nozzle);
        var contact = Vec3.atCenterOf(pipe).add(0, 0, -.5);
        player.setPos(pipe.getX() + .5, pipe.getY() + 1, pipe.getZ() + .5);
        player.gameMode.useItemOn(
                player,
                level,
                nozzle,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(contact, Direction.NORTH, pipe, false));
        require(
                ((TransportNode) level.getBlockEntity(pipe)).nozzle(Direction.NORTH)
                        == TransportNode.Nozzle.EXTRACTION,
                "fitted extraction nozzle");
        upper.setItem(0, new ItemStack(Items.BEDROCK));
        upper.setItem(1, new ItemStack(upgrade));
        upper.setItem(2, new ItemStack(Items.COAL, 2));
        tick(level, pos.above(), 24);
        require(
                count(stock, Items.COAL) == 2
                        && count(upper, Items.BEDROCK) == 1
                        && count(upper, upgrade) == 1
                        && count(stock, upgrade) == 0,
                "hopper routes only valid recipe input");
        stock.setItem(0, new ItemStack(Items.BEDROCK));
        stock.setItem(3, new ItemStack(upgrade));
        tick(level, pos.below(), 12);
        tick(level, pipe, 1);
        require(
                lower.isEmpty() && count(stock, Items.BEDROCK) == 1 && count(stock, upgrade) == 1,
                "stranded old input and a fitted upgrade stay inside");
        stock.setItem(0, new ItemStack(Items.COAL, 2));
        var charged = new ItemStack(battery);
        ToolCheckHost.charge(charged, 10000);
        upper.setItem(3, charged);
        tick(level, pos.above(), 20);
        tick(level, pos.below(), 12);
        tick(level, pipe, 1);
        require(
                count(stock, battery) == 1 && count(stock, Items.COAL) == 2 && lower.isEmpty(),
                "waiting ingredients and power cell remain unavailable for extraction");
        for (Direction face : Direction.values()) {
            var raw = MachineSideHost.item(level, pos, face, new ItemStack(Items.COAL));
            require(raw != null && raw.extract(1, true) == 0, "recipe input protected on " + face);
        }
        var unsided = MachineSideHost.item(level, pos, null, new ItemStack(Items.COAL));
        require(
                unsided == null || unsided.extract(1, true) == 0,
                "unsided access cannot remove ingredients");
        tick(level, pos, 650);
        require(
                count(stock, product) == 2
                        && count(stock, battery) == 1
                        && count(stock, upgrade) == 1,
                "two real recipe completions (coal="
                        + count(stock, Items.COAL)
                        + ", dust="
                        + count(stock, product)
                        + ", batteries="
                        + count(stock, battery)
                        + ")");
        stock.setItem(0, new ItemStack(Items.COAL));
        tick(level, pos.below(), 1);
        require(count(lower, product) == 1, "vanilla hopper takes the first product");
        tick(level, pipe, 1);
        var outlet = MachineSideHost.item(level, pipe, Direction.SOUTH, new ItemStack(product));
        require(
                outlet != null && outlet.extract(4, false) == 1,
                "fitting takes the second product");
        tick(level, pos.below(), 16);
        require(
                count(stock, product) == 0
                        && count(stock, Items.COAL) == 1
                        && count(stock, battery) == 1
                        && count(stock, upgrade) == 1
                        && count(lower, product) == 1,
                "both routes leave all protected slots intact");
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pipe, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static void chargeStation(ServerLevel level, ServerPlayer player, BlockPos pos) {
        Container stock = placeDevice(level, player, pos, "batbox");
        var sink = placeHopper(level, pos.below());
        var partial = new ItemStack(CompatHost.item("re_battery"));
        ToolCheckHost.charge(partial, 9980);
        var full = partial.copy();
        ToolCheckHost.charge(full, 20);
        var in = MachineSideHost.item(level, pos, Direction.UP, partial);
        require(
                in.insert(1, true) == 1 && stock.isEmpty() && in.insert(1, false) == 1,
                "compatible charging workpiece accepts once");
        require(
                ToolCheckHost.stored(stock.getItem(0)) == 9980,
                "partial battery starts in charging work slot");
        var supply = MachineSideHost.item(level, pos, Direction.UP, full);
        require(
                supply.insert(1, false) == 1 && ToolCheckHost.stored(stock.getItem(1)) == 10000,
                "donor enters power input");
        tick(level, pos.below(), 12);
        require(sink.isEmpty(), "incomplete workpiece and full donor cannot leave");
        tick(level, pos, 32);
        require(ToolCheckHost.stored(stock.getItem(0)) == 10000, "station completes charging");
        tick(level, pos.below(), 24);
        require(
                stock.getItem(0).isEmpty()
                        && !stock.getItem(1).isEmpty()
                        && count(sink, full.getItem()) == 1
                        && ToolCheckHost.stored(sink.getItem(0)) == 10000,
                "only completed workpiece exits, donor stays in power slot");
        level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static Container placeDevice(
            ServerLevel level, ServerPlayer player, BlockPos pos, String name) {
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + 1.5);
        level.setBlockAndUpdate(pos, CompatHost.block(name).defaultBlockState());
        var node = level.getBlockEntity(pos);
        // This fixture drives native ticks before the next world tick dispatches onLoad.
        node.onLoad();
        return ((MenuProvider) node)
                .createMenu(17, player.getInventory(), player)
                .getSlot(0)
                .container;
    }

    private static HopperBlockEntity placeHopper(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.HOPPER.defaultBlockState());
        return (HopperBlockEntity) level.getBlockEntity(pos);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void tick(ServerLevel level, BlockPos pos, int steps) {
        var node = level.getBlockEntity(pos);
        var state = node.getBlockState();
        BlockEntityTicker ticker =
                ((EntityBlock) state.getBlock()).getTicker(level, state, node.getType());
        for (int i = 0; i < steps; i++) ticker.tick(level, pos, level.getBlockState(pos), node);
    }

    private static int count(Container stock, Item item) {
        return stock.countItem(item);
    }

    private static void require(boolean value, String check) {
        if (!value) throw new AssertionError(check);
    }
}
