package com.industrialage.ia2.gui;

/** Fits authored coordinates into the logical Minecraft viewport. */
record PanelViewport(int left, int top, float scale) {
    private static final int MARGIN = 2;

    static PanelViewport fit(int width, int height, int panelWidth, int panelHeight) {
        float fitWidth = (float) Math.max(1, width - 2 * MARGIN) / panelWidth;
        float fitHeight = (float) Math.max(1, height - 2 * MARGIN) / panelHeight;
        float scale = Math.min(1, Math.min(fitWidth, fitHeight));
        return new PanelViewport(
                (int) ((width - panelWidth * scale) / 2),
                (int) ((height - panelHeight * scale) / 2),
                scale);
    }

    double unscaleX(double x) {
        return left + (x - left) / scale;
    }

    double unscaleY(double y) {
        return top + (y - top) / scale;
    }

    float translateX() {
        return left * (1 - scale);
    }

    float translateY() {
        return top * (1 - scale);
    }

    int screenX(int x) {
        return (int) Math.round(left + (x - left) * (double) scale);
    }

    int screenY(int y) {
        return (int) Math.round(top + (y - top) * (double) scale);
    }

    int drawSize(int size) {
        return (int) Math.ceil(size * scale);
    }
}
