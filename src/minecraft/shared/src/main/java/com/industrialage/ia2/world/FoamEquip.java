package com.industrialage.ia2.world;

import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class FoamEquip extends Item {
    static final int COST = 100;
    private final int capacity;

    FoamEquip(Properties settings, int capacity) {
        super(settings);
        this.capacity = capacity;
    }

    boolean pack() {
        return capacity > 8000;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(
                "ic2.world.foam_tool", super.getName(stack), amount(stack), capacity);
    }

    int room(ItemStack stack) {
        return capacity - amount(stack);
    }

    int amount(ItemStack stack) {
        return Math.min(capacity, WorldNative.foam(stack));
    }

    void add(ItemStack stack, int delta) {
        WorldNative.setFoam(stack, Math.min(capacity, Math.max(0, amount(stack) + delta)));
    }

    boolean refillBucket(Player player, InteractionHand hand, ItemStack stack, boolean server) {
        var otherHand =
                hand == InteractionHand.MAIN_HAND
                        ? InteractionHand.OFF_HAND
                        : InteractionHand.MAIN_HAND;
        var other = player.getItemInHand(otherHand);
        if (room(stack) < 1000 || !WorldNative.itemId(other).equals("ic2:construction_foam_bucket"))
            return false;
        if (server) {
            add(stack, 1000);
            if (!player.isCreative()) player.setItemInHand(otherHand, new ItemStack(Items.BUCKET));
        }
        return true;
    }

    boolean spray(
            Level level,
            BlockPos clicked,
            Direction side,
            Player player,
            InteractionHand hand,
            ItemStack stack) {
        if (pack()) return false;
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        FoamEquip supply = chest.getItem() instanceof FoamEquip item && item.pack() ? item : null;
        int budget = amount(stack) + (supply == null ? 0 : supply.amount(chest));
        BlockState hit = level.getBlockState(clicked);
        boolean onScaffold = hit.getBlock() instanceof WorldBlocks.Scaffold;
        boolean onCable = cableFoam(hit) != null;
        BlockPos first = onScaffold || onCable ? clicked : clicked.relative(side);
        if (budget < COST && !player.isCreative()
                || !mayFoam(level, first, player, onScaffold, onCable)) return false;
        if (!(level instanceof ServerLevel server)) return true;
        int maxBlocks = player.isShiftKeyDown() ? 1 : 10;
        if (!player.isCreative()) maxBlocks = Math.min(maxBlocks, budget / COST);
        Block firstBlock = level.getBlockState(first).getBlock();
        var queue = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        queue.add(first);
        visited.add(first);
        int placed = 0;
        while (!queue.isEmpty() && placed < maxBlocks) {
            var pos = queue.removeFirst();
            if (!mayFoam(level, pos, player, onScaffold, onCable)) continue;
            var old = level.getBlockState(pos);
            BlockState result =
                    onCable
                            ? cableFoam(old)
                            : WorldNative.block("ic2:foam")
                                    .defaultBlockState()
                                    .setValue(
                                            WorldBlocks.WetFoam.REINFORCED,
                                            onScaffold
                                                    && WorldNative.blockId(old.getBlock())
                                                            .startsWith("ic2:reinforced_"));
            if (result == null || !server.setBlockAndUpdate(pos, result)) continue;
            placed++;
            for (Direction direction : Direction.values()) {
                if (!onScaffold && !onCable && direction.getAxis() == side.getAxis()) continue;
                BlockPos neighbor = pos.relative(direction);
                if (first.distManhattan(neighbor) <= 6 && visited.add(neighbor))
                    queue.addLast(neighbor);
            }
        }
        if (!player.isCreative() && placed > 0) {
            int required = placed * COST;
            int fromStack = Math.min(amount(stack), required);
            add(stack, -fromStack);
            if (supply != null) supply.add(chest, fromStack - required);
        }
        return placed > 0;
    }

    private static boolean mayFoam(
            Level level, BlockPos pos, Player player, boolean onScaffold, boolean onCable) {
        if (!level.hasChunkAt(pos) || !player.mayBuild() || !level.mayInteract(player, pos))
            return false;
        var state = level.getBlockState(pos);
        if (onCable) return cableFoam(state) != null;
        if (onScaffold) return state.getBlock() instanceof WorldBlocks.Scaffold;
        return state.isAir();
    }

    private static BlockState cableFoam(BlockState state) {
        String old = WorldNative.blockId(state.getBlock());
        if (!old.startsWith("ic2:") || !old.endsWith("_cable") || old.endsWith("_foam_cable"))
            return null;
        var foam =
                WorldNative.block(
                        old.substring(0, old.length() - "_cable".length()) + "_foam_cable");
        if (foam == null || foam.defaultBlockState().isAir()) return null;
        return WorldNative.copySharedState(state, foam.defaultBlockState());
    }
}
