package com.industrialage.ia2.content;

import java.util.List;

/** Energy cost and geometry of the mining laser's eight in-game settings. */
public final class MiningLaserModes {
    /** Size of the beam array, hits per beam and duration in game ticks. */
    public record Mode(
            int cost,
            int size,
            int hits,
            int lifetime,
            boolean levelOnly,
            boolean smelt,
            boolean blast,
            boolean parallel) {}

    private static final List<Mode> MODES =
            List.of(
                    new Mode(1_250, 1, 8, 20, false, false, false, false),
                    new Mode(100, 1, 1, 4, false, false, false, false),
                    new Mode(5_000, 1, 24, 40, false, false, false, false),
                    new Mode(3_000, 1, 14, 25, true, false, false, false),
                    new Mode(2_500, 1, 8, 18, false, true, false, false),
                    new Mode(10_000, 5, 3, 14, false, false, false, false),
                    new Mode(5_000, 1, 1, 15, false, false, true, false),
                    new Mode(3_000, 3, 4, 16, false, false, false, true));

    private MiningLaserModes() {}

    /** Resolves any persisted index to a valid setting. */
    public static Mode get(int index) {
        return MODES.get(Math.floorMod(index, MODES.size()));
    }

    /** Cycles the eight supported settings. */
    public static int next(int index) {
        return Math.floorMod(index + 1, MODES.size());
    }

    /** Sound of a successful shot in this mode. */
    public static String sound(int index) {
        return switch (Math.floorMod(index, MODES.size())) {
            case 1 -> "item.laser.low_focus";
            case 2 -> "item.laser.long_range";
            case 5, 7 -> "item.laser.scatter";
            case 6 -> "item.laser.explosive";
            default -> "item.laser.shoot";
        };
    }
}
