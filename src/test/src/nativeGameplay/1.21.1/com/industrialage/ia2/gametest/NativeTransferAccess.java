package com.industrialage.ia2.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** The old NeoForge capability surface used by the native world fixture. */
final class NativeTransferAccess {
    private NativeTransferAccess() {}

    static ItemStack cargo(ServerLevel level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side).getStackInSlot(0);
    }

    static int insert(
            ServerLevel level, BlockPos pos, Direction side, ItemStack stack, boolean simulate) {
        return stack.getCount()
                - level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)
                        .insertItem(0, stack, simulate)
                        .getCount();
    }

    static int extract(
            ServerLevel level, BlockPos pos, Direction side, int amount, boolean simulate) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)
                .extractItem(0, amount, simulate)
                .getCount();
    }

    static int cellVolume(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY).getAmount();
    }

    static int volume(ServerLevel level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side)
                .getFluidInTank(0)
                .getAmount();
    }

    static int fill(
            ServerLevel level, BlockPos pos, Direction side, FluidStack stack, boolean simulate) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side)
                .fill(stack, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
    }

    static int drain(
            ServerLevel level, BlockPos pos, Direction side, int amount, boolean simulate) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side)
                .drain(amount, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE)
                .getAmount();
    }

    static int itemFluidVolume(ItemStack stack) {
        var handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        return handler == null ? 0 : handler.getFluidInTank(0).getAmount();
    }
}
