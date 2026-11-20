package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return interact(level, pos, player, hand, hit.getDirection())
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }

    @Override
    public void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        WorkshopInventory.dropOnReplacement(level, pos, state, replacement);
        super.onRemove(state, level, pos, replacement, moving);
    }
}
