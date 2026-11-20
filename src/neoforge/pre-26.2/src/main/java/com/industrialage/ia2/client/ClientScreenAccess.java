package com.industrialage.ia2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Reads and opens screens managed by Minecraft in 1.21.1 and 26.1.2. */
final class ClientScreenAccess {
    private ClientScreenAccess() {}

    static boolean hasScreenOrOverlay(Minecraft client) {
        return client.screen != null || client.getOverlay() != null;
    }

    static boolean open(Minecraft client, Screen screen) {
        client.setScreen(screen);
        return client.screen == screen;
    }
}
