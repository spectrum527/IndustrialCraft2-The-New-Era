package com.industrialage.ia2.content;

import com.industrialage.ia2.content.ProcessingMachine.Recipe;
import com.industrialage.ia2.content.ProcessingMachine.WeightedCount;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Single-result machine recipes and reference tag members, resolved once when first used. */
final class ProcessingRecipes {
    private static final Set<String> WEIGHTED_MACHINES =
            Set.of("macerator", "compressor", "extractor");
    private static final Set<String> VANILLA_ORE_METALS =
            Set.of("coal", "copper", "iron", "gold", "lapis", "redstone", "diamond", "emerald");
    private static final Set<String> BROAD_TAG_PATHS =
            Set.of(
                    "stone",
                    "stones",
                    "cobblestone",
                    "cobblestones",
                    "sand",
                    "sands",
                    "gravel",
                    "gravels",
                    "netherrack",
                    "netherracks",
                    "sandstone",
                    "sandstone/blocks",
                    "sandstones",
                    "obsidian",
                    "obsidians",
                    "glass",
                    "glass_blocks",
                    "wool",
                    "leaves",
                    "saplings",
                    "planks",
                    "logs");

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
                register(row[0], input, inputCount, outputId, weights.get(0).count(), weights);
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
                    List.of());
        }
        // Sort tag ingredients stably so specific ore/metal/material tags always take precedence
        // over broad stone/cobblestone/sand/gravel/log tags (e.g. #forge:ores/silver before
        // #forge:cobblestone), then populate offline reference memberships in the same order.
        for (var entry : tagged.entrySet()) {
            String machine = entry.getKey();
            var list = entry.getValue();
            list.sort(Comparator.comparingInt(ProcessingRecipes::tagPriority));
            var ref = reference.computeIfAbsent(machine, ignored -> new HashMap<>());
            for (var ingredient : list) {
                String tagName = ingredient.id().substring(1);
                for (String candidate : tagAliasCandidates(tagName)) {
                    for (String member : memberships.getOrDefault(candidate, List.of())) {
                        ref.putIfAbsent(member, ingredient.match(member));
                    }
                }
            }
        }
    }

    private static int tagPriority(Ingredient ingredient) {
        if (!ingredient.weightedCounts().isEmpty()) {
            return 0;
        }
        String raw =
                ingredient.id().startsWith("#") ? ingredient.id().substring(1) : ingredient.id();
        int colon = raw.indexOf(':');
        String path = colon >= 0 ? raw.substring(colon + 1) : raw;
        return BROAD_TAG_PATHS.contains(path) ? 2 : 1;
    }

    private void register(
            String machine,
            String input,
            int count,
            String output,
            int resultSize,
            List<WeightedCount> weightedCounts) {
        var ingredient = new Ingredient(input, count, output, resultSize, weightedCounts);
        if (input.startsWith("#")) {
            tagged.computeIfAbsent(machine, ignored -> new ArrayList<>()).add(ingredient);
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
        for (String candidate : tagAliasCandidates(tag)) {
            if (matchesTag.test(candidate)) return true;
        }
        return false;
    }

    static List<String> tagAliasCandidates(String tag) {
        if (tag == null || tag.isEmpty()) return List.of();
        var candidates = new LinkedHashSet<String>();
        candidates.add(tag);
        int colon = tag.indexOf(':');
        if (colon <= 0) {
            return List.copyOf(candidates);
        }
        String ns = tag.substring(0, colon);
        String path = tag.substring(colon + 1);
        if ("minecraft".equals(ns) && path.endsWith("_ores")) {
            String metal = path.substring(0, path.length() - 5);
            if (!metal.isEmpty()) {
                candidates.add("forge:ores/" + metal);
                candidates.add("c:ores/" + metal);
            }
            return List.copyOf(candidates);
        }
        if (("forge".equals(ns) || "c".equals(ns)) && path.startsWith("ores/")) {
            String metal = path.substring(5);
            if (VANILLA_ORE_METALS.contains(metal)) {
                candidates.add("minecraft:" + metal + "_ores");
            }
        }
        String altNs = "forge".equals(ns) ? "c" : "c".equals(ns) ? "forge" : null;
        if (altNs != null) {
            candidates.add(altNs + ":" + path);
        }
        for (String altPath : alternateTagPaths(path)) {
            candidates.add(ns + ":" + altPath);
            if (altNs != null) {
                candidates.add(altNs + ":" + altPath);
            }
        }
        return List.copyOf(candidates);
    }

    private static List<String> alternateTagPaths(String path) {
        if (path.equals("raw_ores")) return List.of("raw_materials");
        if (path.startsWith("raw_ores/")) return List.of("raw_materials/" + path.substring(9));
        if (path.equals("raw_materials")) return List.of("raw_ores");
        if (path.startsWith("raw_materials/")) return List.of("raw_ores/" + path.substring(14));
        if (path.equals("blocks")) return List.of("storage_blocks");
        if (path.startsWith("blocks/")) return List.of("storage_blocks/" + path.substring(7));
        if (path.equals("storage_blocks")) return List.of("blocks");
        if (path.startsWith("storage_blocks/")) return List.of("blocks/" + path.substring(15));
        if (path.equals("tiny_dusts")) return List.of("small_dusts");
        if (path.startsWith("tiny_dusts/")) return List.of("small_dusts/" + path.substring(11));
        if (path.equals("small_dusts")) return List.of("tiny_dusts");
        if (path.startsWith("small_dusts/")) return List.of("tiny_dusts/" + path.substring(12));
        if (path.equals("dense_plates")) return List.of("plates/dense");
        if (path.startsWith("dense_plates/")) return List.of("plates/dense/" + path.substring(13));
        if (path.equals("plates/dense")) return List.of("dense_plates");
        if (path.startsWith("plates/dense/")) return List.of("dense_plates/" + path.substring(13));
        if (path.equals("sand")) return List.of("sands");
        if (path.startsWith("sand/")) return List.of("sands/" + path.substring(5));
        if (path.equals("sands")) return List.of("sand");
        if (path.startsWith("sands/")) return List.of("sand/" + path.substring(6));
        return switch (path) {
            case "rubbers" -> List.of("rubber");
            case "rubber" -> List.of("rubbers");
            case "coal_coke" -> List.of("fuels/coal_coke");
            case "fuels/coal_coke" -> List.of("coal_coke");
            case "tools/wrench" -> List.of("tools/wrenches", "wrenches");
            case "tools/wrenches" -> List.of("tools/wrench", "wrenches");
            case "wrenches" -> List.of("tools/wrench", "tools/wrenches");
            case "shears" -> List.of("tools/shear", "tools/shears");
            case "tools/shear" -> List.of("shears", "tools/shears");
            case "tools/shears" -> List.of("shears", "tools/shear");
            case "obsidian" -> List.of("obsidians");
            case "obsidians" -> List.of("obsidian");
            case "glass" -> List.of("glass_blocks");
            case "glass_blocks" -> List.of("glass");
            case "stone" -> List.of("stones");
            case "stones" -> List.of("stone");
            case "cobblestone" -> List.of("cobblestones");
            case "cobblestones" -> List.of("cobblestone");
            case "gravel" -> List.of("gravels");
            case "gravels" -> List.of("gravel");
            case "slimeball", "slimeballs" -> List.of("slime_balls", "slimeball", "slimeballs");
            case "slime_balls" -> List.of("slimeball", "slimeballs");
            case "netherrack" -> List.of("netherracks");
            case "netherracks" -> List.of("netherrack");
            case "sandstone" -> List.of("sandstone/blocks", "sandstones");
            case "sandstone/blocks" -> List.of("sandstone", "sandstones");
            case "sandstones" -> List.of("sandstone", "sandstone/blocks");
            case "leather" -> List.of("leathers");
            case "leathers" -> List.of("leather");
            case "string" -> List.of("strings");
            case "strings" -> List.of("string");
            case "bone" -> List.of("bones");
            case "bones" -> List.of("bone");
            case "feather" -> List.of("feathers");
            case "feathers" -> List.of("feather");
            case "gunpowder" -> List.of("gunpowders");
            case "gunpowders" -> List.of("gunpowder");
            case "ingots/brick" -> List.of("bricks/normal");
            case "bricks/normal" -> List.of("ingots/brick");
            case "ingots/nether_brick" -> List.of("bricks/nether");
            case "bricks/nether" -> List.of("ingots/nether_brick");
            default -> List.of();
        };
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
