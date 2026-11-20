package com.industrialage.ia2.content;

/** Inventory anchors within the authored IC2 panels. */
public enum MachineLayout {
    MACERATOR(166, 83),
    COMPRESSOR(166, 83),
    EXTRACTOR(166, 83),
    ELECTRIC_FURNACE(166, 83),
    IRON_FURNACE(166, 83),
    GENERATOR(166, 83),
    STORAGE(196, 113),
    CHARGE_PAD(161, 78);

    /** Panel width in unscaled GUI pixels. */
    public static final int WIDTH = 176;

    private final int height;
    private final int inventoryTop;

    MachineLayout(int height, int inventoryTop) {
        this.height = height;
        this.inventoryTop = inventoryTop;
    }

    /** Selects the authored artwork and anchors for a registered machine. */
    public static MachineLayout byPath(String path) {
        return switch (path) {
            case "macerator" -> MACERATOR;
            case "compressor" -> COMPRESSOR;
            case "extractor" -> EXTRACTOR;
            case "electric_furnace" -> ELECTRIC_FURNACE;
            case "iron_furnace" -> IRON_FURNACE;
            case "generator" -> GENERATOR;
            case "batbox", "cesu", "mfe", "mfsu", "classic_mfe", "classic_mfsu" -> STORAGE;
            case "batbox_chargepad", "cesu_chargepad", "mfe_chargepad", "mfsu_chargepad" ->
                    CHARGE_PAD;
            default -> throw new IllegalArgumentException("Machine has no panel: " + path);
        };
    }

    /** Height of the visible panel region, excluding sprites in the atlas margins. */
    public int height() {
        return height;
    }

    /** Top edge of the first row of the authored player inventory. */
    public int inventoryTop() {
        return inventoryTop;
    }

    /** Top edge of the independent hotbar row. */
    public int hotbarTop() {
        return inventoryTop + 58;
    }

    /** Whether this menu uses an authored, complete storage panel. */
    public boolean storage() {
        return this == STORAGE || this == CHARGE_PAD;
    }

    /** Point at which the native item icon is centered within its authored slot. */
    public Point item(Kind role, int index) {
        var frame = frame(role, index);
        int inset = role == Kind.OUTPUT ? 5 : 1;
        return new Point(frame.x + inset, frame.y + inset);
    }

    /** Top-left edge of a slot's authored art. */
    public Point frame(Kind role, int index) {
        return switch (role) {
            case INPUT -> new Point(55, 16);
            case FUEL -> new Point(this == GENERATOR ? 56 : 55, 52);
            case CHARGE -> new Point(storage() ? 55 : 56, 16);
            case DISCHARGE -> new Point(55, 52);
            case OUTPUT -> new Point(111, 30);
            case UPGRADE -> new Point(151, 7 + 18 * index);
        };
    }

    /** Native slot purposes used by both platform adapters. */
    public enum Kind {
        INPUT,
        OUTPUT,
        FUEL,
        CHARGE,
        DISCHARGE,
        UPGRADE
    }

    /** A GUI position in unscaled pixels. */
    public record Point(int x, int y) {}
}
