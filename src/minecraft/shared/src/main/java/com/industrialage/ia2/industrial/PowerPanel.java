package com.industrialage.ia2.industrial;

import com.industrialage.ia2.gui.PanelCanvas;
import net.minecraft.network.chat.Component;

/** Fills the active regions of the redrawn IC2 control panels from synchronized server readings. */
final class PowerPanel {
    private static final int INK = 0xff263340, LIGHT = 0xffcee4e8;

    private PowerPanel() {}

    static void draw(PowerMenu menu, Component title, PanelCanvas g) {
        var layout = menu.layout;
        int width = layout.width(),
                energy = menu.value(2),
                capacity = menu.value(14),
                status = menu.value(12);
        if (layout.assembled()) {
            g.frame(0, 0, width, layout.height());
            for (var slot : menu.slots) if (slot.isActive()) g.slot(slot.x, slot.y);
        } else g.background(layout.texture(), width, layout.height());
        if (menu.kind != PowerKind.NUKE) {
            int headingWidth = width - (menu.kind.storage() ? 56 : menu.core() ? 76 : 16);
            g.fittedText(title, width / 2, 6, headingWidth, INK);
        }
        if (menu.core()) {
            for (int i = 0; i < 54; i++)
                if (!menu.exposed(i)) {
                    var slot = menu.getSlot(i);
                    g.blit(layout.texture(), slot.x - 1, slot.y - 1, 212, 0, 18, 18);
                }
            int heat = menu.value(4), limit = menu.value(6);
            bar(g, layout.texture(), 8, 137, 0, 243, 98, 6, heat, limit);
            String output = menu.value(8) / 10.0 + " EU/t";
            g.text(
                    Component.literal(
                            menu.layout.texture().endsWith("fluid")
                                    ? menu.data.get(11) + " mB"
                                    : output),
                    157,
                    137,
                    LIGHT,
                    true);
            if (menu.layout.texture().endsWith("fluid")) {
                gauge(g, 9, 53, 12, 48, menu.data.get(10), 10000, 0xff32a7c6);
                gauge(g, 191, 53, 12, 48, menu.data.get(11), 10000, 0xffb45339);
            }
        } else if (menu.kind.lowTier() > 0) {
            g.text(Component.translatable("ic2.panel.transform." + status), 95, 29, LIGHT, true);
            g.text(Component.literal(energy + " EU"), 95, 45, LIGHT, true);
        } else if (menu.kind.storage()) {
            g.text(Component.literal(Integer.toString(status)), 162, 9, INK, true);
            bar(g, layout.texture(), 79, 36, 176, 16, 24, 8, energy, capacity);
            g.text(Component.literal(energy + " EU"), 121, 55, INK, true);
        } else if (menu.kind == PowerKind.GEO_GENERATOR) {
            bar(g, layout.texture(), 112, 27, 176, 0, 24, 8, energy, capacity);
            gauge(g, 73, 25, 8, 44, status, 8000, 0xffd36f26);
        } else if (menu.kind.trader()) {
            if (menu.guest) {
                g.text(
                        Component.translatable(
                                menu.kind == PowerKind.ENERGY_O_MAT
                                        ? "ic2.panel.credit"
                                        : "ic2.panel.trades",
                                status),
                        10,
                        34,
                        INK,
                        false);
            } else if (menu.kind == PowerKind.ENERGY_O_MAT) {
                g.text(Component.literal(menu.data.get(16) + " EU"), 133, 25, INK, true);
                g.text(Component.literal("−       +"), 133, 43, INK, true);
            } else g.text(Component.translatable("ic2.panel.trades", status), 116, 39, INK, true);
        } else if (menu.kind == PowerKind.MAGNETIZER) {
            g.text(Component.literal(energy + " / " + capacity + " EU"), 80, 66, LIGHT, true);
        } else if (menu.kind == PowerKind.REACTOR_FLUID_PORT) {
            g.text(Component.literal(menu.data.get(10) + " mB"), 41, 65, INK, true);
            g.text(Component.literal(menu.data.get(11) + " mB"), 135, 65, INK, true);
        } else if (layout.assembled()) {
            if (menu.kind == PowerKind.RT_GENERATOR) {
                g.blit("common", 115, 38, 128, 7, 32, 17);
                bar(g, "common", 119, 42, 132, 43, 24, 8, energy, capacity);
            } else if (menu.kind == PowerKind.RCI_LZH || menu.kind == PowerKind.RCI_RSH) {
                g.blit("common", 8, 40, 100, 64, 12, 14);
            } else if (menu.kind == PowerKind.SOLAR_GENERATOR) {
                g.blit(
                        "overlay/solar_sun",
                        81,
                        47,
                        menu.data.get(17) > 0 ? 14 : 0,
                        0,
                        14,
                        14,
                        28,
                        14);
            } else if (menu.kind == PowerKind.TELEPORTER) {
                g.text(Component.translatable("ic2.panel.teleporter"), width / 2, 40, INK, true);
            } else g.text(Component.literal(energy + " EU"), 44, 39, INK, true);
        }
    }

    private static void gauge(
            PanelCanvas canvas, int x, int y, int w, int h, int n, int max, int color) {
        int filled = Math.max(0, Math.min(h, (int) ((long) n * h / max)));
        if (filled > 0) canvas.fill(x, y + h - filled, x + w, y + h, color);
    }

    private static void bar(
            PanelCanvas canvas,
            String texture,
            int x,
            int y,
            int u,
            int v,
            int w,
            int h,
            int n,
            int max) {
        int filled = max <= 0 ? 0 : Math.max(0, Math.min(w, (int) ((long) n * w / max)));
        if (filled > 0) canvas.blit(texture, x, y, u, v, filled, h);
    }
}
