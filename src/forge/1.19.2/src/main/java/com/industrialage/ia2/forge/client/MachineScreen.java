package com.industrialage.ia2.forge.client;

import com.industrialage.ia2.forge.content.MachineMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private Button redstone;

    MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        if (menu.storageControl()) {
            redstone =
                    addRenderableWidget(
                            new Button(
                                    leftPos + 12,
                                    topPos + 28,
                                    26,
                                    20,
                                    modeNumber(),
                                    button ->
                                            minecraft.gameMode.handleInventoryButtonClick(
                                                    menu.containerId, 0)));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (redstone != null) {
            redstone.setMessage(modeNumber());
        }
    }

    private Component modeNumber() {
        return Component.literal(Integer.toString(menu.redstoneMode()));
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        renderTooltip(pose, mouseX, mouseY);
        if (redstone != null && redstone.isHoveredOrFocused()) {
            renderTooltip(
                    pose,
                    Component.translatable("gui.ic2.redstone." + menu.redstoneMode()),
                    mouseX,
                    mouseY);
        }
        if (hoveredSlot != null && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            var label = menu.slotLabel(hoveredSlot.index);
            if (label != null) {
                renderTooltip(pose, label, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        fill(pose, left, top, left + imageWidth, top + imageHeight, 0xFF292D32);
        fill(pose, left + 2, top + 2, left + imageWidth - 2, top + imageHeight - 2, 0xFFC7CDD0);
        fill(pose, left + 4, top + 3, left + imageWidth - 4, top + 72, 0xFFDEE3E4);
        for (var slot : menu.slots) {
            int x = left + slot.x;
            int y = top + slot.y;
            fill(pose, x - 1, y - 1, x + 17, y + 17, 0xFF56626A);
            fill(pose, x, y, x + 16, y + 16, 0xFF9CA8AE);
            fill(pose, x, y + 16, x + 17, y + 17, 0xFFF2F4F4);
            fill(pose, x + 16, y, x + 17, y + 17, 0xFFF2F4F4);
        }
        if (!menu.storageControl()) {
            meter(pose, left + 78, top + 37, 27, menu.progressRatio(), 0xFF648A2B);
        }
        if (menu.capacity() > 0) {
            meter(
                    pose,
                    left + 85,
                    top + 55,
                    78,
                    (float) menu.energy() / menu.capacity(),
                    0xFF237785);
        }
        if (menu.active()) {
            fill(pose, left + 160, top + 7, left + 168, top + 15, 0xFF6C932F);
        }
    }

    private static void meter(PoseStack pose, int x, int y, int width, float ratio, int color) {
        fill(pose, x, y, x + width, y + 4, 0xFF77838B);
        fill(pose, x, y, x + Mth.clamp(Math.round(ratio * width), 0, width), y + 4, color);
    }

    @Override
    protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        super.renderLabels(pose, mouseX, mouseY);
        if (menu.capacity() > 0) {
            var label = Component.translatable("gui.ic2.energy", menu.energy(), menu.capacity());
            font.draw(pose, label, 164 - font.width(label), 62, 0x293137);
        }
    }
}
