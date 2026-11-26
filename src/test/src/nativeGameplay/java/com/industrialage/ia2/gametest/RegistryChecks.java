package com.industrialage.ia2.gametest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Compare each IC2 runtime registry to the complete independent repaired-game inventory. */
final class RegistryChecks {
    private static final java.util.Set<String> INTENTIONALLY_REMOVED_ITEMS =
            java.util.Set.of("ic2:copper_ingot");

    private RegistryChecks() {}

    static void run(BiConsumer<String, Boolean> check) {
        var result = new JsonObject();
        try (var reader =
                new InputStreamReader(
                        Objects.requireNonNull(
                                RegistryChecks.class.getResourceAsStream(
                                        "/reference-registries.json")),
                        StandardCharsets.UTF_8)) {
            var expected =
                    JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("registries");
            for (String key : expected.keySet()) {
                Registry<?> registry =
                        switch (key) {
                            case "minecraft:item" -> BuiltInRegistries.ITEM;
                            case "minecraft:block" -> BuiltInRegistries.BLOCK;
                            case "minecraft:fluid" -> BuiltInRegistries.FLUID;
                            case "minecraft:entity_type" -> BuiltInRegistries.ENTITY_TYPE;
                            case "minecraft:mob_effect" -> BuiltInRegistries.MOB_EFFECT;
                            case "minecraft:sound_event" -> BuiltInRegistries.SOUND_EVENT;
                            default ->
                                    throw new IllegalArgumentException(
                                            "Unexpected reference registry " + key);
                        };
                var actual = new TreeSet<String>();
                registry.keySet().stream()
                        .filter(k -> k.getNamespace().equals("ic2"))
                        .forEach(k -> actual.add(k.toString()));
                var needed = new TreeSet<String>();
                expected.getAsJsonArray(key).forEach(e -> needed.add(e.getAsString()));
                if (key.equals("minecraft:item")) {
                    needed.removeAll(INTENTIONALLY_REMOVED_ITEMS);
                    check.accept("removed-copper-ingot", !actual.contains("ic2:copper_ingot"));
                    check.accept("registered-biogas-cell", actual.contains("ic2:biogas_cell"));
                }
                var missing = new TreeSet<>(needed);
                missing.removeAll(actual);
                var extra = new TreeSet<>(actual);
                extra.removeAll(needed);
                var row = new JsonObject();
                row.addProperty("reference", needed.size());
                row.addProperty("registered", actual.size());
                var gson = new GsonBuilder().setPrettyPrinting().create();
                row.add("missing", gson.toJsonTree(missing));
                row.add("additional", gson.toJsonTree(extra));
                row.add("ids", gson.toJsonTree(actual));
                result.add(key, row);
            }
            Files.writeString(
                    Path.of("runtime-registries.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(result) + "\n");
            result.entrySet()
                    .forEach(
                            e ->
                                    check.accept(
                                            "registered-" + e.getKey().substring(10),
                                            e.getValue()
                                                    .getAsJsonObject()
                                                    .getAsJsonArray("missing")
                                                    .isEmpty()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
