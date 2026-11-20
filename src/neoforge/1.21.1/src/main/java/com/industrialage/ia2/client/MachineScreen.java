package com.industrialage.ia2.client;

import com.industrialage.ia2.gameplay.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Native interactive machine screen for Minecraft 1.21.1. */
final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = MachineMenu.WIDTH;
        imageHeight = MachineMenu.HEIGHT;
        inventoryLabelY = 84;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachinePanel.draw(
                menu,
                (x1, y1, x2, y2, color) ->
                        graphics.fill(leftPos + x1, topPos + y1, leftPos + x2, topPos + y2, color),
                (line, x, y, center) ->
                        graphics.drawString(
                                font,
                                line,
                                leftPos + x - (center ? font.width(line) / 2 : 0),
                                topPos + y,
                                0xffd6e6f0,
                                false));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xffedf3f8, false);
        graphics.drawString(
                font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xffd6e6f0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
