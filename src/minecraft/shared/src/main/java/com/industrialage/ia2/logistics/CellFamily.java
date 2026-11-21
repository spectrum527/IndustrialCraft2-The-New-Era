package com.industrialage.ia2.logistics;

import java.util.Map;

/** Registry identities and allowed liquids of each reusable cell family. */
public enum CellFamily {
    BASIC(
            "empty_cell",
            Map.of("basic_water_cell", "minecraft:water", "basic_lava_cell", "minecraft:lava")),
    UNIVERSAL(
            "fluid_cell",
            Map.of(
                    "water_cell", "minecraft:water",
                    "lava_cell", "minecraft:lava",
                    "air_cell", "ic2:air",
                    "weed_ex_cell", "ic2:weed_ex"));

    /** Volume of one filled cell, in millibuckets. */
    public static final int CAPACITY = 1000;

    /** The registered fluid identifier and measured contents of one cell, in millibuckets. */
    public record Contents(String id, int amount) {}

    private final String emptyPath;
    private final Map<String, String> namedLiquids;

    CellFamily(String emptyPath, Map<String, String> namedLiquids) {
        this.emptyPath = emptyPath;
        this.namedLiquids = namedLiquids;
    }

    /** Item path for the empty shell and the shell holding data-backed contents. */
    public String emptyPath() {
        return emptyPath;
    }

    /** Item paths and fluid identifiers encoded by the named full cells. */
    public Map<String, String> namedLiquids() {
        return namedLiquids;
    }

    static CellFamily of(String path) {
        for (CellFamily family : values()) {
            if (family.emptyPath.equals(path) || family.namedLiquids.containsKey(path))
                return family;
        }
        return null;
    }

    static String itemPath(String id) {
        return id.startsWith("ic2:") ? id.substring(4) : "";
    }

    boolean admits(String liquid) {
        return this == UNIVERSAL || namedLiquids.containsValue(liquid);
    }

    String resultingPath(String liquid, int amount) {
        if (amount == CAPACITY) {
            for (var entry : namedLiquids.entrySet()) {
                if (entry.getValue().equals(liquid)) return entry.getKey();
            }
        }
        return emptyPath;
    }
}
