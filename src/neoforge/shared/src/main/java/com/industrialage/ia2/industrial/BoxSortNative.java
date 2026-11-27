package com.industrialage.ia2.industrial;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Platform-specific item identity and component metadata signature for {@link BoxSort}. */
final class BoxSortNative {
    private BoxSortNative() {}

    static String itemId(ItemStack stack) {
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    static String metadataSignature(ItemStack stack) {
        var patch = stack.getComponentsPatch();
        if (patch.isEmpty()) return "";
        List<String> entries = new ArrayList<>(patch.size());
        for (var entry : patch.entrySet()) {
            var typeKey = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
            String id = typeKey == null ? String.valueOf(entry.getKey()) : typeKey.toString();
            String value = entry.getValue().map(Object::toString).orElse("!");
            entries.add(id + "=" + value);
        }
        return BoxSort.formatComponentEntries(entries);
    }
}
