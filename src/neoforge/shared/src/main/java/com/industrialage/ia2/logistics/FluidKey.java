package com.industrialage.ia2.logistics;

import net.neoforged.neoforge.fluids.FluidStack;

/** Fluid identity with its full data components and without an associated volume. */
final class FluidKey {
    private final FluidStack fluid;

    FluidKey(FluidStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Empty fluid has no transport identity");
        }
        fluid = stack.copyWithAmount(1);
    }

    FluidStack stack(int amount) {
        return fluid.copyWithAmount(amount);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FluidKey key
                && FluidStack.isSameFluidSameComponents(fluid, key.fluid);
    }

    @Override
    public int hashCode() {
        return FluidStack.hashFluidAndComponents(fluid);
    }
}
