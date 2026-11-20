package com.industrialage.ia2.client;

import com.industrialage.ia2.content.MachineLayout;
import com.industrialage.ia2.content.MachineLayout.Kind;
import com.industrialage.ia2.gui.PanelCanvas;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;

/** Composes the author's panel and gauges at the native container anchors. */
public final class MachineArtwork {
    private static final String COMMON = "common";

    private MachineArtwork() {}

    /** Paints static authored art and the portions of sprites indicated by live menu data. */
    public static void draw(
            MachineLayout layout,
            int machineSlots,
            IntFunction<Kind> slotRole,
            double energy,
            double work,
            double heat,
            PanelCanvas blit) {
        if (layout.storage()) {
            String atlas =
                    layout == MachineLayout.CHARGE_PAD ? "guichargepadblock" : "guielectricblock";
            sprite(blit, atlas, 0, 0, 0, 0, MachineLayout.WIDTH, layout.height());
            horizontal(blit, atlas, 79, 38, 176, 18, 24, 9, energy);
            return;
        }
        blit.frame(0, 0, MachineLayout.WIDTH, layout.height());
        for (int index = 0; index < machineSlots; index++) {
            Kind kind = slotRole.apply(index);
            var at = layout.frame(kind, Math.max(0, index - 3));
            if (kind == Kind.OUTPUT) {
                sprite(blit, COMMON, at.x(), at.y(), 99, 35, 26, 26);
            } else {
                blit.slot(at.x() + 1, at.y() + 1);
            }
        }
        blit.inventory(8, layout.inventoryTop() + 1, layout.hotbarTop() + 1);

        if (layout == MachineLayout.GENERATOR) {
            sprite(blit, COMMON, 100, 39, 128, 7, 32, 17);
            horizontal(blit, COMMON, 104, 43, 132, 43, 24, 9, energy);
        } else {
            progress(blit, layout, work);
        }
        if (layout == MachineLayout.IRON_FURNACE || layout == MachineLayout.GENERATOR) {
            int x = layout == MachineLayout.GENERATOR ? 57 : 56;
            sprite(blit, COMMON, x, 36, 96, 80, 12, 13);
            vertical(blit, x, 36, 112, 80, 12, 13, heat);
        } else {
            sprite(blit, COMMON, 59, 37, 100, 65, 7, 13);
            vertical(blit, 59, 37, 116, 65, 7, 13, energy);
        }
    }

    /** Draws the labels in the neutral areas left by the authored frames. */
    public static void labels(
            MachineLayout layout,
            Component title,
            Component inventoryTitle,
            int redstoneMode,
            PanelCanvas canvas) {
        int ink = 0xff26323a;
        canvas.text(title, MachineLayout.WIDTH / 2, 6, ink, true);
        canvas.text(inventoryTitle, 8, layout.inventoryTop() - 11, ink, false);
        if (layout.storage()) {
            canvas.text(Component.literal(Integer.toString(redstoneMode)), 162, 11, ink, true);
        }
    }

    /** Describes the live work or energy gauge under the pointer. */
    public static Component gaugeTip(
            MachineLayout layout,
            int x,
            int y,
            int energy,
            int capacity,
            int progress,
            int duration) {
        boolean energyGauge =
                switch (layout) {
                    case STORAGE, CHARGE_PAD -> within(x, y, 75, 34, 32, 17);
                    case GENERATOR -> within(x, y, 100, 39, 32, 17);
                    case IRON_FURNACE -> false;
                    default -> within(x, y, 59, 37, 7, 13);
                };
        if (capacity > 0 && energyGauge) {
            return Component.translatable("container.ic2.energy", energy, capacity);
        }
        if (duration > 0 && within(x, y, 80, 35, 22, 15)) {
            return Component.translatable("container.ic2.progress", progress, duration);
        }
        return null;
    }

    private static boolean within(int x, int y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < left + width && y < top + height;
    }

    private static void progress(PanelCanvas blit, MachineLayout layout, double work) {
        int y = layout == MachineLayout.MACERATOR ? 38 : 35;
        int v =
                switch (layout) {
                    case MACERATOR -> 34;
                    case COMPRESSOR -> 65;
                    case EXTRACTOR -> 97;
                    default -> 0;
                };
        int height =
                switch (layout) {
                    case MACERATOR -> 11;
                    case COMPRESSOR, EXTRACTOR -> 13;
                    default -> 15;
                };
        int width =
                layout == MachineLayout.COMPRESSOR || layout == MachineLayout.EXTRACTOR ? 20 : 21;
        sprite(blit, COMMON, 80, y, 166, v, width, height);
        horizontal(blit, COMMON, 80, y, 166, v + 16, width, height, work);
    }

    private static void horizontal(
            PanelCanvas blit,
            String atlas,
            int x,
            int y,
            int u,
            int v,
            int width,
            int height,
            double ratio) {
        sprite(blit, atlas, x, y, u, v, portion(width, ratio), height);
    }

    private static void vertical(
            PanelCanvas blit, int x, int y, int u, int v, int width, int height, double ratio) {
        int part = portion(height, ratio);
        sprite(blit, COMMON, x, y + height - part, u, v + height - part, width, part);
    }

    private static int portion(int extent, double ratio) {
        return (int) Math.ceil(extent * Math.max(0, Math.min(1, ratio)));
    }

    private static void sprite(
            PanelCanvas blit, String atlas, int x, int y, int u, int v, int width, int height) {
        if (width > 0 && height > 0) blit.blit(atlas, x, y, u, v, width, height);
    }
}
