package com.industrialage.ia2.industrial;

import java.util.Map;
import net.minecraft.world.item.ItemStack;

final class WorkshopCells {
    static final int VOLUME = 1000;
    static final Map<String, String> FILLED =
            Map.of(
                    "air_cell", "ic2:air",
                    "water_cell", "minecraft:water",
                    "lava_cell", "minecraft:lava");

    private WorkshopCells() {}

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
