package com.industrialage.ia2.compat;

import net.minecraft.resources.Identifier;

final class TopBlockProvider extends TopProbe.Blocks {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("ic2", "contents");

    @Override
    public Identifier getID() {
        return ID;
    }
}
