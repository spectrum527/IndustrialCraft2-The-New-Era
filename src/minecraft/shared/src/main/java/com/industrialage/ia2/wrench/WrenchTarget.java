package com.industrialage.ia2.wrench;

import com.industrialage.ia2.power.WorldEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Marks an IC2 block with a wrench operation and its supported facing property. */
public interface WrenchTarget {
    /** Whether this block has wrench controls in the given state. */
    default boolean wrenchable(BlockState state) {
        return true;
    }

    /** Uses the native block loot table, including composite or state-dependent drops. */
    default boolean wrenchUsesLoot() {
        return false;
    }

    /** Checks device ownership or other device-specific access rules. */
    default boolean wrenchPermits(Level level, BlockPos pos, Player player) {
        return true;
    }

    /** Tests whether the specified facing has a defined action on this block. */
    default boolean wrenchSide(BlockState state, Direction side) {
        var facing = directionProperty(state);
        return facing != null && facing.getPossibleValues().contains(side);
    }

    /** Performs the facing operation, preserving the existing block entity and its inventory. */
    default boolean wrenchTurn(ServerLevel level, BlockPos pos, BlockState state, Direction side) {
        var next = state.setValue(directionProperty(state), side);
        if (next == state || !level.setBlockAndUpdate(pos, next)) return false;
        if (level.getBlockEntity(pos) != null) WorldEnergy.connect(level.getBlockEntity(pos));
        return true;
    }

    /** Supplies the whole block item, including retained charge where the device supports it. */
    default ItemStack wrenchDrop(ServerLevel level, BlockPos pos, BlockState state) {
        return new ItemStack(state.getBlock());
    }

    /**
     * Dismantles through the loader's break permission and the block's normal removal callbacks.
     */
    default boolean wrenchRemove(
            ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (!WrenchNative.mayBreak(level, player, pos, state)) return false;
        if (wrenchUsesLoot()) return level.destroyBlock(pos, !player.isCreative(), player);
        var drop = wrenchDrop(level, pos, state);
        if (!level.removeBlock(pos, false)) return false;
        if (!player.isCreative()) Block.popResource(level, pos, drop);
        return true;
    }

    private static EnumProperty<Direction> directionProperty(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) return BlockStateProperties.FACING;
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? BlockStateProperties.HORIZONTAL_FACING
                : null;
    }
}
