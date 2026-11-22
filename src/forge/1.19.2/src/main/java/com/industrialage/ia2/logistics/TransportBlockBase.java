package com.industrialage.ia2.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

abstract class TransportBlockBase extends Block {
    TransportBlockBase(Properties properties) {
        super(properties);
    }

    abstract boolean interact(
            Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit);

    abstract BlockState connect(
            BlockState state, Direction side, BlockState neighbor, LevelReader level, BlockPos pos);

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        Direction side = hit.getDirection();
        if (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, side, player.getItemInHand(hand))) {
            return InteractionResult.PASS;
        }
        return interact(level, pos, player, hand, hit)
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        return connect(state, side, neighbor, level, pos);
    }

    @Override
    public void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock())
                && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof TransportNode node) {
            node.dropContents(server);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
