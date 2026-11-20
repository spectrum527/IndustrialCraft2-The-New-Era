package com.industrialage.ia2.industrial;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Native client view of the live, server-owned workshop inventory. */
public final class WorkshopScreen extends AbstractContainerScreen<WorkshopMenu> {
    private static final ResourceLocation ART =
            ResourceLocation.fromNamespaceAndPath("ic2", WorkshopPanel.INVENTORY_TEXTURE);
    private Button control;

    /** Creates the screen for the machine menu sent by the server. */
    public WorkshopScreen(WorkshopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WorkshopMenu.WIDTH;
        imageHeight = WorkshopMenu.HEIGHT;
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
    protected void renderBg(GuiGraphics graphics, float tick, int mouseX, int mouseY) {
        WorkshopPanel.surface(
                menu,
                (x1, y1, x2, y2, color) ->
                        graphics.fill(leftPos + x1, topPos + y1, leftPos + x2, topPos + y2, color));
        graphics.blit(ART, leftPos + 56, topPos + 153, 0, 79, 176, 84);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        WorkshopPanel.labels(
                menu,
                title,
                playerInventoryTitle,
                mouseX - leftPos,
                mouseY - topPos,
                graphics::fill,
                (text, x, y, color, max) -> drawLine(graphics, text, x, y, color, max));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        super.render(graphics, mouseX, mouseY, tick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void drawLine(
            GuiGraphics graphics, Component text, int x, int y, int color, int maxWidth) {
        int width = font.width(text);
        if (width <= maxWidth) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        float scale = (float) maxWidth / width;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y + (1 - scale) * 4, 0);
        pose.scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, color, false);
        pose.popPose();
    }
}
