package com.industrialage.ia2.logistics;

import net.minecraftforge.fluids.FluidStack;

final class FluidKey {
    private final FluidStack prototype;

    FluidKey(FluidStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Empty fluid cannot identify a liquid");
        }
        prototype = new FluidStack(stack, 1);
    }

    FluidStack stack(int amount) {
        return new FluidStack(prototype, amount);
    }

    int registryId() {
        return net.minecraft.core.Registry.FLUID.getId(stack(1).getFluid());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FluidKey key && prototype.isFluidEqual(key.prototype);
    }

    @Override
    public int hashCode() {
        return 31 * prototype.getFluid().hashCode()
                + java.util.Objects.hashCode(prototype.getTag());
    }
}
