package com.industrialage.ia2.industrial;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
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
                            new Button(
                                    leftPos + 166,
                                    topPos + 6,
                                    112,
                                    18,
                                    menu.controlLabel(),
                                    button ->
                                            minecraft.gameMode.handleInventoryButtonClick(
                                                    menu.containerId, 0)));
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
    protected void renderBg(PoseStack graphics, float tick, int mouseX, int mouseY) {
        WorkshopPanel.surface(
                menu,
                (x1, y1, x2, y2, color) ->
                        fill(
                                graphics,
                                leftPos + x1,
                                topPos + y1,
                                leftPos + x2,
                                topPos + y2,
                                color));
        RenderSystem.setShaderTexture(0, ART);
        blit(graphics, leftPos + 56, topPos + 153, 0, 79, 176, 84);
    }

    @Override
    protected void renderLabels(PoseStack graphics, int mouseX, int mouseY) {
        WorkshopPanel.labels(
                menu,
                title,
                playerInventoryTitle,
                mouseX - leftPos,
                mouseY - topPos,
                (x1, y1, x2, y2, color) -> fill(graphics, x1, y1, x2, y2, color),
                (text, x, y, color, max) -> drawLine(graphics, text, x, y, color, max));
    }

    @Override
    public void render(PoseStack graphics, int mouseX, int mouseY, float tick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, tick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void drawLine(
            PoseStack graphics, Component text, int x, int y, int color, int maxWidth) {
        int width = font.width(text);
        if (width <= maxWidth) {
            font.draw(graphics, text, x, y, color);
            return;
        }
        float scale = (float) maxWidth / width;
        var pose = graphics;
        pose.pushPose();
        pose.translate(x, y + (1 - scale) * 4, 0);
        pose.scale(scale, scale, 1);
        font.draw(graphics, text, 0, 0, color);
        pose.popPose();
    }
}
