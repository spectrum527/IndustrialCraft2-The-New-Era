package com.industrialage.ia2.compat;

import net.minecraft.resources.ResourceLocation;

final class TopBlockProvider extends TopProbe.Blocks {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("ic2", "contents");

    @Override
    public ResourceLocation getID() {
        return ID;
    }
}
