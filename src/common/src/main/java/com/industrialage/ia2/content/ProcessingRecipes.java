package com.industrialage.ia2.content;

import com.industrialage.ia2.content.ProcessingMachine.Recipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Single-result machine recipes and reference tag members, resolved once when first used. */
final class ProcessingRecipes {
    private record Ingredient(String id, int count, String output, int resultSize) {
        Recipe match(String input) {
            return new Recipe(input, count, output, resultSize);
        }
    }

    private final Map<String, Map<String, Recipe>> direct = new HashMap<>();
    private final Map<String, List<Ingredient>> tagged = new HashMap<>();
    private final Map<String, Map<String, Recipe>> reference = new HashMap<>();
    static final ProcessingRecipes BUILTIN = new ProcessingRecipes();

    private ProcessingRecipes() {
        Map<String, List<String>> memberships = new HashMap<>();
        for (String line : IndustrialMaterials.rows("processing-tags.tsv")) {
            String[] row = line.split("\t", -1);
            memberships.put(
                    row[0],
                    row.length < 2 || row[1].isEmpty() ? List.of() : List.of(row[1].split(",")));
        }
        var lines = new ArrayList<>(IndustrialMaterials.rows("processing-recipes.tsv"));
        lines.addAll(IndustrialMaterials.rows("classic-processing.tsv"));
        for (String line : lines) {
            String[] row = line.split("\t");
            String input = row[2];
            int count = Integer.parseInt(row[3]);
            var ingredient = new Ingredient(input, count, row[4], Integer.parseInt(row[5]));
            if (input.startsWith("#")) {
                tagged.computeIfAbsent(row[0], ignored -> new ArrayList<>()).add(ingredient);
                for (String member : memberships.get(input.substring(1))) {
                    reference
                            .computeIfAbsent(row[0], ignored -> new HashMap<>())
                            .put(member, ingredient.match(member));
                }
            } else {
                direct.computeIfAbsent(row[0], ignored -> new HashMap<>())
                        .put(input, ingredient.match(input));
            }
        }
    }

    Recipe find(String machine, String input, Predicate<String> matchesTag) {
        var exact = direct.getOrDefault(machine, Map.of()).get(input);
        if (exact != null) return exact;
        if (matchesTag != null) {
            for (var ingredient : tagged.getOrDefault(machine, List.of())) {
                if (matchesTagAlias(ingredient.id().substring(1), matchesTag)) {
                    return ingredient.match(input);
                }
            }
        }
        return reference.getOrDefault(machine, Map.of()).get(input);
    }

    static boolean matchesTagAlias(String tag, Predicate<String> matchesTag) {
        if (tag == null || tag.isEmpty() || matchesTag == null) return false;
        if (matchesTag.test(tag)) return true;
        int colon = tag.indexOf(':');
        if (colon <= 0) return false;
        String ns = tag.substring(0, colon);
        String path = tag.substring(colon + 1);
        String altNs = "forge".equals(ns) ? "c" : "c".equals(ns) ? "forge" : null;
        if (altNs != null && matchesTag.test(altNs + ":" + path)) return true;
        String altPath = alternateTagPath(path);
        if (altPath != null) {
            if (matchesTag.test(ns + ":" + altPath)) return true;
            if (altNs != null && matchesTag.test(altNs + ":" + altPath)) return true;
        }
        return false;
    }

    private static String alternateTagPath(String path) {
        if (path.equals("raw_ores")) return "raw_materials";
        if (path.startsWith("raw_ores/")) return "raw_materials/" + path.substring(9);
        if (path.equals("raw_materials")) return "raw_ores";
        if (path.startsWith("raw_materials/")) return "raw_ores/" + path.substring(14);
        if (path.equals("blocks")) return "storage_blocks";
        if (path.startsWith("blocks/")) return "storage_blocks/" + path.substring(7);
        if (path.equals("storage_blocks")) return "blocks";
        if (path.startsWith("storage_blocks/")) return "blocks/" + path.substring(15);
        return null;
    }

    List<Recipe> all(String machine) {
        var list = new ArrayList<Recipe>();
        list.addAll(direct.getOrDefault(machine, Map.of()).values());
        for (var ingredient : tagged.getOrDefault(machine, List.of())) {
            list.add(
                    new Recipe(
                            ingredient.id(),
                            ingredient.count(),
                            ingredient.output(),
                            ingredient.resultSize()));
        }
        return List.copyOf(list);
    }
}
