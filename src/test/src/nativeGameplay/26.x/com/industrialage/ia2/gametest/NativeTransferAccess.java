package com.industrialage.ia2.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** The transactional NeoForge capability surface used by the native world fixture. */
final class NativeTransferAccess {
    private NativeTransferAccess() {}

    static ItemStack cargo(ServerLevel level, BlockPos pos, Direction side) {
        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        return handler.getResource(0).toStack(handler.getAmountAsInt(0));
    }

    static int insert(
            ServerLevel level, BlockPos pos, Direction side, ItemStack stack, boolean simulate) {
        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        try (var transaction = Transaction.openRoot()) {
            int moved = handler.insert(0, ItemResource.of(stack), stack.getCount(), transaction);
            if (!simulate) transaction.commit();
            return moved;
        }
    }

    static int extract(
            ServerLevel level, BlockPos pos, Direction side, int amount, boolean simulate) {
        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        try (var transaction = Transaction.openRoot()) {
            int moved = handler.extract(0, handler.getResource(0), amount, transaction);
            if (!simulate) transaction.commit();
            return moved;
        }
    }

    static int cellVolume(ItemStack stack) {
        return FluidUtil.getFirstStackContained(stack).getAmount();
    }

    static int volume(ServerLevel level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.Fluid.BLOCK, pos, side).getAmountAsInt(0);
    }

    static int fill(
            ServerLevel level, BlockPos pos, Direction side, FluidStack stack, boolean simulate) {
        var handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        try (var transaction = Transaction.openRoot()) {
            int moved = handler.insert(0, FluidResource.of(stack), stack.getAmount(), transaction);
            if (!simulate) transaction.commit();
            return moved;
        }
    }

    static int drain(
            ServerLevel level, BlockPos pos, Direction side, int amount, boolean simulate) {
        var handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        try (var transaction = Transaction.openRoot()) {
            int moved = handler.extract(0, handler.getResource(0), amount, transaction);
            if (!simulate) transaction.commit();
            return moved;
        }
    }

    static ItemStack filledCell(ItemStack stack, FluidStack fluid) {
        var scratch = new net.minecraft.world.SimpleContainer(stack.copyWithCount(1));
        var access =
                ItemAccess.forHandlerIndex(
                        net.neoforged.neoforge.transfer.item.VanillaContainerWrapper.of(scratch),
                        0);
        var handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) throw new AssertionError("Missing native 26.x cell capability");
        try (var transaction = Transaction.openRoot()) {
            if (handler.insert(FluidResource.of(fluid), fluid.getAmount(), transaction)
                    != fluid.getAmount())
                throw new AssertionError("Could not fill native 26.x cell");
            transaction.commit();
        }
        return scratch.getItem(0).copy();
    }

    static int itemFluidVolume(ItemStack stack) {
        var handler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
        return handler == null ? 0 : handler.getAmountAsInt(0);
    }
}
