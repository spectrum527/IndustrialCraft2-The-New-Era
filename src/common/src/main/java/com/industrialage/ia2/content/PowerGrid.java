package com.industrialage.ia2.content;

import com.industrialage.ia2.energy.graph.PacketBudget;
import java.util.function.DoubleUnaryOperator;

/** Per-packet accounting shared by the loaded-world electricity networks. */
public final class PowerGrid {
    private PowerGrid() {}

    /** Returns the rated storage block, or throws if the registry path is not storage. */
    public static Storage storage(String path) {
        return switch (path) {
            case "batbox" -> new Storage(40_000, 1);
            case "cesu" -> new Storage(300_000, 2);
            case "mfe" -> new Storage(4_000_000, 3);
            case "mfsu" -> new Storage(40_000_000, 4);
            default -> throw new IllegalArgumentException("Unknown storage: " + path);
        };
    }

    /** Capacity in EU and nominal packet tier of a directional storage block. */
    public record Storage(double capacity, int tier) {
        /** Returns the single-round budget if the storage can form a complete packet. */
        public double offer(double stored) {
            double packet = PacketBudget.packetSize(tier);
            return stored >= packet ? packet : 0;
        }
    }

    /** Per-segment unrounded line loss for an IC2 cable registry path. */
    public static double cableLoss(String path) {
        return switch (path) {
            case "tin_cable", "insulated_tin_cable", "copper_cable", "insulated_copper_cable" ->
                    0.2;
            case "gold_cable", "insulated_gold_cable", "double_insulated_gold_cable" -> 0.4;
            case "iron_cable",
                            "insulated_iron_cable",
                            "double_insulated_iron_cable",
                            "triple_insulated_iron_cable" ->
                    0.8;
            case "glass_fibre_cable" -> 0.025;
            case "detector_cable", "splitter_cable" -> 0.5;
            default -> throw new IllegalArgumentException("Unknown conductor: " + path);
        };
    }

    /** Returns the nominal packet rating of a cable by registry path. */
    public static double cableRating(String path) {
        return switch (path) {
            case "tin_cable", "insulated_tin_cable" -> 32;
            case "copper_cable", "insulated_copper_cable" -> 128;
            case "gold_cable", "insulated_gold_cable", "double_insulated_gold_cable" -> 512;
            case "iron_cable",
                            "insulated_iron_cable",
                            "double_insulated_iron_cable",
                            "triple_insulated_iron_cable" ->
                    2048;
            case "glass_fibre_cable", "detector_cable", "splitter_cable" -> 8192;
            default -> throw new IllegalArgumentException("Unknown conductor: " + path);
        };
    }

    /**
     * Offers the budget after line loss to the sink. Returns the debit for the source. No energy is
     * charged for an unreachable or entirely rejecting sink.
     */
    public static double transfer(
            double budget, double rawLoss, boolean roundLoss, DoubleUnaryOperator receive) {
        if (!Double.isFinite(budget) || !Double.isFinite(rawLoss) || rawLoss < 0) {
            throw new IllegalArgumentException("Invalid energy settlement");
        }
        double loss = roundLoss ? Math.floor(rawLoss) : rawLoss;
        if (budget <= loss) {
            return 0;
        }
        double offer = budget - loss;
        double accepted = receive.applyAsDouble(offer);
        if (!(accepted >= 0 && accepted <= offer)) {
            throw new IllegalArgumentException("Sink accepted an amount outside its offer");
        }
        return accepted == 0 ? 0 : Math.min(budget, accepted + loss);
    }
}
