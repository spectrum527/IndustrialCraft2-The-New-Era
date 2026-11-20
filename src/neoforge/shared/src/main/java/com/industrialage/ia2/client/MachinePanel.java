package com.industrialage.ia2.client;

import com.industrialage.ia2.gameplay.MachineMenu;
import net.minecraft.network.chat.Component;

/** Pixel layout of the machine panel shared by the two client rendering APIs. */
final class MachinePanel {
    private MachinePanel() {}

    static void draw(MachineMenu menu, Rect fill, Text text) {
        fill.paint(0, 0, MachineMenu.WIDTH, MachineMenu.HEIGHT, 0xff171f25);
        fill.paint(1, 1, MachineMenu.WIDTH - 1, MachineMenu.HEIGHT - 1, 0xff465762);
        fill.paint(3, 3, MachineMenu.WIDTH - 3, MachineMenu.HEIGHT - 3, 0xff29353d);
        fill.paint(5, 20, MachineMenu.WIDTH - 5, 81, 0xff172028);
        for (int i = 0; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            fill.paint(slot.x - 1, slot.y - 1, slot.x + 17, slot.y + 17, 0xff566c78);
            fill.paint(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xff121b23);
        }
        for (int i = 0; i < menu.machineSlots(); i++) {
            if (!menu.upgradeSlot(i))
                text.paint(menu.slotLabel(i), menu.machineSlotX(i) + 8, 26, true);
        }
        if (menu.capacity() > 0) {
            text.paint(
                    Component.translatable(
                            "container.ic2.energy",
                            energyText(menu.energy()),
                            energyText(menu.capacity())),
                    9,
                    61,
                    false);
        }
        if (menu.duration() > 0) {
            text.paint(
                    Component.translatable(
                            "container.ic2.progress", menu.progress(), menu.duration()),
                    9,
                    71,
                    false);
            meter(fill, (double) menu.progress() / menu.duration(), 0xff65c9a3);
        } else if (menu.usesFuel()) {
            text.paint(
                    Component.translatable("container.ic2.fuel_time", menu.fuelRemaining()),
                    9,
                    71,
                    false);
        }
        if (menu.duration() == 0 && menu.capacity() > 0) {
            meter(fill, (double) menu.energy() / menu.capacity(), 0xffe8b564);
        }
    }

    private static String energyText(int eu) {
        if (eu < 1_000_000) return Integer.toString(eu);
        return (eu / 100_000 / 10.0) + "M";
    }

    private static void meter(Rect fill, double ratio, int color) {
        fill.paint(9, 80, 166, 82, 0xff354752);
        int width = (int) Math.round(157 * Math.clamp(ratio, 0.0, 1.0));
        if (width > 0) {
            fill.paint(9, 80, 9 + width, 82, color);
        }
    }

    @FunctionalInterface
    interface Rect {
        void paint(int x1, int y1, int x2, int y2, int color);
    }

    @FunctionalInterface
    interface Text {
        void paint(Component line, int x, int y, boolean center);
    }
}
