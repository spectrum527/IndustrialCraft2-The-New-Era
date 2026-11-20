package com.industrialage.ia2.world;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Shared cable block-state dye for display, saves, and electrical connections. */
public final class CableColors {
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 15);

    private CableColors() {}

    /** Identifies insulation and glass fibre that can carry a painted color. */
    public static boolean paintable(String cable) {
        return cable.contains("insulated_") || cable.equals("glass_fibre_cable");
    }

    /** Returns the persisted dye number, with zero representing unpainted cable. */
    public static int color(BlockState state) {
        return state.hasProperty(COLOR) ? state.getValue(COLOR) : 0;
    }

    /** Models and energy routing share the same connection rule. */
    public static boolean compatible(BlockState first, BlockState second) {
        int a = color(first);
        int b = color(second);
        return a == 0 || b == 0 || a == b;
    }

    static int dye(String name) {
        for (DyeColor dye : DyeColor.values()) {
            if (dye.getName().equals(name)) return dye == DyeColor.BLACK ? 0 : dye.getId() + 1;
        }
        return -1;
    }
}
