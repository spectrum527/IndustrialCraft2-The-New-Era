package com.industrialage.ia2.industrial;

import java.util.Locale;

/** Slot counts of portable boxes and the owner-locked personal safe. */
enum BoxKind {
    WOODEN_STORAGE_BOX(27),
    IRON_STORAGE_BOX(45),
    BRONZE_STORAGE_BOX(45),
    STEEL_STORAGE_BOX(63),
    IRIDIUM_STORAGE_BOX(126),
    PERSONAL_CHEST(54),
    INDUSTRIAL_WORKBENCH(0);

    final int slots;
    final String path;

    BoxKind(int slots) {
        this.slots = slots;
        path = name().toLowerCase(Locale.ROOT);
    }

    boolean portable() {
        return slots > 0 && this != PERSONAL_CHEST;
    }
}
