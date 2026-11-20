package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Placed IC2 storage box, personal safe or player crafting bench. */
public final class BoxBlock extends WorkshopBlockBase implements EntityBlock {
    final BoxKind kind;

    BoxBlock(Properties properties, BoxKind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoxNode(pos, state);
    }

    @Override
    boolean interact(
            Level world, BlockPos pos, Player player, InteractionHand hand, Direction face) {
        if (world.getBlockEntity(pos) instanceof BoxNode box
                && player instanceof net.minecraft.server.level.ServerPlayer) box.open(player, 0);
        return true;
    }

    @Override
    public float getDestroyProgress(
            BlockState state, Player player, BlockGetter world, BlockPos pos) {
        if (kind == BoxKind.PERSONAL_CHEST
                && world.getBlockEntity(pos) instanceof BoxNode node
                && !node.permits(player)) return 0;
        return super.getDestroyProgress(state, player, world, pos);
    }
}
