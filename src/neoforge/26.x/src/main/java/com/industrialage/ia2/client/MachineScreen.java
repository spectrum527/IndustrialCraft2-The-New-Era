package com.industrialage.ia2.client;

import com.industrialage.ia2.content.MachineLayout;
import com.industrialage.ia2.gameplay.MachineMenu;
import com.industrialage.ia2.gui.DeviceScreen;
import com.industrialage.ia2.gui.PanelButton;
import com.industrialage.ia2.gui.PanelGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Native interaction and authored machine art for Minecraft 26.x. */
final class MachineScreen extends DeviceScreen<MachineMenu> {
    private PanelButton redstone;

    MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MachineLayout.WIDTH, menu.layout().height());
    }

    @Override
    protected void init() {
        super.init();
        if (menu.layout().storage()) {
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
            redstone.setTooltip(Tooltip.create(redstoneMode()));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (redstone != null) {
            var mode = redstoneMode();
            if (!redstone.getMessage().equals(mode)) {
                redstone.setMessage(mode);
                redstone.setTooltip(Tooltip.create(mode));
            }
        }
    }

    private Component redstoneMode() {
        return Component.translatable("container.ic2.redstone_mode." + menu.redstoneMode());
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        double energy = menu.capacity() > 0 ? (double) menu.energy() / menu.capacity() : 0;
        double work = menu.duration() > 0 ? (double) menu.progress() / menu.duration() : 0;
        MachineArtwork.draw(
                menu.layout(),
                menu.machineSlots(),
                menu::slotRole,
                energy,
                work,
                menu.fuelRatio(),
                PanelGraphics.at(graphics, font, leftPos, topPos));
        super.extractContents(graphics, mouseX, mouseY, tick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MachineArtwork.labels(
                menu.layout(),
                title,
                playerInventoryTitle,
                menu.redstoneMode(),
                PanelGraphics.at(graphics, font, 0, 0));
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (menu.getCarried().isEmpty()) {
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
            if (tip != null) graphics.setTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }
}
