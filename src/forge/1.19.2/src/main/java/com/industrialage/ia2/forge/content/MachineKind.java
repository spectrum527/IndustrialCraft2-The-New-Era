package com.industrialage.ia2.forge.content;

import static com.industrialage.ia2.forge.content.MachineKind.Slot.*;

import com.industrialage.ia2.content.PowerGrid;
import java.util.List;

enum MachineKind {
    GENERATOR("generator", FUEL, CHARGE),
    BATBOX("batbox", CHARGE, DISCHARGE),
    CESU("cesu", CHARGE, DISCHARGE),
    MFE("mfe", CHARGE, DISCHARGE),
    MFSU("mfsu", CHARGE, DISCHARGE),
    BATBOX_CHARGEPAD("batbox_chargepad", CHARGE, DISCHARGE),
    CESU_CHARGEPAD("cesu_chargepad", CHARGE, DISCHARGE),
    MFE_CHARGEPAD("mfe_chargepad", CHARGE, DISCHARGE),
    MFSU_CHARGEPAD("mfsu_chargepad", CHARGE, DISCHARGE),
    MACERATOR("macerator", INPUT, DISCHARGE, OUTPUT, UPGRADE, UPGRADE, UPGRADE, UPGRADE),
    EXTRACTOR("extractor", INPUT, DISCHARGE, OUTPUT, UPGRADE, UPGRADE, UPGRADE, UPGRADE),
    COMPRESSOR("compressor", INPUT, DISCHARGE, OUTPUT, UPGRADE, UPGRADE, UPGRADE, UPGRADE),
    ELECTRIC_FURNACE(
            "electric_furnace", INPUT, DISCHARGE, OUTPUT, UPGRADE, UPGRADE, UPGRADE, UPGRADE),
    IRON_FURNACE("iron_furnace", INPUT, FUEL, OUTPUT);

    final String path;
    final List<Slot> slots;

    MachineKind(String path, Slot... slots) {
        this.path = path;
        this.slots = List.of(slots);
    }

    boolean processes() {
        return this != GENERATOR && !storesEnergy();
    }

    boolean storesEnergy() {
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

    int itemTier() {
        return storesEnergy() ? PowerGrid.storage(storagePath()).tier() : 1;
    }

    enum Slot {
        INPUT,
        FUEL,
        CHARGE,
        DISCHARGE,
        OUTPUT,
        UPGRADE
    }
}
