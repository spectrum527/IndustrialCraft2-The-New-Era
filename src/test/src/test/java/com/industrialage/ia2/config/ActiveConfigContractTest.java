package com.industrialage.ia2.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ActiveConfigContractTest {
    private static final Map<String, ActiveConfig.Entry<?>> ENTRIES =
            ActiveConfig.entries().stream()
                    .collect(Collectors.toMap(entry -> entry.key().name(), Function.identity()));

    @Test
    void allDeclaredPathsAndTypedDefaultsMatchTheApprovedTable() throws Exception {
        var rows = Files.readAllLines(Path.of(System.getProperty("ic2.activeKeysTable")));
        assertEquals(59, rows.size());
        assertEquals(58, ENTRIES.size());
        for (String row : rows.subList(1, rows.size())) {
            var fields = row.split("\t");
            var name = fields[0];
            var entry = ENTRIES.get(name);
            assertNotNull(entry, name);
            assertEquals(name, String.join("/", entry.path()));
            assertEquals("SERVER", fields[3]);
            var value = entry.defaultValue();
            if ("List<String>".equals(fields[1])) {
                assertSame(entry.key(), ActiveConfig.stringListKey(name));
                List<?> strings = assertInstanceOf(List.class, value);
                assertEquals(
                        fields[2],
                        strings.stream()
                                .map(s -> "\"" + s + "\"")
                                .collect(Collectors.joining(",", "[", "]")));
            } else {
                Object literal =
                        switch (fields[1]) {
                            case "Bool" -> Boolean.valueOf(fields[2]);
                            case "Int32" -> Integer.valueOf(fields[2]);
                            case "Float32" -> Float.valueOf(fields[2]);
                            case "Float64" -> Double.valueOf(fields[2]);
                            default ->
                                    throw new AssertionError("Unexpected table type " + fields[1]);
                        };
                assertSame(entry.key(), ActiveConfig.scalarKey(name, literal.getClass()));
                assertEquals(literal, value, name);
            }
            assertEquals(value, entry.resolve(value), name);
        }
    }

    @Test
    void validatesExactTypesNumericEndpointsAndRecipeInputLists() {
        var ratio = ENTRIES.get("balance/energyRetainedInStorageBlockDrops");
        assertEquals(0.0, ratio.resolve(0));
        assertEquals(1.0, ratio.resolve(1));
        for (Object bad : List.of(-0.01, 1.5, Double.POSITIVE_INFINITY, Double.NaN, "0.5")) {
            assertThrows(IllegalArgumentException.class, () -> ratio.resolve(bad));
        }
        var heat = ENTRIES.get("balance/fermenter/hU_per_run");
        assertEquals(Integer.MIN_VALUE, heat.resolve((long) Integer.MIN_VALUE));
        assertEquals(Integer.MAX_VALUE, heat.resolve((long) Integer.MAX_VALUE));
        for (Object bad : List.of(0.5, "45", (long) Integer.MAX_VALUE + 1)) {
            assertThrows(IllegalArgumentException.class, () -> heat.resolve(bad));
        }
        var allowed = ENTRIES.get("balance/recyclerWhitelist");
        var example = List.of("ic2:test", "Tag:c:items", "Fluid:minecraft:water");
        assertEquals(example, allowed.resolve(example));
        assertEquals(List.of(), allowed.resolve(List.of()));
        for (var bad : List.of(List.of(8), List.of("Other:ic2:test"), List.of("ic2:test*3"))) {
            assertThrows(IllegalArgumentException.class, () -> allowed.resolve(bad));
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> ENTRIES.get("protection/wrenchLogging").resolve("true"));
        assertThrows(
                IllegalArgumentException.class,
                () -> ActiveConfig.scalarKey("worldgen/copper", Boolean.class));
        assertThrows(
                IllegalArgumentException.class,
                () -> ActiveConfig.scalarKey("balance/recyclerWhitelist", String.class));
    }
}
