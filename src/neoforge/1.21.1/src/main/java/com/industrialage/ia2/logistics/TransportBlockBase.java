package com.industrialage.ia2.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Native 1.21.1 interaction and removal hooks for transport blocks. */
abstract class TransportBlockBase extends Block {
    TransportBlockBase(Properties properties) {
        super(properties);
    }

    abstract boolean interact(
            Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit);

    abstract BlockState connect(
            BlockState state, Direction side, BlockState neighbor, LevelReader level, BlockPos pos);

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return interact(level, pos, player, hand, hit)
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        return connect(state, side, neighbor, level, pos);
    }

    @Override
    protected void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock()
                && level instanceof ServerLevel server
                && server.getBlockEntity(pos) instanceof TransportNode node) {
            node.dropContents(server);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
