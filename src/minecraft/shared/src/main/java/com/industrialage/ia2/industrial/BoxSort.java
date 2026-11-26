package com.industrialage.ia2.industrial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative inventory consolidation and deterministic sorting for storage boxes. */
final class BoxSort {
    private static final BoxSorter.StackOps<ItemStack> OPS =
            BoxSorter.ops(
                    ItemStack::isEmpty,
                    ItemStack::getCount,
                    ItemStack::getMaxStackSize,
                    WorkshopPlatform::sameItem,
                    ItemStack::copyWithCount,
                    ItemStack::setCount,
                    BoxSort::sortKey);

    private BoxSort() {}

    /**
     * Consolidates and sorts slots {@code 0 .. count - 1} of {@code inventory} in place.
     *
     * <p>Respects both {@link Container#getMaxStackSize()} and {@link ItemStack#getMaxStackSize()},
     * writes empty stacks into trailing slots, and marks the container changed.
     */
    static void sort(Container inventory, int slotCount) {
        int count = Math.min(slotCount, inventory.getContainerSize());
        if (count <= 0) return;

        List<ItemStack> collected = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                collected.add(stack);
            }
        }

        List<ItemStack> sorted =
                BoxSorter.mergeAndSort(collected, inventory.getMaxStackSize(), OPS);
        if (sorted.size() > count) {
            // Defensive guard: never drop stacks if an invariant were ever broken externally.
            return;
        }

        for (int i = 0; i < count; i++) {
            inventory.setItem(i, i < sorted.size() ? sorted.get(i) : ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    static BoxSorter.SortKey sortKey(ItemStack stack) {
        var itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemId = itemKey == null ? "" : itemKey.toString();
        int damage = stack.getDamageValue();
        String displayName = stack.getHoverName().getString();
        String components = componentSignature(stack);
        return new BoxSorter.SortKey(itemId, damage, displayName, components, stack.getCount());
    }

    static String componentSignature(ItemStack stack) {
        var patch = stack.getComponentsPatch();
        if (patch.isEmpty()) return "";
        List<String> entries = new ArrayList<>(patch.size());
        for (var entry : patch.entrySet()) {
            var typeKey = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
            String id = typeKey == null ? String.valueOf(entry.getKey()) : typeKey.toString();
            String value = entry.getValue().map(Object::toString).orElse("!");
            entries.add(id + "=" + value);
        }
        return formatComponentEntries(entries);
    }

    static String formatComponentEntries(List<String> entries) {
        if (entries == null || entries.isEmpty()) return "";
        List<String> sorted = new ArrayList<>(entries);
        Collections.sort(sorted);
        return String.join(";", sorted);
    }
}
