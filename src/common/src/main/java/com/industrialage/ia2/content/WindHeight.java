package com.industrialage.ia2.content;

/**
 * Height response of identical, unobstructed wind generators observed at every overworld block Y.
 */
public final class WindHeight {
    private static final double[] RESPONSE =
            IndustrialMaterials.rows("wind-height.tsv").stream()
                    .mapToDouble(row -> Double.parseDouble(row.substring(row.indexOf('\t') + 1)))
                    .toArray();

    private WindHeight() {}

    /** Fraction of unobstructed air in the observed nine by seven by nine wind window. */
    public static double clearance(int blocking) {
        return Math.max(0, 1.0 - Math.max(0, blocking) / 567.0);
    }

    /** Relative output at the target altitude, compared with the maximum around Y=223. */
    public static double response(int y) {
        if (y < -64 || y > 319) return 0;
        return RESPONSE[y + 64];
    }
}
