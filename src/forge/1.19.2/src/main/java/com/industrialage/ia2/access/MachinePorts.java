package com.industrialage.ia2.access;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * Native sided views retain slot restrictions and consult current machine permissions on every
 * transfer.
 */
public final class MachinePorts {
    private MachinePorts() {}

    /** Wraps an inventory supplied by the machine for this world face. */
    public static IItemHandler item(BlockEntity machine, Direction side, IItemHandler original) {
        return new Inventory(machine, side, original);
    }

    /** Wraps a tank supplied by the machine for this world face. */
    public static IFluidHandler fluid(BlockEntity machine, Direction side, IFluidHandler original) {
        return new Tank(machine, side, original);
    }

    private record Inventory(BlockEntity machine, Direction side, IItemHandler source)
            implements IItemHandler {
        @Override
        public int getSlots() {
            return source.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return source.getStackInSlot(slot);
        }

        @Override
        public int getSlotLimit(int slot) {
            return source.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return MachineAccess.allowsInput(machine, side) && source.isItemValid(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return MachineAccess.allowsInput(machine, side)
                    ? source.insertItem(slot, stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int count, boolean simulate) {
            return MachineAccess.allowsOutput(machine, side)
                    ? source.extractItem(slot, count, simulate)
                    : ItemStack.EMPTY;
        }
    }

    private record Tank(BlockEntity machine, Direction side, IFluidHandler source)
            implements IFluidHandler {
        @Override
        public int getTanks() {
            return source.getTanks();
        }

        @Override
        public int getTankCapacity(int tank) {
            return source.getTankCapacity(tank);
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return source.getFluidInTank(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack fluid) {
            return MachineAccess.allowsInput(machine, side) && source.isFluidValid(tank, fluid);
        }

        @Override
        public int fill(FluidStack fluid, FluidAction action) {
            return MachineAccess.allowsInput(machine, side) ? source.fill(fluid, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack fluid, FluidAction action) {
            return MachineAccess.allowsOutput(machine, side)
                    ? source.drain(fluid, action)
                    : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return MachineAccess.allowsOutput(machine, side)
                    ? source.drain(amount, action)
                    : FluidStack.EMPTY;
        }
    }
}
