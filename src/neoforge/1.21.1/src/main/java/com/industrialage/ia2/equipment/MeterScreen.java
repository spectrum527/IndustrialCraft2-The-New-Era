package com.industrialage.ia2.equipment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The physical EU reader panel on the legacy rendering API. */
final class MeterScreen extends AbstractContainerScreen<MeterMenu> {
    private Button mode;

    MeterScreen(MeterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = MeterPanel.WIDTH;
        imageHeight = MeterPanel.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        mode =
                addRenderableWidget(
                        Button.builder(
                                        MeterPanel.mode(menu),
                                        button ->
                                                Minecraft.getInstance()
                                                        .gameMode
                                                        .handleInventoryButtonClick(
                                                                menu.containerId,
                                                                (menu.mode() + 1)
                                                                        % MeterMenu.MODES))
                                .bounds(leftPos + 8, topPos + 21, imageWidth - 16, 20)
                                .build());
    }

    @Override
    protected void containerTick() {
        mode.setMessage(MeterPanel.mode(menu));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float tick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff60798a);
        graphics.fill(
                leftPos + 2,
                topPos + 2,
                leftPos + imageWidth - 2,
                topPos + imageHeight - 2,
                0xff202e38);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 8, 0xffedf3f8, false);
        MeterPanel.values(
                menu, (label, y) -> graphics.drawString(font, label, 8, y, 0xffd6e6f0, false));
    }
}
