package com.industrialage.ia2.equipment;

import com.mojang.blaze3d.vertex.PoseStack;
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
                        new Button(
                                leftPos + 8,
                                topPos + 21,
                                imageWidth - 16,
                                20,
                                MeterPanel.mode(menu),
                                button ->
                                        minecraft.gameMode.handleInventoryButtonClick(
                                                menu.containerId,
                                                (menu.mode() + 1) % MeterMenu.MODES)));
    }

    @Override
    protected void containerTick() {
        mode.setMessage(MeterPanel.mode(menu));
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float tick) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, tick);
    }

    @Override
    protected void renderBg(PoseStack graphics, float tick, int mouseX, int mouseY) {
        fill(graphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff60798a);
        fill(
                graphics,
                leftPos + 2,
                topPos + 2,
                leftPos + imageWidth - 2,
                topPos + imageHeight - 2,
                0xff202e38);
    }

    @Override
    protected void renderLabels(PoseStack graphics, int mouseX, int mouseY) {
        font.draw(graphics, title, 8, 8, 0xffedf3f8);
        MeterPanel.values(menu, (label, y) -> font.draw(graphics, label, 8, y, 0xffd6e6f0));
    }
}
