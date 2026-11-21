package com.industrialage.ia2.equipment;

/** Pixel-only lightmap adjustment; this never changes a player's effects or settings. */
public final class QuantumLightmapColor {
    private QuantumLightmapColor() {}

    /** Raises the brightest color channel while preserving the lightmap's relative colors. */
    public static int brighten(int pixel) {
        int red = pixel & 255;
        int green = pixel >>> 8 & 255;
        int blue = pixel >>> 16 & 255;
        int maximum = Math.max(red, Math.max(green, blue));
        if (maximum == 0) return pixel;
        return pixel & 0xff000000
                | (red * 255 / maximum)
                | (green * 255 / maximum) << 8
                | (blue * 255 / maximum) << 16;
    }
}
