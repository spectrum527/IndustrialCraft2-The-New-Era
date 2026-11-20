package com.industrialage.ia2.equipment;

import java.util.Locale;
import java.util.function.BiConsumer;
import net.minecraft.network.chat.Component;

/** Co-locates the four meter measurements and their labels for all client renderers. */
final class MeterPanel {
    static final int WIDTH = 176;
    static final int HEIGHT = 104;

    private MeterPanel() {}

    static Component mode(MeterMenu menu) {
        return Component.translatable("container.ic2.meter.mode." + menu.mode());
    }

    static void values(MeterMenu menu, BiConsumer<Component, Integer> text) {
        var reading = menu.reading();
        text.accept(Component.translatable("container.ic2.meter.min", decimal(reading.min())), 49);
        text.accept(Component.translatable("container.ic2.meter.max", decimal(reading.max())), 62);
        text.accept(
                Component.translatable("container.ic2.meter.average", decimal(reading.average())),
                75);
        text.accept(Component.translatable("container.ic2.meter.samples", reading.count()), 88);
    }

    private static String decimal(double amount) {
        return String.format(Locale.ROOT, "%.3f", amount);
    }
}
