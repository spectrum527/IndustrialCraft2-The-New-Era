package com.industrialage.ia2.world;

import net.minecraft.util.RandomSource;

/** Growth, gain, resistance and the scan level written to a crop and its seed bag. */
record CropGenes(int growth, int gain, int resistance, int scan) {
    static final CropGenes WILD = new CropGenes(1, 1, 1, 0);

    CropGenes {
        growth = clamp(growth, 31);
        gain = clamp(gain, 31);
        resistance = clamp(resistance, 31);
        scan = clamp(scan, 4);
    }

    CropGenes cross(CropGenes other, RandomSource random) {
        return new CropGenes(
                mix(growth, other.growth, random),
                mix(gain, other.gain, random),
                mix(resistance, other.resistance, random),
                0);
    }

    CropGenes scanMore() {
        return new CropGenes(growth, gain, resistance, scan + 1);
    }

    private static int mix(int from, int to, RandomSource random) {
        return (from + to + 1) / 2 + random.nextInt(3) - 1;
    }

    private static int clamp(int value, int upper) {
        return Math.max(0, Math.min(upper, value));
    }
}
