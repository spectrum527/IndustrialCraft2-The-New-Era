package com.industrialage.ia2.industrial;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Measured atlas area, player hotbar and physical item origins for one native menu type. */
record PowerLayout(
        String texture,
        String style,
        int width,
        int height,
        int[] inventory,
        Map<Integer, int[]> slots) {
    private static final Map<String, PowerLayout> PANELS;

    static {
        try (var stream =
                        Objects.requireNonNull(
                                PowerLayout.class.getResourceAsStream(
                                        "/assets/ic2/gui/power-layouts.json"));
                var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var rows = new HashMap<String, PowerLayout>();
            for (var row : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
                var obj = row.getValue().getAsJsonObject();
                var points = new HashMap<Integer, int[]>();
                for (var point : obj.getAsJsonObject("slots").entrySet())
                    points.put(
                            Integer.parseInt(point.getKey()),
                            integers(point.getValue().getAsJsonArray()));
                rows.put(
                        row.getKey(),
                        new PowerLayout(
                                obj.get("texture").getAsString(),
                                obj.get("style").getAsString(),
                                obj.get("width").getAsInt(),
                                obj.get("height").getAsInt(),
                                integers(obj.getAsJsonArray("inventory")),
                                Map.copyOf(points)));
            }
            PANELS = Map.copyOf(rows);
        } catch (java.io.IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static int[] integers(JsonArray row) {
        var result = new int[row.size()];
        for (int i = 0; i < result.length; i++) result[i] = row.get(i).getAsInt();
        return result;
    }

    static PowerLayout get(String name) {
        return Objects.requireNonNull(PANELS.get(name), name);
    }

    boolean assembled() {
        return style.equals("assembled");
    }

    int[] origin(int slot) {
        return slots.getOrDefault(slot, HIDDEN);
    }

    private static final int[] HIDDEN = {-1000, -1000};
}
