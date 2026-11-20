package com.industrialage.ia2.industrial;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Native client view of the live, server-owned workshop inventory. */
public final class WorkshopScreen extends AbstractContainerScreen<WorkshopMenu> {
    private static final Identifier ART =
            Identifier.fromNamespaceAndPath("ic2", WorkshopPanel.INVENTORY_TEXTURE);
    private Button control;

    /** Creates the screen for the machine menu sent by the server. */
    public WorkshopScreen(WorkshopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WorkshopMenu.WIDTH, WorkshopMenu.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        if (menu.hasControl()) {
            control =
                    addRenderableWidget(
                            Button.builder(
                                            menu.controlLabel(),
                                            button ->
                                                    minecraft.gameMode.handleInventoryButtonClick(
                                                            menu.containerId, 0))
                                    .bounds(leftPos + 166, topPos + 6, 112, 18)
                                    .build());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (control != null) {
            control.setMessage(menu.controlLabel());
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        WorkshopPanel.surface(
                menu,
                (x1, y1, x2, y2, color) ->
                        graphics.fill(leftPos + x1, topPos + y1, leftPos + x2, topPos + y2, color));
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ART,
                leftPos + 56,
                topPos + 153,
                0,
                79,
                176,
                84,
                256,
                256);
        super.extractContents(graphics, mouseX, mouseY, tick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        WorkshopPanel.labels(
                menu,
                title,
                playerInventoryTitle,
                mouseX - leftPos,
                mouseY - topPos,
                graphics::fill,
                (text, x, y, color, max) -> drawLine(graphics, text, x, y, color, max));
    }

    private void drawLine(
            GuiGraphicsExtractor graphics, Component text, int x, int y, int color, int maxWidth) {
        int width = font.width(text);
        if (width <= maxWidth) {
            graphics.text(font, text, x, y, color, false);
            return;
        }
        float scale = (float) maxWidth / width;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y + (1 - scale) * 4);
        pose.scale(scale, scale);
        graphics.text(font, text, 0, 0, color, false);
        pose.popMatrix();
    }
}
