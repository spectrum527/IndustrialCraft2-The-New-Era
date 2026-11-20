package com.industrialage.ia2.content;

import java.util.HashMap;
import java.util.Map;

/** Measured effects of exposed inventory items in IC2's fictional radiation system. */
public final class RadiationRules {
    /** Native status duration in game ticks and the in-game effect amplifier. */
    public record Dose(int duration, int amplifier) {}

    private static final Map<String, Dose> EXPOSURES = load();

    private RadiationRules() {}

    private static Map<String, Dose> load() {
        Map<String, Dose> result = new HashMap<>();
        for (String row : IndustrialMaterials.rows("radioactive-items.tsv")) {
            var fields = row.split("\t");
            result.put(
                    fields[0], new Dose(Integer.parseInt(fields[1]), Integer.parseInt(fields[2])));
        }
        return Map.copyOf(result);
    }

    /** Effect from one unshielded item, or null for material with no measured exposure. */
    public static Dose exposure(String id) {
        return EXPOSURES.get(id.startsWith("ic2:") ? id.substring(4) : id);
    }

    /** Whether the item is eligible for the shielding container. */
    public static boolean isRadioactive(String id) {
        return exposure(id) != null;
    }

    /** Damage per successful status pulse; measured low doses cause half a point. */
    public static float damage(int amplifier) {
        return 0.5F + Math.max(0, amplifier) / 100;
    }

    /** The game schedules radiation pulses more often for higher effect levels. */
    public static boolean damageTick(int ticks, int amplifier) {
        return ticks % Math.max(1, 25 >> Math.max(0, Math.min(5, amplifier))) == 0;
    }
}
