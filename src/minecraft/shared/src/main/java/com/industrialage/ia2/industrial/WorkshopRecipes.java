package com.industrialage.ia2.industrial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class WorkshopRecipes {
    private static final Pattern NUMBER = Pattern.compile("\"([A-Za-z]+)\":(\\d+)");
    private static final String ROOT = "/com/industrialage/ia2/content/";
    private static final Set<String> MACHINES =
            Set.of(
                    "metal_former_extruding",
                    "metal_former_rolling",
                    "metal_former_cutting",
                    "ore_washer",
                    "centrifuge",
                    "blast_furnace",
                    "block_cutter",
                    "canner_bottle",
                    "canner_enrich");
    private static final Map<String, List<Recipe>> RECIPES = recipes();
    private static final Map<String, Set<String>> TAG_MEMBERS = tags();

    private WorkshopRecipes() {}

    record Stack(String id, int count) {
        Stack {
            if (id.isBlank() || count <= 0) {
                throw new IllegalArgumentException("Invalid workshop ingredient " + id);
            }
        }
    }

    record Recipe(
            String id, List<Stack> inputs, List<Stack> outputs, Map<String, Integer> requirements) {
        int value(String key) {
            return requirements.getOrDefault(key, 0);
        }
    }

    static List<Recipe> forType(String name) {
        return RECIPES.getOrDefault(name, List.of());
    }

    static boolean knownMember(String tag, String item) {
        return com.industrialage.ia2.content.ProcessingMachine.matchesTagAlias(
                tag, candidate -> TAG_MEMBERS.getOrDefault(candidate, Set.of()).contains(item));
    }

    private static Map<String, List<Recipe>> recipes() {
        var result = new HashMap<String, List<Recipe>>();
        try (var rows = read("workshop-recipes.tsv")) {
            rows.readLine();
            for (String line; (line = rows.readLine()) != null; ) {
                var fields = line.split("\t", -1);
                if (!MACHINES.contains(fields[0])) {
                    continue;
                }
                var parameters = new HashMap<String, Integer>();
                var numbers = NUMBER.matcher(fields[4]);
                while (numbers.find()) {
                    parameters.put(numbers.group(1), Integer.parseInt(numbers.group(2)));
                }
                var recipe =
                        new Recipe(
                                fields[1],
                                stacks(fields[2]),
                                stacks(fields[3]),
                                Map.copyOf(parameters));
                result.computeIfAbsent(fields[0], ignored -> new ArrayList<>()).add(recipe);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot load workshop recipes", failure);
        }
        result.replaceAll((key, recipes) -> List.copyOf(recipes));
        return Map.copyOf(result);
    }

    private static Map<String, Set<String>> tags() {
        var result = new HashMap<String, Set<String>>();
        try (var rows = read("processing-tags.tsv")) {
            rows.readLine();
            for (String line; (line = rows.readLine()) != null; ) {
                var fields = line.split("\t", -1);
                if (fields.length < 2 || fields[0].isBlank()) {
                    continue;
                }
                var members =
                        fields[1].isEmpty()
                                ? Set.<String>of()
                                : Set.copyOf(Arrays.asList(fields[1].split(",")));
                result.put(fields[0], members);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot load workshop tag memberships", failure);
        }
        return Map.copyOf(result);
    }

    private static List<Stack> stacks(String line) {
        if (line.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(line.split(","))
                .map(
                        value -> {
                            int split = value.lastIndexOf('@');
                            return new Stack(
                                    value.substring(0, split),
                                    Integer.parseInt(value.substring(split + 1)));
                        })
                .toList();
    }

    private static BufferedReader read(String file) {
        var stream = WorkshopRecipes.class.getResourceAsStream(ROOT + file);
        if (stream == null) {
            throw new IllegalStateException("The packaged workshop is missing " + file);
        }
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
