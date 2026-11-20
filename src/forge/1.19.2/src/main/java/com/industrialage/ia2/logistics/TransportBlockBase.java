package com.industrialage.ia2.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

abstract class TransportBlockBase extends Block {
    TransportBlockBase(Properties properties) {
        super(properties);
    }

    abstract boolean interact(
            Level level, BlockPos pos, Player player, InteractionHand hand, Direction face);

    abstract BlockState connect(BlockState state, Direction side, BlockState neighbor);

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
        var entity = level.getBlockEntity(pos);
        int valves = entity instanceof TransportNode node ? node.closed : 0;
        if (!interact(level, pos, player, hand, side)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && entity instanceof TransportNode node && valves != node.closed) {
            var current = level.getBlockState(pos);
            var neighborPos = pos.relative(side);
            var updated =
                    updateShape(
                            current,
                            side,
                            level.getBlockState(neighborPos),
                            level,
                            pos,
                            neighborPos);
            if (updated != current) {
                level.setBlockAndUpdate(pos, updated);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        var shape = connect(state, side, neighbor);
        if (getStateDefinition().getProperty(side.getName()) instanceof BooleanProperty arm
                && (closed(level, pos, side) || closed(level, neighborPos, side.getOpposite()))) {
            return shape.setValue(arm, false);
        }
        return shape;
    }

    private static boolean closed(LevelAccessor level, BlockPos pos, Direction side) {
        return level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof TransportNode node
                && !node.open(side);
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
