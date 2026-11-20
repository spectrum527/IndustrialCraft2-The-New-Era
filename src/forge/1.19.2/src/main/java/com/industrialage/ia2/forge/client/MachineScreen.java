package com.industrialage.ia2.forge.client;

import com.industrialage.ia2.client.MachineArtwork;
import com.industrialage.ia2.content.MachineLayout;
import com.industrialage.ia2.forge.content.MachineMenu;
import com.industrialage.ia2.gui.PanelButton;
import com.industrialage.ia2.gui.PanelGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Native interaction and authored machine art for Minecraft 1.19.2. */
final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private PanelButton redstone;

    MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = MachineLayout.WIDTH;
        imageHeight = menu.layout().height();
    }

    @Override
    protected void init() {
        super.init();
        if (menu.storageControl()) {
            redstone =
                    addRenderableWidget(
                            new PanelButton(
                                    leftPos + 151,
                                    topPos + 3,
                                    21,
                                    22,
                                    redstoneMode(),
                                    button ->
                                            minecraft.gameMode.handleInventoryButtonClick(
                                                    menu.containerId, 0)));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (redstone != null) redstone.setMessage(redstoneMode());
    }

    private Component redstoneMode() {
        return Component.translatable("gui.ic2.redstone." + menu.redstoneMode());
    }

    @Override
    public void render(PoseStack graphics, int mouseX, int mouseY, float tick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, tick);
        renderTooltip(graphics, mouseX, mouseY);
        if (redstone != null && redstone.isHoveredOrFocused()) {
            renderTooltip(graphics, redstoneMode(), mouseX, mouseY);
        } else if (menu.getCarried().isEmpty()) {
            Component tip =
                    hoveredSlot != null && !hoveredSlot.hasItem()
                            ? menu.slotLabel(hoveredSlot.index)
                            : MachineArtwork.gaugeTip(
                                    menu.layout(),
                                    mouseX - leftPos,
                                    mouseY - topPos,
                                    menu.energy(),
                                    menu.capacity(),
                                    menu.progress(),
                                    menu.duration());
            if (tip != null) renderTooltip(graphics, tip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(PoseStack graphics, float tick, int mouseX, int mouseY) {
        double energy = menu.capacity() > 0 ? (double) menu.energy() / menu.capacity() : 0;
        MachineArtwork.draw(
                menu.layout(),
                menu.machineSlots(),
                menu::slotRole,
                energy,
                menu.progressRatio(),
                menu.fuelRatio(),
                PanelGraphics.at(graphics, font, leftPos, topPos));
    }

    @Override
    protected void renderLabels(PoseStack graphics, int mouseX, int mouseY) {
        MachineArtwork.labels(
                menu.layout(),
                title,
                playerInventoryTitle,
                menu.redstoneMode(),
                PanelGraphics.at(graphics, font, 0, 0));
    }
}
