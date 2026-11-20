package com.industrialage.ia2.equipment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The physical EU reader panel on the extraction rendering API. */
final class MeterScreen extends AbstractContainerScreen<MeterMenu> {
    private Button mode;

    MeterScreen(MeterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MeterPanel.WIDTH, MeterPanel.HEIGHT);
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
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff60798a);
        graphics.fill(
                leftPos + 2,
                topPos + 2,
                leftPos + imageWidth - 2,
                topPos + imageHeight - 2,
                0xff202e38);
        super.extractContents(graphics, mouseX, mouseY, tick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 8, 8, 0xffedf3f8, false);
        MeterPanel.values(menu, (label, y) -> graphics.text(font, label, 8, y, 0xffd6e6f0, false));
    }
}
