package com.industrialage.ia2.equipment;

import com.industrialage.ia2.gui.PanelCanvas;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Coordinates and readouts aligned with the author's handheld EU reader artwork. */
final class MeterPanel {
    static final int WIDTH = 176;
    static final int HEIGHT = 217;
    static final int INVENTORY_TOP = 136;
    static final int HOTBAR_TOP = 194;
    static final int DIAL_X = 112;
    static final int DIAL_Y = 55;
    static final int DIAL_SIZE = 40;
    static final int RESET_X = 25;
    static final int RESET_Y = 110;
    static final int RESET_WIDTH = 60;
    static final int RESET_HEIGHT = 14;
    private static final String ART = "guitooleumeter";
    private static final int READOUT = 0xff91ff86;

    private MeterPanel() {}

    static Component mode(MeterMenu menu) {
        return Component.translatable("container.ic2.meter.mode." + menu.mode());
    }

    static Component resetLabel() {
        return Component.translatable("container.ic2.meter.reset");
    }

    static void draw(MeterMenu menu, PanelCanvas canvas) {
        canvas.background(ART, WIDTH, HEIGHT);
        canvas.blit(ART, DIAL_X, DIAL_Y, 176, menu.mode() * DIAL_SIZE, DIAL_SIZE, DIAL_SIZE);
    }

    static void labels(MeterMenu menu, Component title, PanelCanvas canvas) {
        var reading = menu.reading();
        canvas.text(title, WIDTH / 2, 6, 0xff26323a, true);
        canvas.text(Component.translatable("container.ic2.meter.mean"), 14, 43, READOUT, false);
        canvas.text(value(menu.mode(), reading.average()), 14, 53, READOUT, false);
        canvas.text(Component.translatable("container.ic2.meter.range"), 14, 65, READOUT, false);
        canvas.text(value(menu.mode(), reading.max()), 14, 76, READOUT, false);
        canvas.text(value(menu.mode(), reading.min()), 14, 86, READOUT, false);
        canvas.text(
                Component.translatable(
                        "container.ic2.meter.period", decimal(reading.count() / 20.0)),
                14,
                99,
                READOUT,
                false);
        canvas.text(Component.translatable("container.ic2.meter.dial"), 132, 42, READOUT, true);
        canvas.text(mode(menu), 132, 99, READOUT, true);
        canvas.text(resetLabel(), RESET_X + RESET_WIDTH / 2, 113, READOUT, true);
    }

    private static Component value(int mode, double amount) {
        return Component.literal(
                mode == MeterMenu.MODES - 1 ? decimal(amount) : decimal(amount) + " EU/t");
    }

    private static String decimal(double amount) {
        return String.format(Locale.ROOT, "%.3f", amount);
    }
}
