package com.industrialage.ia2.equipment;

import com.industrialage.ia2.industrial.PowerNode;
import com.industrialage.ia2.industrial.WorkshopNode;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** Reads the same wind and obstruction values used by the generating devices. */
public final class WindMeter {
    private WindMeter() {}

    /** Localized result and whether the instrument obtained an actual reading. */
    public record Reading(Component message, boolean available) {}

    /** Formats ambient, generating, or missing-rotor feedback for the selected position. */
    public static Reading read(ServerLevel level, BlockPos position) {
        var device = level.getBlockEntity(position);
        if (device instanceof WorkshopNode kinetic && kinetic.windRotor() >= 0) {
            return kinetic.windRotor() == 0
                    ? new Reading(
                            Component.translatable("item.ic2.wind_meter.missing_rotor"), false)
                    : new Reading(
                            Component.translatable(
                                    "item.ic2.wind_meter.kinetic", kinetic.windPotential(level)),
                            true);
        }
        if (device instanceof PowerNode generator) {
            double output = generator.measuredWind(level);
            if (output >= 0)
                return new Reading(
                        Component.translatable("item.ic2.wind_meter.generator", decimal(output)),
                        true);
        }
        return new Reading(
                Component.translatable(
                        "item.ic2.wind_meter.ambient",
                        decimal(PowerNode.ambientWind(level, position.getY()))),
                true);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
