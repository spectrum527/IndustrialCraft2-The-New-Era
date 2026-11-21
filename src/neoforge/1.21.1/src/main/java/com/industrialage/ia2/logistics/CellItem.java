package com.industrialage.ia2.logistics;

import com.industrialage.ia2.industrial.WorkshopPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;

/** Cells collect sources in the world and use native sided fluid ports on machines. */
public final class CellItem extends Item {
    public CellItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }
        return interactWithBlock(
                        level,
                        context.getClickedPos(),
                        context.getClickedFace(),
                        player,
                        context.getHand())
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    /** Exchanges fluid with the clicked face before a machine opens its menu. */
    public static boolean interactWithBlock(
            ServerLevel level, BlockPos pos, Direction face, Player player, InteractionHand hand) {
        return level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, face, player.getItemInHand(hand))
                && (WorkshopPlatform.fillNamedHand(level, pos, face, player, hand)
                        || FluidUtil.interactWithFluidHandler(player, hand, level, pos, face));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        var source =
                CellSource.find(
                        level, getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY));
        if (source == null) return InteractionResultHolder.pass(held);
        if (!source.mayCollect(level, player, held)) return InteractionResultHolder.fail(held);

        var cell = held.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
        var volume = new FluidStack(source.fluid(), NeoTransport.CELL_CAPACITY);
        if (cell == null
                || cell.fill(volume, IFluidHandler.FluidAction.SIMULATE) != volume.getAmount()) {
            return InteractionResultHolder.pass(held);
        }
        if (!(level instanceof ServerLevel)) return InteractionResultHolder.success(held);

        var moved =
                FluidUtil.tryFluidTransfer(
                        cell,
                        new BucketPickupHandlerWrapper(
                                player, source.pickup(), level, source.pos()),
                        volume,
                        true);
        if (moved.getAmount() != volume.getAmount()) return InteractionResultHolder.pass(held);

        var sound = moved.getFluidType().getSound(moved, SoundActions.BUCKET_FILL);
        if (sound != null) level.playSound(null, source.pos(), sound, SoundSource.BLOCKS, 1, 1);
        var result = cell.getContainer();
        if (TransportPlatform.isPristineCell(held)) {
            var named = WorkshopPlatform.namedFilledCell(moved);
            if (!named.isEmpty()) result = named;
        }
        return InteractionResultHolder.consume(ItemUtils.createFilledResult(held, player, result));
    }
}
