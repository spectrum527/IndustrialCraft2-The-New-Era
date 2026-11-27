package com.industrialage.ia2.industrial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Platform-specific item identity and canonical NBT signature for {@link BoxSort}. */
final class BoxSortNative {
    private BoxSortNative() {}

    static String itemId(ItemStack stack) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) key = Registry.ITEM.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    static String metadataSignature(ItemStack stack) {
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
