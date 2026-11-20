package com.industrialage.ia2.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native draggable inventory with IC2's authored background and synchronized menu data. */
public abstract class AuthoredScreen<M extends AbstractContainerMenu>
        extends AbstractContainerScreen<M> {
    protected AuthoredScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title);
        imageWidth = width;
        imageHeight = height;
    }

    /** Menu command for an authored control; a negative value delegates normal inventory input. */
    protected int actionAt(int x, int y, int button) {
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int action =
                actionAt(
                        (int) Math.floor(mouseX - leftPos),
                        (int) Math.floor(mouseY - topPos),
                        button);
        if (action < 0) return super.mouseClicked(mouseX, mouseY, button);
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        return true;
    }

    /** Paint background, inlaid controls and text before native item stacks are rendered. */
    protected abstract void paint(PanelCanvas canvas);

    @Override
    protected void renderBg(GuiGraphics g, float tick, int mouseX, int mouseY) {
        paint(PanelGraphics.at(g, font, leftPos, topPos));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics g, int x, int y, float tick) {
        super.render(g, x, y, tick);
        renderTooltip(g, x, y);
    }
}
