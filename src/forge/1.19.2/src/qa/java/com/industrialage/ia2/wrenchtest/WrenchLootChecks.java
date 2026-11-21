package com.industrialage.ia2.wrenchtest;

import com.industrialage.ia2.wrench.WrenchUse;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/** Exercises the actual loot tables and waterlogging path when the wrench dismantles utilities. */
public final class WrenchLootChecks {
    private WrenchLootChecks() {}

    /** Dismantles utilities and returns the checks to perform after native entity insertion. */
    public static Runnable run(
            ServerLevel level,
            BlockPos origin,
            ServerPlayer player,
            Function<String, Item> items,
            Function<String, Block> blocks,
            BiPredicate<BlockPos, ItemStack> insert,
            BiPredicate<BlockPos, Integer> fill,
            BiConsumer<String, Boolean> check) {
        var itemPipe = origin.above(4);
        var fluidPipe = origin.above(8);
        var tank = origin.above(12);
        var cable = origin.above(16);
        var foam = origin.above(20);
        for (var pos : new BlockPos[] {itemPipe, fluidPipe, tank, cable, foam}) {
            for (var floor : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, -1, 1))) {
                level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            }
        }
        // Keep the preserved water source from carrying its dropped cable into another test.
        for (var wall : BlockPos.betweenClosed(cable.offset(-2, 0, -2), cable.offset(2, 0, 2))) {
            if (Math.abs(wall.getX() - cable.getX()) == 2
                    || Math.abs(wall.getZ() - cable.getZ()) == 2) {
                level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            }
        }
        String itemPath = "steel_item_pipe_small";
        String fluidPath = "bronze_fluid_pipe_medium";
        String tankPath = "bronze_tank";
        String wire = "insulated_copper_cable";
        String foamed = "insulated_copper_foam_cable";
        level.setBlockAndUpdate(
                itemPipe,
                blocks.apply(itemPath)
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST));
        level.setBlockAndUpdate(fluidPipe, blocks.apply(fluidPath).defaultBlockState());
        level.setBlockAndUpdate(tank, blocks.apply(tankPath).defaultBlockState());
        level.setBlockAndUpdate(
                cable,
                blocks.apply(wire)
                        .defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, true));
        level.setBlockAndUpdate(foam, blocks.apply(foamed).defaultBlockState());
        check.accept(
                "wrench-loot-stocks-live-pipes",
                insert.test(itemPipe, new ItemStack(Items.DIAMOND, 3))
                        && fill.test(fluidPipe, 257));
        var cover = new ItemStack(items.apply("pump_cover_mv"));
        player.setItemInHand(InteractionHand.MAIN_HAND, cover);
        move(player, fluidPipe);
        check.accept(
                "wrench-loot-cover-installed",
                player.gameMode
                                .useItemOn(
                                        player,
                                        level,
                                        cover,
                                        InteractionHand.MAIN_HAND,
                                        WrenchHits.center(level, fluidPipe, Direction.NORTH))
                                .consumesAction()
                        && cover.isEmpty());

        var tool = new ItemStack(items.apply("wrench"));
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        remove(level, player, itemPipe, tool, itemPath, Direction.NORTH, 5, check);
        remove(level, player, fluidPipe, tool, fluidPath, Direction.NORTH, 5, check);
        remove(level, player, tank, tool, tankPath, Direction.NORTH, 1, check);
        remove(level, player, cable, tool, wire, Direction.UP, 1, check);
        remove(level, player, foam, tool, foamed, Direction.NORTH, 1, check);
        check.accept(
                "wrench-wet-wire-leaves-source-water",
                level.getBlockState(cable).is(Blocks.WATER)
                        && level.getFluidState(cable).isSource());
        player.getInventory().clearContent();
        return () -> {
            check.accept(
                    "wrench-item-pipe-drops-itself-and-three-diamonds-once",
                    count(level, itemPipe, items.apply(itemPath)) == 1
                            && count(level, itemPipe, Items.DIAMOND) == 3);
            check.accept(
                    "wrench-fluid-pipe-drops-itself-and-one-cover-once",
                    count(level, fluidPipe, items.apply(fluidPath)) == 1
                            && count(level, fluidPipe, items.apply("pump_cover_mv")) == 1);
            check.accept("wrench-tank-drops-once", count(level, tank, items.apply(tankPath)) == 1);
            check.accept(
                    "wrench-wet-wire-and-foamed-wire-use-native-loot",
                    count(level, cable, items.apply(wire)) == 1
                            && count(level, foam, items.apply(wire)) == 1);
        };
    }

    private static void remove(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            ItemStack tool,
            String name,
            Direction face,
            int areas,
            BiConsumer<String, Boolean> check) {
        move(player, pos);
        var hit = WrenchHits.center(level, pos, face);
        var preview = WrenchUse.preview(level, player, hit);
        int before = tool.getDamageValue();
        check.accept(
                "wrench-loot-red-center-" + name,
                preview != null
                        && preview.areas().size() == areas
                        && preview.selected() != null
                        && preview.selected().center()
                        && player.gameMode
                                .useItemOn(player, level, tool, InteractionHand.MAIN_HAND, hit)
                                .consumesAction()
                        && tool.getDamageValue() == before + 1);
    }

    private static int count(ServerLevel level, BlockPos pos, Item expected) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.5)).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(expected))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static void move(ServerPlayer player, BlockPos pos) {
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() - 1.5);
    }
}
