package com.industrialage.ia2.client;

import com.industrialage.ia2.content.MachineLayout;
import com.industrialage.ia2.gameplay.MachineMenu;
import com.industrialage.ia2.gui.AuthoredScreen;
import com.industrialage.ia2.gui.PanelButton;
import com.industrialage.ia2.gui.PanelCanvas;
import com.industrialage.ia2.gui.PanelGraphics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Native interaction and authored machine art for Minecraft 1.21.1. */
final class MachineScreen extends AuthoredScreen<MachineMenu> {
    private PanelButton redstone;

    MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MachineLayout.WIDTH, menu.layout().height());
    }

    @Override
    protected void init() {
        super.init();
        redstone = null;
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
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (redstone != null) {
            var mode = redstoneMode();
            if (!redstone.getMessage().equals(mode)) {
                redstone.setMessage(mode);
            }
        }
    }

    private Component redstoneMode() {
        return Component.translatable("container.ic2.redstone_mode." + menu.redstoneMode());
    }

    @Override
    protected void paint(PanelCanvas canvas) {
        double energy = menu.capacity() > 0 ? (double) menu.energy() / menu.capacity() : 0;
        double work = menu.duration() > 0 ? (double) menu.progress() / menu.duration() : 0;
        MachineArtwork.draw(
                menu.layout(),
                menu.machineSlots(),
                menu::slotRole,
                energy,
                work,
                menu.fuelRatio(),
                canvas);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        MachineArtwork.labels(
                menu.layout(),
                title,
                playerInventoryTitle,
                menu.redstoneMode(),
                PanelGraphics.at(graphics, font, 0, 0));
    }

    @Override
    protected Component tooltipAt(int x, int y) {
        if (redstone != null && x >= 151 && x < 172 && y >= 3 && y < 25) {
            return redstoneMode();
        }
        return hoveredSlot != null && !hoveredSlot.hasItem()
                ? menu.slotLabel(hoveredSlot.index)
                : MachineArtwork.gaugeTip(
                        menu.layout(),
                        x,
                        y,
                        menu.energy(),
                        menu.capacity(),
                        menu.progress(),
                        menu.duration());
    }
}
