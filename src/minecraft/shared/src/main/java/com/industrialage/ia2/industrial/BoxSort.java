package com.industrialage.ia2.industrial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Server-authoritative inventory consolidation and deterministic sorting for storage boxes. */
final class BoxSort {
    private static final BoxSorter.StackOps<ItemStack> OPS =
            BoxSorter.ops(
                    ItemStack::isEmpty,
                    ItemStack::getCount,
                    ItemStack::getMaxStackSize,
                    WorkshopPlatform::sameItem,
                    BoxSort::copyWithCount,
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

    private static ItemStack copyWithCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    static BoxSorter.SortKey sortKey(ItemStack stack) {
        var itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemKey == null) itemKey = Registry.ITEM.getKey(stack.getItem());
        String itemId = itemKey == null ? "" : itemKey.toString();
        int damage = stack.getDamageValue();
        String displayName = stack.getHoverName().getString();
        String tags = tagSignature(stack);
        return new BoxSorter.SortKey(itemId, damage, displayName, tags, stack.getCount());
    }

    static String tagSignature(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.isEmpty()) return "";
        return canonicalTag(tag);
    }

    static String canonicalTag(Tag tag) {
        if (tag == null) return "";
        if (tag instanceof CompoundTag compound) {
            List<String> keys = new ArrayList<>(compound.getAllKeys());
            Collections.sort(keys);
            StringBuilder out = new StringBuilder("{");
            for (int i = 0; i < keys.size(); i++) {
                if (i > 0) out.append(',');
                String key = keys.get(i);
                out.append(key).append('=').append(canonicalTag(compound.get(key)));
            }
            return out.append('}').toString();
        }
        if (tag instanceof ListTag list) {
            StringBuilder out = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) out.append(',');
                out.append(canonicalTag(list.get(i)));
            }
            return out.append(']').toString();
        }
        return tag.toString();
    }
}
