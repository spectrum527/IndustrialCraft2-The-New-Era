package com.industrialage.ia2.world;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Shared curing state of an electrically functional cable enclosed in construction foam. */
public final class FoamCasing {
    /** Marks a casing that has hardened naturally or after application of sand. */
    public static final BooleanProperty HARDENED = BooleanProperty.create("hardened");

    private FoamCasing() {}
}
