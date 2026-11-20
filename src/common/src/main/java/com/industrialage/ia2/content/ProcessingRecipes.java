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
        for (String line : IndustrialMaterials.rows("processing-recipes.tsv")) {
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
        if (matchesTag == null) return reference.getOrDefault(machine, Map.of()).get(input);
        for (var ingredient : tagged.getOrDefault(machine, List.of())) {
            if (matchesTag.test(ingredient.id().substring(1))) return ingredient.match(input);
        }
        return null;
    }
}
