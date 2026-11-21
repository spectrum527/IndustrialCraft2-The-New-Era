package com.industrialage.ia2.industrial;

import com.industrialage.ia2.logistics.CellFamily;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

final class WorkshopCells {
    static final int VOLUME = CellFamily.CAPACITY;

    private WorkshopCells() {}

    static boolean canDrain(ItemStack stack, int capacity, String fluid) {
        return canDrain(stack, capacity, fluid::equals);
    }

    static boolean canDrain(ItemStack stack, int capacity, Predicate<String> accepts) {
        if (stack.isEmpty()) return false;
        var moved = WorkshopPlatform.drainWholeContainer(new WorkshopNode.Tank(capacity), stack);
        return moved != null && accepts.test(moved.liquid().id());
    }

    static WorkshopPlans.ContainerResult measure(ItemStack stack, String fluid) {
        if (stack.isEmpty()) return null;
        var result = WorkshopPlatform.drainWholeContainer(new WorkshopNode.Tank(VOLUME), stack);
        return result != null
                        && result.liquid().amount() == VOLUME
                        && result.liquid().id().equals(fluid)
                ? result
                : null;
    }

    static WorkshopPlans.Liquid wholeContents(
            WorkshopNode.Tank tank, int compartments, IntFunction<WorkshopPlans.Liquid> read) {
        int space = tank.capacity - tank.amount;
        String fluid = tank.id;
        int total = 0;
        for (int slot = 0; slot < compartments; slot++) {
            var part = read.apply(slot);
            if (part == null) return WorkshopPlans.Liquid.EMPTY;
            if (part.amount() <= 0) continue;
            if (part.amount() > space - total || !fluid.isEmpty() && !fluid.equals(part.id())) {
                return WorkshopPlans.Liquid.EMPTY;
            }
            fluid = part.id();
            total += part.amount();
        }
        return total == 0 ? WorkshopPlans.Liquid.EMPTY : new WorkshopPlans.Liquid(fluid, total);
    }
}
