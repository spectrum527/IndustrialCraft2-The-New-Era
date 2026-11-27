package com.industrialage.ia2.logistics;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;

final class CellPickup {
    private CellPickup() {}

    static InteractionResultHolder<ItemStack> collect(
            Level level, Player player, InteractionHand hand, BlockHitResult hit, int capacity) {
        var held = player.getItemInHand(hand);
        var source = CellSource.find(level, hit);
        if (source == null) return InteractionResultHolder.pass(held);
        if (!source.mayCollect(level, player, held)) return InteractionResultHolder.fail(held);

        var single = held.copy();
        single.setCount(1);
        var cell = FluidUtil.getFluidHandler(single).orElse(null);
        var volume = new FluidStack(source.fluid(), capacity);
        if (cell == null || cell.fill(volume, IFluidHandler.FluidAction.SIMULATE) != capacity) {
            return InteractionResultHolder.pass(held);
        }
        if (level.isClientSide) return InteractionResultHolder.success(held);

        var moved =
                FluidUtil.tryFluidTransfer(
                        cell,
                        new BucketPickupHandlerWrapper(source.pickup(), level, source.pos()),
                        volume,
                        true);
        if (moved.getAmount() != capacity) return InteractionResultHolder.pass(held);

        var sound = moved.getFluid().getFluidType().getSound(moved, SoundActions.BUCKET_FILL);
        if (sound != null) level.playSound(null, source.pos(), sound, SoundSource.BLOCKS, 1, 1);
        return InteractionResultHolder.consume(stowFilledCell(held, player, cell.getContainer()));
    }

    private static ItemStack stowFilledCell(ItemStack held, Player player, ItemStack filled) {
        // In 1.19.2 the vanilla creative duplicate check only compares the item type.
        if (player.getAbilities().instabuild
                && player.getInventory()
                        .hasAnyMatching(stack -> ItemStack.isSameItemSameTags(stack, filled))) {
            return held;
        }
        return ItemUtils.createFilledResult(held, player, filled, false);
    }
}
