package com.industrialage.ia2.equipment;

import com.industrialage.ia2.gui.PanelButton;
import com.industrialage.ia2.gui.PanelGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Author-drawn EU reader and native inventory on Minecraft 1.19.2. */
final class MeterScreen extends AbstractContainerScreen<MeterMenu> {
    private PanelButton mode;

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
                        new PanelButton(
                                leftPos + MeterPanel.DIAL_X,
                                topPos + MeterPanel.DIAL_Y,
                                MeterPanel.DIAL_SIZE,
                                MeterPanel.DIAL_SIZE,
                                MeterPanel.mode(menu),
                                button -> press((menu.mode() + 1) % MeterMenu.MODES)));
        addRenderableWidget(
                new PanelButton(
                        leftPos + MeterPanel.RESET_X,
                        topPos + MeterPanel.RESET_Y,
                        MeterPanel.RESET_WIDTH,
                        MeterPanel.RESET_HEIGHT,
                        MeterPanel.resetLabel(),
                        button -> press(MeterMenu.RESET)));
    }

    private void press(int action) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        mode.setMessage(MeterPanel.mode(menu));
    }

    @Override
    public void render(PoseStack graphics, int mouseX, int mouseY, float tick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, tick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack graphics, float tick, int mouseX, int mouseY) {
        MeterPanel.draw(menu, PanelGraphics.at(graphics, font, leftPos, topPos));
    }

    @Override
    protected void renderLabels(PoseStack graphics, int mouseX, int mouseY) {
        MeterPanel.labels(menu, title, PanelGraphics.at(graphics, font, 0, 0));
    }
}
