package com.industrialage.ia2.compat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** NeoForge 1.21.1 CompoundTag readers for shared probe telemetry. */
final class ProbeNbt {
    private ProbeNbt() {}

    static boolean getBoolean(CompoundTag tag, String key) {
        return tag.getBoolean(key);
    }

    static double getDouble(CompoundTag tag, String key) {
        return tag.getDouble(key);
    }

    static int getInt(CompoundTag tag, String key) {
        return tag.getInt(key);
    }

    static String getString(CompoundTag tag, String key) {
        return tag.getString(key);
    }

    static List<CompoundTag> getCompounds(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) return List.of();
        var list = tag.getList(key, Tag.TAG_COMPOUND);
        var result = new ArrayList<CompoundTag>(list.size());
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getCompound(i));
        }
        return result;
    }
}
