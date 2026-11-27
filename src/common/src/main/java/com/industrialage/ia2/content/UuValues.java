package com.industrialage.ia2.content;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Pure UU-Matter scanner valuations and UU Assembly Bench templates with common-tag fallback.
 *
 * <p>Exact canonical IDs are checked first; when an item from another mod is not listed directly,
 * ordered common-tag equivalents (specific metal/ore/gem tags before broad stone/log tags) supply
 * the equivalent microliter cost or assembly recipe.
 */
public final class UuValues {
    /** One UU Assembly Bench recipe mapping a template item and UU cost to an output stack. */
    public record AssemblyRecipe(String templateId, int uuCost, String outputId, int outputCount) {}

    private record TagCost(String tag, int microliters) {}

    private record TagAssembly(String tag, int uuCost, int outputCount) {}

    private static final Map<String, Integer> EXACT_SCAN_COSTS = exactScanCosts();
    private static final List<TagCost> TAG_SCAN_COSTS =
            List.of(
                    new TagCost("c:ores/iridium", 120_000),
                    new TagCost("c:nuggets/iridium", 15_000),
                    new TagCost("c:gems/diamond", 40_000),
                    new TagCost("c:gems/emerald", 25_000),
                    new TagCost("c:ores/uranium", 15_000),
                    new TagCost("c:raw_materials/uranium", 15_000),
                    new TagCost("c:ingots/uranium", 15_000),
                    new TagCost("c:ores/gold", 8_000),
                    new TagCost("c:raw_materials/gold", 8_000),
                    new TagCost("c:ingots/gold", 8_000),
                    new TagCost("c:ores/silver", 6_000),
                    new TagCost("c:raw_materials/silver", 6_000),
                    new TagCost("c:dusts/silver", 6_000),
                    new TagCost("c:ingots/silver", 6_000),
                    new TagCost("c:ores/lead", 3_000),
                    new TagCost("c:raw_materials/lead", 3_000),
                    new TagCost("c:ingots/lead", 3_000),
                    new TagCost("c:ores/iron", 2_500),
                    new TagCost("c:raw_materials/iron", 2_500),
                    new TagCost("c:ingots/iron", 2_500),
                    new TagCost("c:ores/tin", 2_000),
                    new TagCost("c:raw_materials/tin", 2_000),
                    new TagCost("c:ingots/tin", 2_000),
                    new TagCost("c:ores/copper", 1_500),
                    new TagCost("c:raw_materials/copper", 1_500),
                    new TagCost("c:ingots/copper", 1_500),
                    new TagCost("c:dusts/glowstone", 1_500),
                    new TagCost("c:gems/lapis", 1_500),
                    new TagCost("c:gems/quartz", 1_500),
                    new TagCost("c:obsidians", 1_200),
                    new TagCost("c:dusts/redstone", 1_000),
                    new TagCost("c:rubbers", 1_000),
                    new TagCost("minecraft:logs", 50),
                    new TagCost("c:sands", 20),
                    new TagCost("c:gravels", 20),
                    new TagCost("c:stones", 20),
                    new TagCost("c:cobblestones", 10));

    private static final Map<String, AssemblyRecipe> EXACT_ASSEMBLY = exactAssembly();
    private static final List<TagAssembly> TAG_ASSEMBLY =
            List.of(
                    new TagAssembly("c:gems/diamond", 9, 1),
                    new TagAssembly("c:ores/iridium", 7, 1),
                    new TagAssembly("c:ores/uranium", 5, 1),
                    new TagAssembly("c:ores/gold", 3, 2),
                    new TagAssembly("c:ores/copper", 2, 5),
                    new TagAssembly("c:ores/tin", 2, 5),
                    new TagAssembly("c:ores/iron", 2, 2),
                    new TagAssembly("c:ores/lead", 2, 2),
                    new TagAssembly("c:obsidians", 2, 12),
                    new TagAssembly("c:dusts/glowstone", 2, 8),
                    new TagAssembly("c:dusts/redstone", 1, 24),
                    new TagAssembly("c:gems/lapis", 1, 9),
                    new TagAssembly("c:glass_blocks", 1, 32),
                    new TagAssembly("minecraft:logs", 1, 8),
                    new TagAssembly("c:stones", 1, 16),
                    new TagAssembly("c:cobblestones", 1, 16));

    private UuValues() {}

    /**
     * Resolves the UU-Matter scan/replication cost in microliters for {@code itemId}, checking
     * built-in exact items first and falling back to common-tag equivalence when {@code matchesTag}
     * is supplied.
     */
    public static int scanCostMicroliters(String itemId, Predicate<String> matchesTag) {
        if (itemId == null || itemId.isEmpty()) {
            return 0;
        }
        Integer exact = EXACT_SCAN_COSTS.get(itemId);
        if (exact != null) {
            return exact;
        }
        if (matchesTag != null) {
            for (var entry : TAG_SCAN_COSTS) {
                if (ProcessingMachine.matchesTagAlias(entry.tag(), matchesTag)) {
                    return entry.microliters();
                }
            }
        }
        return 0;
    }

    /**
     * Resolves the UU Assembly Bench recipe for a template item, checking exact templates first and
     * falling back to common-tag equivalents (producing the placed template item itself).
     */
    public static AssemblyRecipe assemblyRecipe(String templateId, Predicate<String> matchesTag) {
        if (templateId == null || templateId.isEmpty()) {
            return null;
        }
        var exact = EXACT_ASSEMBLY.get(templateId);
        if (exact != null) {
            return exact;
        }
        if (matchesTag != null) {
            for (var entry : TAG_ASSEMBLY) {
                if (ProcessingMachine.matchesTagAlias(entry.tag(), matchesTag)) {
                    return new AssemblyRecipe(
                            templateId, entry.uuCost(), templateId, entry.outputCount());
                }
            }
        }
        return null;
    }

    /** Returns all canonical UU Assembly Bench recipes in display order. */
    public static List<AssemblyRecipe> allAssemblyRecipes() {
        return List.copyOf(EXACT_ASSEMBLY.values());
    }

    private static Map<String, Integer> exactScanCosts() {
        var map = new LinkedHashMap<String, Integer>();
        map.put("minecraft:cobblestone", 10);
        map.put("minecraft:dirt", 10);
        map.put("minecraft:stone", 20);
        map.put("minecraft:sand", 20);
        map.put("minecraft:gravel", 20);
        map.put("minecraft:oak_log", 50);
        map.put("minecraft:clay_ball", 80);
        map.put("minecraft:coal", 1_000);
        map.put("minecraft:redstone", 1_000);
        map.put("ic2:resin", 1_000);
        map.put("ic2:rubber", 1_000);
        map.put("minecraft:obsidian", 1_200);
        map.put("minecraft:glowstone_dust", 1_500);
        map.put("minecraft:lapis_lazuli", 1_500);
        map.put("minecraft:quartz", 1_500);
        map.put("minecraft:copper_ore", 1_500);
        map.put("minecraft:raw_copper", 1_500);
        map.put("minecraft:copper_ingot", 1_500);
        map.put("ic2:tin_ore", 2_000);
        map.put("ic2:raw_tin", 2_000);
        map.put("ic2:tin_ingot", 2_000);
        map.put("minecraft:iron_ore", 2_500);
        map.put("minecraft:raw_iron", 2_500);
        map.put("minecraft:iron_ingot", 2_500);
        map.put("ic2:lead_ore", 3_000);
        map.put("ic2:raw_lead", 3_000);
        map.put("ic2:lead_ingot", 3_000);
        map.put("ic2:silver_dust", 6_000);
        map.put("ic2:silver_ingot", 6_000);
        map.put("minecraft:gold_ore", 8_000);
        map.put("minecraft:raw_gold", 8_000);
        map.put("minecraft:gold_ingot", 8_000);
        map.put("ic2:uranium_ore", 15_000);
        map.put("ic2:raw_uranium", 15_000);
        map.put("ic2:uranium_ingot", 15_000);
        map.put("ic2:iridium_shard", 15_000);
        map.put("minecraft:emerald", 25_000);
        map.put("minecraft:diamond", 40_000);
        map.put("ic2:iridium_ore", 120_000);
        map.put("minecraft:netherite_scrap", 250_000);
        return Map.copyOf(map);
    }

    private static Map<String, AssemblyRecipe> exactAssembly() {
        var map = new LinkedHashMap<String, AssemblyRecipe>();
        putAssembly(map, "ic2:uu_matter", 1, "minecraft:oak_log", 8);
        putAssembly(map, "minecraft:oak_log", 1, "minecraft:oak_log", 8);
        putAssembly(map, "minecraft:cobblestone", 1, "minecraft:cobblestone", 16);
        putAssembly(map, "minecraft:stone", 1, "minecraft:stone", 16);
        putAssembly(map, "minecraft:glass", 1, "minecraft:glass", 32);
        putAssembly(map, "ic2:resin", 1, "ic2:resin", 21);
        putAssembly(map, "minecraft:redstone", 1, "minecraft:redstone", 24);
        putAssembly(map, "minecraft:lapis_lazuli", 1, "minecraft:lapis_lazuli", 9);
        putAssembly(map, "minecraft:coal", 1, "minecraft:coal", 20);
        putAssembly(map, "minecraft:glowstone_dust", 2, "minecraft:glowstone_dust", 8);
        putAssembly(map, "minecraft:obsidian", 2, "minecraft:obsidian", 12);
        putAssembly(map, "minecraft:copper_ore", 2, "minecraft:copper_ore", 5);
        putAssembly(map, "ic2:tin_ore", 2, "ic2:tin_ore", 5);
        putAssembly(map, "minecraft:iron_ore", 2, "minecraft:iron_ore", 2);
        putAssembly(map, "ic2:lead_ore", 2, "ic2:lead_ore", 2);
        putAssembly(map, "minecraft:gold_ore", 3, "minecraft:gold_ore", 2);
        putAssembly(map, "ic2:uranium_ore", 5, "ic2:uranium_ore", 1);
        putAssembly(map, "ic2:iridium_ore", 7, "ic2:iridium_ore", 1);
        putAssembly(map, "minecraft:diamond", 9, "minecraft:diamond", 1);
        return Map.copyOf(map);
    }

    private static void putAssembly(
            Map<String, AssemblyRecipe> map,
            String templateId,
            int uuCost,
            String outputId,
            int outputCount) {
        map.put(templateId, new AssemblyRecipe(templateId, uuCost, outputId, outputCount));
    }
}
