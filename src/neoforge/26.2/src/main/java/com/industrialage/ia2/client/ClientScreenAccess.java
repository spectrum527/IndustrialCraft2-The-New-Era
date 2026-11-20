package com.industrialage.ia2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Reads and opens screens managed by Gui in Minecraft 26.2. */
final class ClientScreenAccess {
    private ClientScreenAccess() {}

    static boolean hasScreenOrOverlay(Minecraft client) {
        var gui = client.gui;
        return gui.screen() != null || gui.overlay() != null;
    }

    static boolean open(Minecraft client, Screen screen) {
        var gui = client.gui;
        gui.setScreen(screen);
        return gui.screen() == screen;
    }
}
