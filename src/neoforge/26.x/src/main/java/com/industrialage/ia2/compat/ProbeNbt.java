package com.industrialage.ia2.compat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;

/** NeoForge 26.x CompoundTag readers for shared probe telemetry. */
final class ProbeNbt {
    private ProbeNbt() {}

    static boolean getBoolean(CompoundTag tag, String key) {
        return tag.getBooleanOr(key, false);
    }

    static double getDouble(CompoundTag tag, String key) {
        return tag.getDoubleOr(key, 0.0);
    }

    static int getInt(CompoundTag tag, String key) {
        return tag.getIntOr(key, 0);
    }

    static String getString(CompoundTag tag, String key) {
        return tag.getStringOr(key, "");
    }

    static List<CompoundTag> getCompounds(CompoundTag tag, String key) {
        var list = tag.getListOrEmpty(key);
        var result = new ArrayList<CompoundTag>(list.size());
        for (int i = 0; i < list.size(); i++) {
            list.getCompound(i).ifPresent(result::add);
        }
        return result;
    }
}
