package com.industrialage.ia2.gui;

import net.minecraft.network.chat.Component;

/** Draws at native GUI pixel coordinates from the artist's 256-square IC2 sheets. */
public interface PanelCanvas {
    /** Copy atlas pixels at (u,v) to screen-local (x,y), with no scaling or colour substitution. */
    default void blit(String gui, int x, int y, int u, int v, int width, int height) {
        blit(gui, x, y, u, v, width, height, 256, 256);
    }

    /** Original-size overlays retain their own texture dimensions. */
    void blit(
            String gui,
            int x,
            int y,
            int u,
            int v,
            int width,
            int height,
            int textureWidth,
            int textureHeight);

    /** Draws the registered fluid's still sprite using its native texture atlas and tint. */
    void fluid(int registryId, int x, int y, int width, int height);

    /** Overlay a flat colour for a selection, mask or gauge when the design calls for it. */
    void fill(int x, int y, int right, int bottom, int argb);

    /** Render a localized label in the given alignment. */
    void text(Component value, int x, int y, int argb, boolean centered);

    /** Copy a named, already-painted full panel from the top-left region of its atlas. */
    default void background(String gui, int width, int height) {
        blit(gui, 0, 0, 0, 0, width, height);
    }

    /** Frame for externally defined IC2 screens. Corners stay full size and middle pixels tile. */
    default void frame(int x, int y, int width, int height) {
        final int sheet = 16, edge = 8, inner = 64 - 2 * edge;
        if (width < 2 * edge || height < 2 * edge)
            throw new IllegalArgumentException("Panel too small");
        for (int row = 0; row < height; ) {
            int v = row == 0 ? sheet : row == height - edge ? sheet + 64 - edge : sheet + edge;
            int h = row == 0 || row == height - edge ? edge : Math.min(inner, height - edge - row);
            for (int col = 0; col < width; ) {
                int u = col == 0 ? sheet : col == width - edge ? sheet + 64 - edge : sheet + edge;
                int w =
                        col == 0 || col == width - edge
                                ? edge
                                : Math.min(inner, width - edge - col);
                blit("common", x + col, y + row, u, v, w, h);
                col += w;
            }
            row += h;
        }
    }

    /** The standard 18px well surrounding a 16px item. */
    default void slot(int x, int y) {
        blit("common", x - 1, y - 1, 103, 7, 18, 18);
    }

    /** The standard native player inventory grid at the top-left item position. */
    default void inventory(int x, int y, int hotbarY) {
        for (int col = 0; col < 9; col++) {
            for (int row = 0; row < 3; row++) slot(x + col * 18, y + row * 18);
            slot(x + col * 18, hotbarY);
        }
    }
}
