package com.industrialage.ia2.gameplay;

import java.util.Arrays;
import java.util.List;

/** Native menu layouts of the machines with block entities. */
enum MachineKind {
    GENERATOR("generator", SlotRole.FUEL, SlotRole.CHARGE),
    BATBOX("batbox", SlotRole.CHARGE, SlotRole.DISCHARGE),
    CESU("cesu", SlotRole.CHARGE, SlotRole.DISCHARGE),
    MFE("mfe", SlotRole.CHARGE, SlotRole.DISCHARGE),
    MFSU("mfsu", SlotRole.CHARGE, SlotRole.DISCHARGE),
    BATBOX_CHARGEPAD("batbox_chargepad", SlotRole.CHARGE, SlotRole.DISCHARGE),
    CESU_CHARGEPAD("cesu_chargepad", SlotRole.CHARGE, SlotRole.DISCHARGE),
    MFE_CHARGEPAD("mfe_chargepad", SlotRole.CHARGE, SlotRole.DISCHARGE),
    MFSU_CHARGEPAD("mfsu_chargepad", SlotRole.CHARGE, SlotRole.DISCHARGE),
    IRON_FURNACE("iron_furnace", SlotRole.INPUT, SlotRole.FUEL, SlotRole.OUTPUT),
    MACERATOR(
            "macerator",
            SlotRole.INPUT,
            SlotRole.OUTPUT,
            SlotRole.DISCHARGE,
            SlotRole.UPGRADE,
            SlotRole.UPGRADE,
            SlotRole.UPGRADE,
            SlotRole.UPGRADE),
    ELECTRIC_FURNACE(
            "electric_furnace",
            SlotRole.INPUT,
            SlotRole.OUTPUT,
            SlotRole.DISCHARGE,
            SlotRole.UPGRADE,
            SlotRole.UPGRADE,
            SlotRole.UPGRADE,
            SlotRole.UPGRADE),
    COMPRESSOR(
            "compressor",
            SlotRole.INPUT,
            SlotRole.OUTPUT,
            SlotRole.DISCHARGE,
            SlotRole.UPGRADE,
            SlotRole.UPGRADE,
            SlotRole.UPGRADE,
            SlotRole.UPGRADE),
    EXTRACTOR(
            "extractor",
            SlotRole.INPUT,
            SlotRole.OUTPUT,
            SlotRole.DISCHARGE,
            SlotRole.UPGRADE,
            SlotRole.UPGRADE,
            SlotRole.UPGRADE,
            SlotRole.UPGRADE);

    final String path;
    final List<SlotRole> slots;

    MachineKind(String path, SlotRole... slots) {
        this.path = path;
        this.slots = List.of(slots);
    }

    static MachineKind fromPath(String path) {
        return Arrays.stream(values())
                .filter(kind -> kind.path.equals(path))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown IC2 machine: " + path));
    }

    boolean electricProcessor() {
        return this != GENERATOR && !storage() && this != IRON_FURNACE;
    }

    boolean storage() {
        return switch (this) {
            case BATBOX,
                    CESU,
                    MFE,
                    MFSU,
                    BATBOX_CHARGEPAD,
                    CESU_CHARGEPAD,
                    MFE_CHARGEPAD,
                    MFSU_CHARGEPAD ->
                    true;
            default -> false;
        };
    }

    boolean chargingPad() {
        return path.endsWith("_chargepad");
    }

    String storagePath() {
        return chargingPad() ? path.substring(0, path.length() - "_chargepad".length()) : path;
    }

    enum SlotRole {
        INPUT("input"),
        OUTPUT("output"),
        FUEL("fuel"),
        CHARGE("charge"),
        DISCHARGE("discharge"),
        UPGRADE("upgrade");

        final String label;

        SlotRole(String label) {
            this.label = label;
        }
    }
}
