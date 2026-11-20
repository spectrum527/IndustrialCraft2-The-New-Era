package com.industrialage.ia2.gui;

import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shift transfers respect active pages and input restrictions even when merging stacks. */
public final class MenuTransfer {
    private MenuTransfer() {}

    public static boolean move(
            ItemStack source,
            List<Slot> slots,
            int start,
            int end,
            boolean reverse,
            BiPredicate<ItemStack, ItemStack> same) {
        boolean moved = false;
        for (int pass = 0; pass < 2 && !source.isEmpty(); pass++) {
            for (int step = 0; step < end - start && !source.isEmpty(); step++) {
                Slot slot = slots.get(reverse ? end - 1 - step : start + step);
                if (!slot.isActive() || !slot.mayPlace(source)) continue;
                ItemStack present = slot.getItem();
                if (pass == 0) {
                    if (present.isEmpty() || !source.isStackable() || !same.test(source, present))
                        continue;
                    int amount =
                            Math.min(
                                    source.getCount(),
                                    slot.getMaxStackSize(source) - present.getCount());
                    if (amount > 0) {
                        present.grow(amount);
                        source.shrink(amount);
                        slot.setChanged();
                        moved = true;
                    }
                } else {
                    if (!present.isEmpty()) continue;
                    int amount = Math.min(source.getCount(), slot.getMaxStackSize(source));
                    if (amount > 0) {
                        slot.set(source.split(amount));
                        slot.setChanged();
                        moved = true;
                    }
                }
            }
        }
        return moved;
    }
}
