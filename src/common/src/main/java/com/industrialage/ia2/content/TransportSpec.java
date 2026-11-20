package com.industrialage.ia2.content;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registry paths, capacities and per-tick transport limits from the repaired game. */
public record TransportSpec(String path, Kind kind, int capacity, int rate, int size) {
    /** The unit of both limits is items for item pipes, and millibuckets for fluids. */
    public enum Kind {
        ITEM_PIPE,
        FLUID_PIPE,
        TANK
    }

    /** All placeable transport devices, in stable registration order. */
    public static final Map<String, TransportSpec> DEVICES = devices();

    private static Map<String, TransportSpec> devices() {
        Map<String, TransportSpec> result = new LinkedHashMap<>();
        String[] sizes = {"tiny", "small", "medium", "large"};
        int[] fluidBase = {400, 800, 2400, 4800};
        for (String metal : new String[] {"bronze", "steel"}) {
            for (int size = 0; size < sizes.length; size++) {
                int items = 1 << (2 * size);
                int fluid = fluidBase[size] * (metal.equals("steel") ? 2 : 1);
                String itemPipe = metal + "_item_pipe_" + sizes[size];
                String fluidPipe = metal + "_fluid_pipe_" + sizes[size];
                result.put(
                        itemPipe, new TransportSpec(itemPipe, Kind.ITEM_PIPE, items, items, size));
                result.put(
                        fluidPipe,
                        new TransportSpec(fluidPipe, Kind.FLUID_PIPE, fluid, fluid / 20, size));
            }
        }
        for (var entry :
                Map.of("bronze", 32_000, "iron", 32_000, "steel", 128_000, "iridium", 1_024_000)
                        .entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        .toList()) {
            String id = entry.getKey() + "_tank";
            result.put(id, new TransportSpec(id, Kind.TANK, entry.getValue(), entry.getValue(), 0));
        }
        return Collections.unmodifiableMap(result);
    }
}
