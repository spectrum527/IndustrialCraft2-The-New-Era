package com.industrialage.ia2.industrial;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Live native capability for the geothermal inlet or an intact reactor fluid port. */
final class LavaInlet implements IFluidHandler {
    private final PowerNode node;

    LavaInlet(PowerNode node) {
        this.node = node;
    }

    private boolean geothermal() {
        return node.kind == PowerKind.GEO_GENERATOR;
    }

    @Override
    public int getTanks() {
        return geothermal() ? 1 : 2;
    }

    @Override
    public int getTankCapacity(int slot) {
        return slot < 0 || slot >= getTanks()
                ? 0
                : geothermal() ? 8000 : ReactorVessel.TANK_CAPACITY;
    }

    @Override
    public FluidStack getFluidInTank(int slot) {
        if (getTankCapacity(slot) == 0) return FluidStack.EMPTY;
        if (geothermal()) return WorkshopPlatform.fluid("minecraft:lava", node.liquid);
        var tank = node.coolantTank();
        return tank == null
                ? FluidStack.EMPTY
                : WorkshopPlatform.fluid(
                        slot == 0 ? "ic2:coolant" : "ic2:hot_coolant",
                        tank.coolantAmount(slot == 1));
    }

    @Override
    public boolean isFluidValid(int slot, FluidStack stack) {
        return slot == 0
                && !stack.isEmpty()
                && WorkshopPlatform.fluidId(stack)
                        .equals(geothermal() ? "minecraft:lava" : "ic2:coolant");
    }

    @Override
    public int fill(FluidStack stack, FluidAction action) {
        if (!isFluidValid(0, stack)) return 0;
        if (geothermal()) return node.fillLava(stack.getAmount(), action.simulate());
        var tank = node.coolantTank();
        if (tank == null) return 0;
        int accepted = Math.min(stack.getAmount(), ReactorVessel.TANK_CAPACITY - tank.coolant);
        if (accepted > 0 && action.execute()) {
            tank.coolant += accepted;
            tank.setChanged();
        }
        return accepted;
    }

    @Override
    public FluidStack drain(FluidStack stack, FluidAction action) {
        if (stack.isEmpty() || !WorkshopPlatform.fluidId(stack).equals("ic2:hot_coolant"))
            return FluidStack.EMPTY;
        return drain(stack.getAmount(), action);
    }

    @Override
    public FluidStack drain(int max, FluidAction action) {
        if (max <= 0 || geothermal()) return FluidStack.EMPTY;
        var tank = node.coolantTank();
        if (tank == null) return FluidStack.EMPTY;
        int removed = Math.min(max, tank.hotCoolant);
        if (removed > 0 && action.execute()) {
            tank.hotCoolant -= removed;
            tank.setChanged();
        }
        return WorkshopPlatform.fluid("ic2:hot_coolant", removed);
    }
}
