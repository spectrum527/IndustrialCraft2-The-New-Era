package com.industrialage.ia2.logistics;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Cells collect sources in the world and use native sided fluid ports on machines. */
public final class CellItem extends Item {
    public CellItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return TransportPlatform.hasEncodedFluid(stack) ? 1 : super.getMaxStackSize(stack);
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
                && FluidUtil.interactWithFluidHandler(player, hand, level, pos, face, null);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        var source =
                CellSource.find(
                        level, getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY));
        if (source == null) return InteractionResult.PASS;
        if (!source.mayCollect(level, player, held)) return InteractionResult.FAIL;

        var storage = new SimpleContainer(held.copyWithCount(1));
        var cell =
                ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(storage), 0)
                        .getCapability(Capabilities.Fluid.ITEM);
        if (cell == null) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel)) {
            try (var tx = Transaction.openRoot()) {
                return cell.insert(FluidResource.of(source.fluid()), CellFamily.CAPACITY, tx)
                                == CellFamily.CAPACITY
                        ? InteractionResult.SUCCESS
                        : InteractionResult.PASS;
            }
        }

        // The BucketPickup overload inserts a whole source before removing it from the world.
        try (var tx = Transaction.openRoot()) {
            var moved = FluidUtil.tryPickupFluid(cell, player, level, source.pos(), tx);
            if (moved.getAmount() != CellFamily.CAPACITY) return InteractionResult.PASS;
            tx.commit();
            var result = storage.getItem(0);
            return InteractionResult.SUCCESS.heldItemTransformedTo(
                    ItemUtils.createFilledResult(held, player, result));
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flags) {
        var fluid = TransportPlatform.readCellFluid(stack);
        TransportPlatform.appendCellContents(fluid, tooltip);
    }
}
