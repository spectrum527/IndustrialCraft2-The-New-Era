package com.industrialage.ia2.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Native button input over an already-painted control in the player's authored artwork. */
public final class PanelButton extends Button {
    /** Creates a clickable, focusable hit area whose art is rendered by the owning screen. */
    public PanelButton(int x, int y, int width, int height, Component title, OnPress action) {
        super(x, y, width, height, title, action, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {}
}
