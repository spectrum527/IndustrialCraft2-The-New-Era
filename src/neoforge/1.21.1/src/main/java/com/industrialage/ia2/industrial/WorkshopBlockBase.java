package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

abstract class WorkshopBlockBase extends Block {
    WorkshopBlockBase(Properties props) {
        super(props);
    }

    abstract boolean interact(
            Level world, BlockPos pos, Player player, InteractionHand hand, Direction side);

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return interact(level, pos, player, hand, hit.getDirection())
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(level, pos, player, InteractionHand.MAIN_HAND, hit.getDirection())
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    @Override
    protected void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        WorkshopInventory.dropOnReplacement(level, pos, state, next);
        super.onRemove(state, level, pos, next, moving);
    }
}
