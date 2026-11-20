package com.industrialage.ia2.industrial;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Maps each native sided capability onto the processor's intake and product tanks. */
record WorkshopFluidPort(WorkshopNode node, Direction side) {
    int size() {
        var output = output();
        return (node.exposesIntake(side) ? 1 : 0)
                + (output != null && (output != node.inlet || !node.exposesIntake(side)) ? 1 : 0);
    }

    WorkshopNode.Tank tank(int slot) {
        if (slot < 0 || slot >= size()) {
            return null;
        }
        return slot == 0 && node.exposesIntake(side) ? node.inlet : output();
    }

    boolean canInsert(int slot, String fluid) {
        return slot == 0 && node.exposesIntake(side) && node.canReceiveFluid(fluid);
    }

    int insert(String fluid, int requested, boolean simulate) {
        return canInsert(0, fluid)
                ? node.receiveFluid(
                        fluid, requested, simulate || !(node.getLevel() instanceof ServerLevel))
                : 0;
    }

    WorkshopNode.Tank output() {
        return node.accessibleOutput(side);
    }

    int extract(String fluid, int requested, boolean simulate) {
        return node.outputFluid(
                output(), fluid, requested, simulate || !(node.getLevel() instanceof ServerLevel));
    }
}
