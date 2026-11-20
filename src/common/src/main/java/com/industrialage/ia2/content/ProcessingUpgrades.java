package com.industrialage.ia2.content;

import java.util.function.ToIntFunction;

/** Processing speed, capacity and packet tier after fitting the four upgrade slots. */
public record ProcessingUpgrades(int overclockers, int transformers, int storage) {
    /** An empty upgrade inventory. */
    public static final ProcessingUpgrades NONE = new ProcessingUpgrades(0, 0, 0);

    /** Bounds upgrade counts to the capacity of four full item stacks. */
    public ProcessingUpgrades {
        overclockers = clamp(overclockers, 256);
        transformers = clamp(transformers, 256 - overclockers);
        storage = clamp(storage, 256 - overclockers - transformers);
    }

    /** Reads upgrade counts from a host inventory by qualified item ID. */
    public static ProcessingUpgrades count(ToIntFunction<String> total) {
        return new ProcessingUpgrades(
                total.applyAsInt("ic2:overclocker_upgrade"),
                total.applyAsInt("ic2:transformer_upgrade"),
                total.applyAsInt("ic2:energy_storage_upgrade"));
    }

    /** Whether a basic single-item electric processor accepts this upgrade type. */
    public static boolean accepts(String id) {
        return switch (id) {
            case "ic2:overclocker_upgrade",
                            "ic2:transformer_upgrade",
                            "ic2:energy_storage_upgrade",
                            "ic2:ejector_upgrade",
                            "ic2:advanced_ejector_upgrade",
                            "ic2:pulling_upgrade",
                            "ic2:advanced_pulling_upgrade" ->
                    true;
            default -> false;
        };
    }

    /** Uses the specified machine's original duration, drain and buffer reserve. */
    public Stats stats(int baseDuration, int baseEnergy, int reserve, int tier) {
        double precise = baseDuration * Math.pow(0.7, overclockers);
        int operations = Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.ceil(1 / precise)));
        int duration = Math.max(1, (int) Math.round(precise * operations));
        int energy =
                (int)
                        Math.min(
                                Integer.MAX_VALUE,
                                Math.round(baseEnergy * Math.pow(1.6, overclockers)));
        int capacity =
                (int)
                        Math.min(
                                Integer.MAX_VALUE,
                                reserve + 10_000L * storage + (long) duration * energy);
        return new Stats(duration, energy, capacity, operations, tier + transformers);
    }

    /** Convert paid progress from an old operating length to a new operating length. */
    public static int progress(int oldProgress, int oldDuration, int newDuration) {
        if (oldProgress <= 0 || oldDuration <= 0) return 0;
        return Math.min(
                newDuration - 1,
                (int) Math.floor((double) oldProgress / oldDuration * newDuration + 0.1));
    }

    /** Maximum number of items one installed route may transfer on each eligible face. */
    public static int transferLimit(int count) {
        return count <= 0 ? 0 : 1 << (2 * Math.min(4, count - 1));
    }

    /** Current operation duration, EU/t, EU buffer, maximum batch size and packet tier. */
    public record Stats(int duration, int energyPerTick, int capacity, int operations, int tier) {}

    private static int clamp(int count, int max) {
        return Math.max(0, Math.min(max, count));
    }
}
