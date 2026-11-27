package com.industrialage.ia2.content;

import com.industrialage.ia2.content.ProcessingMachine.Recipe;
import com.industrialage.ia2.content.ProcessingMachine.WeightedCount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Single-result machine recipes and reference tag members, resolved once when first used. */
final class ProcessingRecipes {
    private static final Set<String> WEIGHTED_MACHINES =
            Set.of("macerator", "compressor", "extractor");

    private record Ingredient(
            String id,
            int count,
            String output,
            int resultSize,
            List<WeightedCount> weightedCounts) {
        Recipe match(String input) {
            return new Recipe(input, count, output, resultSize, weightedCounts);
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
        // Register weighted ore-block recipes before generic stone/tag rules so ore blocks that
        // also carry a broad stone tag still resolve to their specific ore maceration recipe.
        for (String line : IndustrialMaterials.rows("workshop-recipes.tsv")) {
            String[] row = line.split("\t", -1);
            if (row.length < 5
                    || !WEIGHTED_MACHINES.contains(row[0])
                    || !row[4].contains("\"weighted\":true")) {
                continue;
            }
            int inSplit = row[2].lastIndexOf('@');
            if (inSplit <= 0) continue;
            String input = row[2].substring(0, inSplit);
            int inputCount = Integer.parseInt(row[2].substring(inSplit + 1));
            var weights = new ArrayList<WeightedCount>();
            String outputId = null;
            for (String token : row[3].split(",")) {
                int at = token.lastIndexOf('@');
                int star = token.lastIndexOf('*');
                if (at <= 0 || star <= at) continue;
                String candidate = token.substring(0, at);
                if (outputId == null) outputId = candidate;
                int count = Integer.parseInt(token.substring(at + 1, star));
                int weight = Integer.parseInt(token.substring(star + 1));
                weights.add(new WeightedCount(count, weight));
            }
            if (outputId != null && !weights.isEmpty()) {
                register(
                        row[0],
                        input,
                        inputCount,
                        outputId,
                        weights.get(0).count(),
                        weights,
                        memberships);
            }
        }
        var lines = new ArrayList<>(IndustrialMaterials.rows("processing-recipes.tsv"));
        lines.addAll(IndustrialMaterials.rows("classic-processing.tsv"));
        for (String line : lines) {
            String[] row = line.split("\t");
            register(
                    row[0],
                    row[2],
                    Integer.parseInt(row[3]),
                    row[4],
                    Integer.parseInt(row[5]),
                    List.of(),
                    memberships);
        }
    }

    private void register(
            String machine,
            String input,
            int count,
            String output,
            int resultSize,
            List<WeightedCount> weightedCounts,
            Map<String, List<String>> memberships) {
        var ingredient = new Ingredient(input, count, output, resultSize, weightedCounts);
        if (input.startsWith("#")) {
            tagged.computeIfAbsent(machine, ignored -> new ArrayList<>()).add(ingredient);
            String tagName = input.substring(1);
            for (String member : memberships.getOrDefault(tagName, List.of())) {
                reference
                        .computeIfAbsent(machine, ignored -> new HashMap<>())
                        .putIfAbsent(member, ingredient.match(member));
            }
        } else {
            direct.computeIfAbsent(machine, ignored -> new HashMap<>())
                    .put(input, ingredient.match(input));
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
        if ("minecraft".equals(ns) && path.endsWith("_ores")) {
            String metal = path.substring(0, path.length() - 5);
            if (!metal.isEmpty()
                    && (matchesTag.test("forge:ores/" + metal)
                            || matchesTag.test("c:ores/" + metal))) {
                return true;
            }
        }
        if (("forge".equals(ns) || "c".equals(ns)) && path.startsWith("ores/")) {
            String metal = path.substring(5);
            if ((metal.equals("copper") || metal.equals("iron") || metal.equals("gold"))
                    && matchesTag.test("minecraft:" + metal + "_ores")) {
                return true;
            }
        }
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
        if (path.equals("tiny_dusts")) return "small_dusts";
        if (path.startsWith("tiny_dusts/")) return "small_dusts/" + path.substring(11);
        if (path.equals("small_dusts")) return "tiny_dusts";
        if (path.startsWith("small_dusts/")) return "tiny_dusts/" + path.substring(12);
        if (path.equals("rubbers")) return "rubber";
        if (path.equals("rubber")) return "rubbers";
        if (path.equals("coal_coke")) return "fuels/coal_coke";
        if (path.equals("fuels/coal_coke")) return "coal_coke";
        if (path.equals("tools/wrench") || path.equals("tools/wrenches")) return "wrenches";
        if (path.equals("wrenches")) return "tools/wrench";
        if (path.equals("obsidian")) return "obsidians";
        if (path.equals("obsidians")) return "obsidian";
        if (path.equals("glass")) return "glass_blocks";
        if (path.equals("glass_blocks")) return "glass";
        if (path.equals("sand")) return "sands";
        if (path.equals("sands")) return "sand";
        if (path.equals("stone")) return "stones";
        if (path.equals("stones")) return "stone";
        if (path.equals("cobblestone")) return "cobblestones";
        if (path.equals("cobblestones")) return "cobblestone";
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
                            ingredient.resultSize(),
                            ingredient.weightedCounts()));
        }
        return List.copyOf(list);
    }
}
