package com.industrialage.ia2.industrial;

import com.industrialage.ia2.gui.PanelGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Draws the device's returned industrial artwork around its real server inventory. */
public final class WorkshopScreen extends AbstractContainerScreen<WorkshopMenu> {
    private final List<Button> controls = new ArrayList<>();

    /** Creates the screen for the machine menu sent by the server. */
    public WorkshopScreen(WorkshopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = menu.layout().width;
        imageHeight = menu.layout().height;
    }

    @Override
    protected void init() {
        super.init();
        controls.clear();
        for (var control : menu.layout().controls) {
            var area = control.area();
            var widget =
                    new Button(
                            leftPos + area.x(),
                            topPos + area.y(),
                            area.width(),
                            area.height(),
                            control.caption(menu),
                            button ->
                                    minecraft.gameMode.handleInventoryButtonClick(
                                            menu.containerId, control.action())) {
                        @Override
                        public void renderButton(
                                PoseStack graphics, int mouseX, int mouseY, float tick) {
                            // The authored atlas supplies the visible button face.
                        }
                    };
            controls.add(addRenderableWidget(widget));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < controls.size(); i++) {
            controls.get(i).setMessage(menu.layout().controls.get(i).caption(menu));
        }
    }

    @Override
    protected void renderBg(PoseStack graphics, float tick, int mouseX, int mouseY) {
        WorkshopPanel.background(menu, PanelGraphics.at(graphics, font, leftPos, topPos));
        var mode = WorkshopPanel.modePicture(menu);
        if (mode != null) {
            itemRenderer.renderAndDecorateItem(mode.item(), leftPos + mode.x(), topPos + mode.y());
        }
    }

    @Override
    protected void renderLabels(PoseStack graphics, int mouseX, int mouseY) {
        WorkshopPanel.labels(
                menu,
                title,
                playerInventoryTitle,
                PanelGraphics.at(graphics, font, 0, 0),
                (label, width) ->
                        font.width(label) <= width
                                ? label
                                : Component.literal(
                                        font.plainSubstrByWidth(
                                                        label.getString(), Math.max(1, width - 6))
                                                + "…"));
    }

    @Override
    protected void renderTooltip(PoseStack graphics, int mouseX, int mouseY) {
        boolean item = hoveredSlot != null && hoveredSlot.hasItem();
        var details =
                WorkshopPanel.tooltip(
                        menu,
                        mouseX - leftPos,
                        mouseY - topPos,
                        hoveredSlot == null ? -1 : hoveredSlot.index,
                        item);
        if (!details.isEmpty()) {
            renderComponentTooltip(graphics, details, mouseX, mouseY);
        } else {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public void render(PoseStack graphics, int mouseX, int mouseY, float tick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, tick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
