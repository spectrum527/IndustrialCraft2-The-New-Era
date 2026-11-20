package com.industrialage.ia2.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native draggable inventory with IC2's authored background and synchronized menu data. */
public abstract class AuthoredScreen<M extends AbstractContainerMenu>
        extends AbstractContainerScreen<M> {
    protected AuthoredScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    /** Menu command for an authored control; a negative value delegates normal inventory input. */
    protected int actionAt(int x, int y, int button) {
        return -1;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent button, boolean twice) {
        int action =
                actionAt(
                        (int) Math.floor(button.x() - leftPos),
                        (int) Math.floor(button.y() - topPos),
                        button.button());
        if (action < 0) return super.mouseClicked(button, twice);
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        return true;
    }

    /** Paint background, inlaid controls and text before native item stacks are rendered. */
    protected abstract void paint(PanelCanvas canvas);

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float tick) {
        paint(PanelGraphics.at(g, font, leftPos, topPos));
        super.extractContents(g, mouseX, mouseY, tick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}
}
