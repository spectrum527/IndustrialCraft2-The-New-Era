package com.industrialage.ia2.wrenchtest;

import com.industrialage.ia2.content.PortableEquipment;
import com.industrialage.ia2.wrench.WrenchUse;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Native wrench regression using ordinary server-side player interaction on real block surfaces.
 */
public final class WrenchChecks {
    private WrenchChecks() {}

    /**
     * Exercises every orientation, no-op zones and conservation using the registered tool items.
     */
    public static Runnable run(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            Function<String, Item> items,
            Function<String, Block> blocks,
            ObjDoubleConsumer<ItemStack> charge,
            ToDoubleFunction<ItemStack> stored,
            BiConsumer<String, Boolean> check) {
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setShiftKeyDown(false);
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() - 1.5);
        var manual = new ItemStack(items.apply("wrench"));
        var electric = new ItemStack(items.apply("electric_wrench"));
        charge.accept(electric, 2500);
        for (var face : Direction.values()) {
            for (var target : Direction.values()) {
                if (face.getAxis() == target.getAxis()) continue;
                level.setBlockAndUpdate(
                        pos,
                        blocks.apply("batbox")
                                .defaultBlockState()
                                .setValue(BlockStateProperties.FACING, target.getOpposite()));
                player.setItemInHand(InteractionHand.MAIN_HAND, manual);
                var hit = WrenchHits.turn(level, pos, face, target);
                var preview = WrenchUse.preview(level, player, hit);
                int damage = manual.getDamageValue();
                check.accept(
                        "wrench-" + face.getName() + "-to-" + target.getName(),
                        preview != null
                                && preview.areas().size() == 5
                                && preview.selected() != null
                                && preview.selected().direction() == target
                                && use(level, player, hit)
                                && level.getBlockState(pos).getValue(BlockStateProperties.FACING)
                                        == target
                                && manual.getDamageValue() == damage + 1);
            }
        }

        var processor = blocks.apply("macerator");
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlockAndUpdate(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            }
        }
        level.setBlockAndUpdate(
                pos,
                processor
                        .defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        var entity = level.getBlockEntity(pos);
        var menu = ((MenuProvider) entity).createMenu(50, player.getInventory(), player);
        menu.getSlot(0).set(new ItemStack(Items.RAW_IRON, 3));
        var gap = WrenchHits.ray(level, pos, Direction.SOUTH, Vec3.atCenterOf(pos).add(.20, 0, 0));
        var corner =
                WrenchHits.ray(level, pos, Direction.SOUTH, Vec3.atCenterOf(pos).add(.38, .38, 0));
        int damage = manual.getDamageValue();
        check.accept(
                "wrench-gap-corner-and-vertical-noop",
                !use(level, player, gap)
                        && !use(level, player, corner)
                        && !use(level, player, WrenchHits.turn(level, pos, Direction.UP))
                        && level.getBlockEntity(pos) == entity
                        && manual.getDamageValue() == damage
                        && menu.getSlot(0).getItem().getCount() == 3);
        player.setItemInHand(InteractionHand.OFF_HAND, electric);
        player.setShiftKeyDown(true);
        check.accept(
                "wrench-blue-sneaking-still-rotates-and-uses-main-tool",
                use(level, player, WrenchHits.turn(level, pos, Direction.EAST))
                        && level.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING)
                                == Direction.EAST
                        && level.getBlockEntity(pos) == entity
                        && manual.getDamageValue() == damage + 1
                        && stored.applyAsDouble(electric) == 2500);
        check.accept(
                "wrench-already-facing-is-free",
                !use(level, player, WrenchHits.turn(level, pos, Direction.EAST))
                        && manual.getDamageValue() == damage + 1);

        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        check.accept(
                "wrench-empty-main-offhand-spends-only-rotation-eu",
                use(level, player, WrenchHits.turn(level, pos, Direction.WEST))
                        && level.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING)
                                == Direction.WEST
                        && stored.applyAsDouble(electric)
                                == 2500 - PortableEquipment.ELECTRIC_WRENCH.cost
                        && electric.getDamageValue() == 0);
        var center = WrenchHits.center(level, pos, Direction.SOUTH);
        var boundary =
                WrenchHits.ray(level, pos, Direction.SOUTH, Vec3.atCenterOf(pos).add(.16, 0, 0));
        player.setGameMode(GameType.ADVENTURE);
        check.accept(
                "wrench-adventure-central-denied",
                !use(level, player, center) && level.getBlockEntity(pos) == entity);
        player.setGameMode(GameType.SURVIVAL);
        charge.accept(electric, 999);
        check.accept(
                "wrench-uncharged-for-dismantling-leaves-inventory",
                !use(level, player, center)
                        && level.getBlockEntity(pos) == entity
                        && stored.applyAsDouble(electric) == 999
                        && menu.getSlot(0).getItem().getCount() == 3);
        charge.accept(electric, 1000);
        check.accept(
                "wrench-red-edge-inclusive-dismantles-without-crouching",
                use(level, player, boundary)
                        && level.getBlockState(pos).isAir()
                        && stored.applyAsDouble(electric) == 0
                        && electric.getDamageValue() == 0);
        player.getInventory().clearContent();
        return () -> verifyDrops(level, pos, items.apply("macerator"), check);
    }

    private static void verifyDrops(
            ServerLevel level, BlockPos pos, Item machine, BiConsumer<String, Boolean> check) {
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        int machines = 0, inventory = 0;
        for (var drop : drops) {
            var stack = drop.getItem();
            if (stack.is(machine)) machines += stack.getCount();
            if (stack.is(Items.RAW_IRON)) inventory += stack.getCount();
            drop.discard();
        }
        check.accept("wrench-machine-and-contents-dropped-once", machines == 1 && inventory == 3);
    }

    private static boolean use(ServerLevel world, ServerPlayer player, BlockHitResult hit) {
        return player.gameMode
                .useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit)
                .consumesAction();
    }
}
