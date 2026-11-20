package com.industrialage.ia2.energy.graph;

/** Nominal packet sizes and energy allowances per source and settlement round. */
public final class PacketBudget {
    private PacketBudget() {}

    /**
     * Returns the nominal packet size in EU for a tier.
     *
     * @throws IllegalArgumentException if the tier is negative
     */
    public static double packetSize(int tier) {
        if (tier < 0) {
            throw new IllegalArgumentException("Negative energy tier: " + tier);
        }
        return Math.scalb(8.0, 2 * Math.min(tier, 30));
    }

    /** Returns a source's round budget in EU, with a diagnostic flag for a negative tier. */
    public static SourceBudget sourceBudget(int tier, double offeredEu, int packets) {
        if (tier < 0) {
            return new SourceBudget(0, true);
        }
        if (!(offeredEu > 0) || packets <= 0) {
            return new SourceBudget(0, false);
        }
        return new SourceBudget(Math.min(offeredEu, packets * packetSize(tier)), false);
    }

    /**
     * A round budget and its tier diagnostic.
     *
     * @param eu energy available in EU
     * @param invalidTier whether the input tier was negative
     */
    public record SourceBudget(double eu, boolean invalidTier) {}
}
