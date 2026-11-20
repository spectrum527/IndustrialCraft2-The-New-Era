package com.industrialage.ia2.industrial;

import net.minecraft.network.chat.Component;

final class WorkshopPanel {
    private static final int INK = 0xffedf2f5;
    private static final int FAINT = 0xffb6c7d3;
    private static final int CYAN = 0xff72bac9;
    private static final int STEEL = 0xff12232e;
    private static final int TRACK = 0xff344852;
    static final String INVENTORY_TEXTURE = "textures/gui/guimetalformer.png";

    private WorkshopPanel() {}

    static void surface(WorkshopMenu menu, Paint paint) {
        paint.fill(0, 0, WorkshopMenu.WIDTH, WorkshopMenu.HEIGHT, 0xff192631);
        paint.fill(1, 1, WorkshopMenu.WIDTH - 1, WorkshopMenu.HEIGHT - 1, 0xff768898);
        paint.fill(3, 3, WorkshopMenu.WIDTH - 3, WorkshopMenu.HEIGHT - 3, 0xff2a3946);
        paint.fill(8, 28, 150, 147, 0xff203443);
        paint.fill(154, 28, WorkshopMenu.WIDTH - 8, 147, STEEL);
        paint.fill(8, 27, WorkshopMenu.WIDTH - 8, 28, CYAN);
        paint.fill(8, 149, WorkshopMenu.WIDTH - 8, 151, 0xff8195a4);
        for (int index = 0; index < WorkshopInventory.SIZE; index++) {
            var slot = menu.slots.get(index);
            if (slot.isActive()) {
                paint.fill(slot.x - 1, slot.y - 1, slot.x + 17, slot.y + 17, 0xff728896);
                paint.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xff0d1e2b);
                paint.fill(slot.x + 16, slot.y, slot.x + 17, slot.y + 17, 0xffa8bbc4);
                paint.fill(slot.x, slot.y + 16, slot.x + 17, slot.y + 17, 0xffa8bbc4);
            }
        }
    }

    static void labels(
            WorkshopMenu menu,
            Component title,
            Component inventoryTitle,
            int pointerX,
            int pointerY,
            Paint paint,
            Draw draw) {
        draw.line(title, 11, 10, INK, menu.hasControl() ? 145 : 266);
        draw.line(inventoryTitle, 10, 154, FAINT, 44);
        if (menu.slots.get(0).isActive()) {
            var caption = label(menu.slots.get(1).isActive() ? "inputs" : "input");
            draw.line(caption, 12, 42, CYAN, 113);
        }
        if (menu.slots.get(WorkshopInventory.AUX_SLOT).isActive()
                && menu.slots.get(WorkshopInventory.AUX_SLOT).x == 120) {
            draw.line(label("auxiliary"), 115, 42, CYAN, 34);
        }
        if (menu.slots.get(WorkshopInventory.OUTPUT_START).isActive()) {
            draw.line(label("outputs"), 12, 74, CYAN, 107);
        }
        if (menu.slots.get(WorkshopInventory.RETURN_SLOT).isActive()) {
            draw.line(label("returned"), 108, 74, CYAN, 41);
        }
        if (menu.slots.get(WorkshopMatter.FIRST_PATTERN).isActive()) {
            int y = menu.slots.get(WorkshopMatter.FIRST_PATTERN).y - 13;
            draw.line(label("patterns"), 12, y, CYAN, 113);
        }
        if (menu.slots.get(WorkshopInventory.UPGRADE_START).isActive()
                && menu.slots.get(WorkshopInventory.UPGRADE_START).y == 125) {
            draw.line(label("upgrades"), 12, 112, CYAN, 125);
        }
        for (int index = 0; index < WorkshopInventory.SIZE; index++) {
            var slot = menu.slots.get(index);
            if (slot.isActive()
                    && pointerX >= slot.x - 1
                    && pointerX <= slot.x + 16
                    && pointerY >= slot.y - 1
                    && pointerY <= slot.y + 16) {
                draw.line(menu.slotLabel(index), 160, 137, CYAN, 116);
                break;
            }
        }

        int y = 34;
        draw.line(menu.job(), 160, y, INK, 110);
        if (menu.active()) {
            paint.fill(271, y + 1, 276, y + 6, 0xff8ede83);
        }
        y += 13;
        if (menu.duration() > 0) {
            draw.line(
                    Component.translatable(
                            "gui.ic2.workshop.progress", menu.progress(), menu.duration()),
                    160,
                    y,
                    FAINT,
                    116);
            y += 10;
            meter(paint, y, menu.progress(), menu.duration(), 0xffa4c276);
            y += 7;
        }
        if (menu.capacity() > 0) {
            draw.line(
                    Component.translatable(
                            "gui.ic2.workshop.energy", menu.energy(), menu.capacity()),
                    160,
                    y,
                    INK,
                    116);
            y += 10;
            meter(paint, y, menu.energy(), menu.capacity(), CYAN);
            y += 7;
        }
        if (menu.heatMeter()) {
            draw.line(
                    Component.translatable("gui.ic2.workshop.heat", menu.heat()),
                    160,
                    y,
                    FAINT,
                    116);
            y += 12;
        }
        for (int index = 0; index < 3; index++) {
            var tank = menu.tank(index);
            if (tank.maximum() > 0) {
                draw.line(
                        Component.translatable(
                                "gui.ic2.workshop.tank_meter",
                                label(tank.role()),
                                tank.amount(),
                                tank.maximum()),
                        160,
                        y,
                        INK,
                        116);
                draw.line(tank.liquid(), 160, y + 9, CYAN, 116);
                y += 19;
            }
        }
    }

    private static void meter(Paint paint, int y, int value, int maximum, int color) {
        paint.fill(160, y, 276, y + 3, TRACK);
        int width = (int) Math.max(0, Math.min(116, 116L * value / maximum));
        paint.fill(160, y, 160 + width, y + 3, color);
    }

    private static Component label(String key) {
        return Component.translatable("gui.ic2.workshop." + key);
    }

    @FunctionalInterface
    interface Paint {
        void fill(int x1, int y1, int x2, int y2, int color);
    }

    @FunctionalInterface
    interface Draw {
        void line(Component text, int x, int y, int color, int maxWidth);
    }
}
