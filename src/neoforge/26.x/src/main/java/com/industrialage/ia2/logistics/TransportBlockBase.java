package com.industrialage.ia2.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Native 26.x interaction and connection hooks for transport blocks. */
abstract class TransportBlockBase extends Block {
    TransportBlockBase(Properties properties) {
        super(properties);
    }

    abstract boolean interact(
            Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit);

    abstract BlockState connect(
            BlockState state, Direction side, BlockState neighbor, LevelReader level, BlockPos pos);

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return interact(level, pos, player, hand, hit)
                ? InteractionResult.SUCCESS
                : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction side,
            BlockPos neighborPos,
            BlockState neighbor,
            RandomSource random) {
        return connect(state, side, neighbor, level, pos);
    }
}
