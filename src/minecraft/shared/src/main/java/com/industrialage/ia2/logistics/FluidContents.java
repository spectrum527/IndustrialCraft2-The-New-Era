package com.industrialage.ia2.logistics;

import net.minecraft.network.chat.Component;

/** Read-only snapshot of one physical tank, in millibuckets. */
public record FluidContents(Component name, int amount, int capacity) {
    /** Snapshot of an empty tank with its installed capacity. */
    public static FluidContents empty(int capacity) {
        return new FluidContents(Component.translatable("top.ic2.empty"), 0, capacity);
    }
}
