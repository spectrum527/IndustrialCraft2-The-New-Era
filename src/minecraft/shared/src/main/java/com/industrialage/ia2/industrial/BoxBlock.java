package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
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
        if (kind.portable()
                && player.isShiftKeyDown()
                && WorkshopPlatform.itemId(player.getItemInHand(hand)).equals("ic2:wrench"))
            return false;
        if (world.getBlockEntity(pos) instanceof BoxNode box
                && player instanceof net.minecraft.server.level.ServerPlayer) box.open(player, 0);
        return true;
    }

    /** Dismantles a box through the ordinary player break and loot path. */
    public InteractionResult wrench(UseOnContext context) {
        var player = context.getPlayer();
        var world = context.getLevel();
        var pos = context.getClickedPos();
        if (!kind.portable() || player == null || !player.isShiftKeyDown())
            return InteractionResult.PASS;
        if (!player.mayBuild()
                || !world.mayInteract(player, pos)
                || !(world.getBlockEntity(pos) instanceof BoxNode node)
                || !node.permits(player)) {
            return InteractionResult.FAIL;
        }
        if (world instanceof net.minecraft.server.level.ServerLevel) {
            if (!(player instanceof net.minecraft.server.level.ServerPlayer server)
                    || !server.gameMode.destroyBlock(pos)) return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
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
