package com.industrialage.ia2.industrial;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Draws the same live industrial panel over the native inventory protocol on this game version. */
final class PowerScreen extends AbstractContainerScreen<PowerMenu> {
    PowerScreen(PowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = menu.width();
        imageHeight = menu.height();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float tick, int mouseX, int mouseY) {
        PowerPanel.draw(
                menu,
                title,
                new PowerPanel.Canvas() {
                    public void rect(int x, int y, int r, int b, int c) {
                        graphics.fill(leftPos + x, topPos + y, leftPos + r, topPos + b, c);
                    }

                    public void text(Component t, int x, int y, int c, boolean center) {
                        graphics.drawString(
                                font,
                                t,
                                leftPos + x - (center ? font.width(t) / 2 : 0),
                                topPos + y,
                                c,
                                false);
                    }

                    public void texture(String id, int width, int height) {
                        graphics.blit(
                                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                        "ic2", "textures/gui/" + id + ".png"),
                                leftPos,
                                topPos,
                                0,
                                0,
                                width,
                                height,
                                256,
                                256);
                    }
                });
    }

    @Override
    protected void renderLabels(GuiGraphics g, int x, int y) {}

    @Override
    public void render(GuiGraphics g, int x, int y, float tick) {
        super.render(g, x, y, tick);
        renderTooltip(g, x, y);
    }
}
