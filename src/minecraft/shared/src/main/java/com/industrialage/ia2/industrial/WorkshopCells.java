package com.industrialage.ia2.industrial;

import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

final class WorkshopCells {
    static final int VOLUME = 1000;
    static final Map<String, String> FILLED =
            Map.of(
                    "air_cell", "ic2:air",
                    "water_cell", "minecraft:water",
                    "lava_cell", "minecraft:lava");

    private WorkshopCells() {}

    static boolean canDrain(ItemStack stack, int capacity, String fluid) {
        return canDrain(stack, capacity, fluid::equals);
    }

    static boolean canDrain(ItemStack stack, int capacity, Predicate<String> accepts) {
        if (stack.isEmpty()) return false;
        var moved = WorkshopPlatform.drainWholeContainer(new WorkshopNode.Tank(capacity), stack);
        return moved != null && accepts.test(moved.liquid().id());
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

    static WorkshopPlans.ContainerResult namedFill(WorkshopNode.Tank tank, ItemStack stack) {
        if (tank.amount < VOLUME
                || !WorkshopPlatform.sameItem(stack, WorkshopPlatform.stack("ic2:empty_cell", 1))) {
            return null;
        }
        var filled = namedStack(tank.id);
        return filled.isEmpty()
                ? null
                : new WorkshopPlans.ContainerResult(
                        filled, new WorkshopPlans.Liquid(tank.id, VOLUME));
    }

    static ItemStack namedStack(String fluid) {
        for (var cell : FILLED.entrySet()) {
            if (cell.getValue().equals(fluid)) {
                return WorkshopPlatform.stack("ic2:" + cell.getKey(), 1);
            }
        }
        return ItemStack.EMPTY;
    }
}
