package com.industrialage.ia2.compat;

import com.industrialage.ia2.content.ProcessingMachine;
import com.industrialage.ia2.content.RecyclerRules;
import com.industrialage.ia2.content.UuValues;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shared recipe catalog for IC2's 18 machine processing categories, consumed by both JEI and EMI
 * plugins across NeoForge 1.21.1, 26.1.2, and 26.2 without importing any viewer API.
 */
final class Ic2RecipeCatalog {
    enum SourceKind {
        PROCESSING,
        WORKSHOP,
        CANNER_ENRICH,
        COKE_KILN,
        RECYCLER,
        ELECTROLYZER,
        FERMENTER,
        MASS_FABRICATOR,
        UU_ASSEMBLY
    }

    record CategorySpec(
            String id,
            String titleKey,
            boolean blockTitle,
            String iconItemId,
            List<String> catalystItemIds,
            SourceKind source) {}

    record ItemInput(String tag, int count, List<ItemStack> stacks) {
        boolean isTag() {
            return tag != null && !tag.isEmpty();
        }
    }

    record FluidAmount(String fluidId, int amountMb) {}

    record MachineRecipe(
            String id,
            List<ItemInput> itemInputs,
            List<FluidAmount> fluidInputs,
            List<ItemStack> itemOutputs,
            List<FluidAmount> fluidOutputs,
            String note) {
        int inputSlots() {
            return itemInputs.size() + fluidInputs.size();
        }

        int outputSlots() {
            return itemOutputs.size() + fluidOutputs.size();
        }
    }

    static final List<CategorySpec> CATEGORIES =
            List.of(
                    category(
                            "macerator",
                            "ic2:macerator",
                            List.of("ic2:macerator"),
                            SourceKind.PROCESSING),
                    category(
                            "extractor",
                            "ic2:extractor",
                            List.of("ic2:extractor"),
                            SourceKind.PROCESSING),
                    category(
                            "compressor",
                            "ic2:compressor",
                            List.of("ic2:compressor"),
                            SourceKind.PROCESSING),
                    category(
                            "metal_former_rolling",
                            "ic2:metal_former",
                            List.of("ic2:metal_former"),
                            SourceKind.WORKSHOP),
                    category(
                            "metal_former_extruding",
                            "ic2:metal_former",
                            List.of("ic2:metal_former"),
                            SourceKind.WORKSHOP),
                    category(
                            "metal_former_cutting",
                            "ic2:metal_former",
                            List.of("ic2:metal_former"),
                            SourceKind.WORKSHOP),
                    category(
                            "ore_washer",
                            "ic2:ore_washing_plant",
                            List.of("ic2:ore_washing_plant"),
                            SourceKind.WORKSHOP),
                    category(
                            "centrifuge",
                            "ic2:centrifuge",
                            List.of("ic2:centrifuge"),
                            SourceKind.WORKSHOP),
                    category(
                            "blast_furnace",
                            "ic2:blast_furnace",
                            List.of("ic2:blast_furnace"),
                            SourceKind.WORKSHOP),
                    category(
                            "block_cutter",
                            "ic2:block_cutter",
                            List.of("ic2:block_cutter"),
                            SourceKind.WORKSHOP),
                    category(
                            "canner_bottle",
                            "ic2:canner",
                            List.of("ic2:canner", "ic2:solid_canner", "ic2:classic_canner"),
                            SourceKind.WORKSHOP),
                    category(
                            "canner_enrich",
                            "ic2:canner",
                            List.of("ic2:canner"),
                            SourceKind.CANNER_ENRICH),
                    blockCategory(
                            "coke_kiln",
                            "coke_kiln",
                            List.of("ic2:coke_kiln"),
                            SourceKind.COKE_KILN),
                    blockCategory(
                            "recycler", "recycler", List.of("ic2:recycler"), SourceKind.RECYCLER),
                    blockCategory(
                            "electrolyzer",
                            "electrolyzer",
                            List.of("ic2:electrolyzer", "ic2:classic_electrolyzer"),
                            SourceKind.ELECTROLYZER),
                    blockCategory(
                            "fermenter",
                            "fermenter",
                            List.of("ic2:fermenter"),
                            SourceKind.FERMENTER),
                    blockCategory(
                            "mass_fabricator",
                            "mass_fabricator",
                            List.of(
                                    "ic2:mass_fabricator",
                                    "ic2:classic_mass_fabricator",
                                    "ic2:matter_generator"),
                            SourceKind.MASS_FABRICATOR),
                    blockCategory(
                            "uu_assembly",
                            "uu_assembly_bench",
                            List.of("ic2:uu_assembly_bench"),
                            SourceKind.UU_ASSEMBLY));

    private Ic2RecipeCatalog() {}

    private static CategorySpec category(
            String id, String iconItemId, List<String> catalysts, SourceKind source) {
        return new CategorySpec(id, "jei.ic2.category." + id, false, iconItemId, catalysts, source);
    }

    private static CategorySpec blockCategory(
            String id, String blockPath, List<String> catalysts, SourceKind source) {
        return new CategorySpec(
                id, "block.ic2." + blockPath, true, "ic2:" + blockPath, catalysts, source);
    }

    static Map<CategorySpec, List<MachineRecipe>> buildAll() {
        var tagCache = new HashMap<String, List<Item>>();
        var all = new LinkedHashMap<CategorySpec, List<MachineRecipe>>();
        for (var spec : CATEGORIES) {
            all.put(spec, recipesFor(spec, tagCache));
        }
        return all;
    }

    static List<MachineRecipe> recipesFor(CategorySpec category) {
        return recipesFor(category, new HashMap<>());
    }

    private static List<MachineRecipe> recipesFor(
            CategorySpec category, Map<String, List<Item>> tagCache) {
        return switch (category.source()) {
            case PROCESSING -> processingRecipes(category.id(), tagCache);
            case WORKSHOP -> workshopRecipes(category.id(), tagCache);
            case CANNER_ENRICH -> cannerEnrichRecipes(tagCache);
            case COKE_KILN -> cokeKilnRecipes(tagCache);
            case RECYCLER -> recyclerRecipes(tagCache);
            case ELECTROLYZER -> electrolyzerRecipes();
            case FERMENTER -> fermenterRecipes();
            case MASS_FABRICATOR -> massFabricatorRecipes();
            case UU_ASSEMBLY -> uuAssemblyRecipes();
        };
    }

    private static List<MachineRecipe> processingRecipes(
            String categoryId, Map<String, List<Item>> tagCache) {
        var result = new ArrayList<MachineRecipe>();
        int idx = 0;
        for (var recipe : ProcessingMachine.allRecipes(categoryId)) {
            var input = resolveInput(recipe.input(), recipe.inputCount(), tagCache);
            var output = WorkshopPlatform.itemStack(recipe.output(), recipe.outputCount());
            if (input != null && !output.isEmpty()) {
                String note =
                        recipe.weightedCounts().isEmpty()
                                ? ""
                                : recipe.outputCount() + "-" + recipe.maxOutputCount();
                result.add(
                        new MachineRecipe(
                                categoryId + "_" + (idx++),
                                List.of(input),
                                List.of(),
                                List.of(output),
                                List.of(),
                                note));
            }
        }
        return result;
    }

    private static List<MachineRecipe> workshopRecipes(
            String categoryId, Map<String, List<Item>> tagCache) {
        var result = new ArrayList<MachineRecipe>();
        int idx = 0;
        for (var recipe : WorkshopPlatform.workshopRecipes(categoryId)) {
            var inputs = new ArrayList<ItemInput>();
            boolean valid = true;
            for (var in : recipe.inputs()) {
                var resolved = resolveInput(in.id(), in.count(), tagCache);
                if (resolved == null) {
                    valid = false;
                    break;
                }
                inputs.add(resolved);
            }
            if (!valid || inputs.isEmpty()) continue;
            var outputs = new ArrayList<ItemStack>();
            for (var out : recipe.outputs()) {
                var stack = WorkshopPlatform.itemStack(out.id(), out.count());
                if (!stack.isEmpty()) outputs.add(stack);
            }
            if (outputs.isEmpty()) continue;
            var fluidInputs = new ArrayList<FluidAmount>();
            if (categoryId.equals("ore_washer")) {
                int water = recipe.requirements().getOrDefault("amount", 100);
                addFluidIfRegistered(fluidInputs, "minecraft:water", water);
            } else if (categoryId.equals("blast_furnace")) {
                int duration = recipe.requirements().getOrDefault("duration", 6000);
                int perTick = recipe.requirements().getOrDefault("fluid", 1);
                addFluidIfRegistered(fluidInputs, "ic2:air", Math.max(1, duration * perTick));
            }
            result.add(
                    new MachineRecipe(
                            categoryId + "_" + (idx++),
                            List.copyOf(inputs),
                            List.copyOf(fluidInputs),
                            List.copyOf(outputs),
                            List.of(),
                            ""));
        }
        return result;
    }

    private static List<MachineRecipe> cannerEnrichRecipes(Map<String, List<Item>> tagCache) {
        var result = new ArrayList<MachineRecipe>();
        int idx = 0;
        for (var recipe : WorkshopPlatform.workshopRecipes("canner_enrich")) {
            if (recipe.inputs().size() < 2 || recipe.outputs().isEmpty()) continue;
            var fluidInSpec = recipe.inputs().get(0);
            var fluidOutSpec = recipe.outputs().get(0);
            if (WorkshopPlatform.fluid(fluidInSpec.id(), fluidInSpec.count()).isEmpty()
                    || WorkshopPlatform.fluid(fluidOutSpec.id(), fluidOutSpec.count()).isEmpty()) {
                continue;
            }
            var itemInputs = new ArrayList<ItemInput>();
            boolean valid = true;
            for (int i = 1; i < recipe.inputs().size(); i++) {
                var in = recipe.inputs().get(i);
                var resolved = resolveInput(in.id(), in.count(), tagCache);
                if (resolved == null) {
                    valid = false;
                    break;
                }
                itemInputs.add(resolved);
            }
            if (!valid || itemInputs.isEmpty()) continue;
            result.add(
                    new MachineRecipe(
                            "canner_enrich_" + (idx++),
                            List.copyOf(itemInputs),
                            List.of(new FluidAmount(fluidInSpec.id(), fluidInSpec.count())),
                            List.of(),
                            List.of(new FluidAmount(fluidOutSpec.id(), fluidOutSpec.count())),
                            ""));
        }
        return result;
    }

    private static List<MachineRecipe> cokeKilnRecipes(Map<String, List<Item>> tagCache) {
        var result = new ArrayList<MachineRecipe>();
        var coal = resolveInput("minecraft:coal", 1, tagCache);
        var coke = WorkshopPlatform.itemStack("ic2:coke", 1);
        if (coal != null
                && !coke.isEmpty()
                && !WorkshopPlatform.fluid("ic2:creosote", 500).isEmpty()) {
            result.add(
                    new MachineRecipe(
                            "coke_kiln_coal",
                            List.of(coal),
                            List.of(),
                            List.of(coke),
                            List.of(new FluidAmount("ic2:creosote", 500)),
                            "1200t"));
        }
        var logs = resolveInput("#minecraft:logs", 1, tagCache);
        var charcoal = WorkshopPlatform.itemStack("minecraft:charcoal", 1);
        if (logs != null
                && !charcoal.isEmpty()
                && !WorkshopPlatform.fluid("ic2:creosote", 250).isEmpty()) {
            result.add(
                    new MachineRecipe(
                            "coke_kiln_logs",
                            List.of(logs),
                            List.of(),
                            List.of(charcoal),
                            List.of(new FluidAmount("ic2:creosote", 250)),
                            "1200t"));
        }
        return result;
    }

    private static List<MachineRecipe> recyclerRecipes(Map<String, List<Item>> tagCache) {
        var result = new ArrayList<MachineRecipe>();
        var sample = resolveInput("#c:cobblestones", 1, tagCache);
        var scrap = WorkshopPlatform.itemStack("ic2:scrap", 1);
        if (sample != null && !scrap.isEmpty()) {
            result.add(
                    new MachineRecipe(
                            "recycler_scrap",
                            List.of(sample),
                            List.of(),
                            List.of(scrap),
                            List.of(),
                            "12.5%"));
        }
        var excluded = new LinkedHashSet<Item>();
        for (String id : RecyclerRules.DEFAULT_BLACKLIST) {
            var stack = WorkshopPlatform.itemStack(id, 1);
            if (!stack.isEmpty()) excluded.add(stack.getItem());
        }
        for (String tag : List.of("ic2:recycler_blacklist", "c:glass_panes", "c:rods/wooden")) {
            excluded.addAll(resolveTagItems(tag, tagCache));
        }
        if (!excluded.isEmpty()) {
            var stacks = excluded.stream().map(item -> new ItemStack(item, 1)).toList();
            result.add(
                    new MachineRecipe(
                            "recycler_blacklist",
                            List.of(new ItemInput("ic2:recycler_blacklist", 1, stacks)),
                            List.of(),
                            List.of(),
                            List.of(),
                            "0%"));
        }
        return result;
    }

    private static List<MachineRecipe> electrolyzerRecipes() {
        var result = new ArrayList<MachineRecipe>();
        if (!WorkshopPlatform.fluid("minecraft:water", 40).isEmpty()
                && !WorkshopPlatform.fluid("ic2:hydrogen", 26).isEmpty()
                && !WorkshopPlatform.fluid("ic2:oxygen", 13).isEmpty()) {
            result.add(
                    new MachineRecipe(
                            "electrolyzer_water",
                            List.of(),
                            List.of(new FluidAmount("minecraft:water", 40)),
                            List.of(),
                            List.of(
                                    new FluidAmount("ic2:hydrogen", 26),
                                    new FluidAmount("ic2:oxygen", 13)),
                            ""));
        }
        var waterCell = WorkshopPlatform.itemStack("ic2:water_cell", 1);
        var elecCell = WorkshopPlatform.itemStack("ic2:electrolyzed_water_cell", 1);
        if (!waterCell.isEmpty() && !elecCell.isEmpty()) {
            result.add(
                    new MachineRecipe(
                            "electrolyzer_charge_cell",
                            List.of(new ItemInput(null, 1, List.of(waterCell))),
                            List.of(),
                            List.of(elecCell),
                            List.of(),
                            "-20k EU"));
            result.add(
                    new MachineRecipe(
                            "electrolyzer_discharge_cell",
                            List.of(new ItemInput(null, 1, List.of(elecCell.copy()))),
                            List.of(),
                            List.of(waterCell.copy()),
                            List.of(),
                            "+18k EU"));
        }
        return result;
    }

    private static List<MachineRecipe> fermenterRecipes() {
        var result = new ArrayList<MachineRecipe>();
        if (!WorkshopPlatform.fluid("ic2:biomass", 20).isEmpty()
                && !WorkshopPlatform.fluid("ic2:biogas", 400).isEmpty()) {
            result.add(
                    new MachineRecipe(
                            "fermenter_biogas",
                            List.of(),
                            List.of(new FluidAmount("ic2:biomass", 20)),
                            List.of(),
                            List.of(new FluidAmount("ic2:biogas", 400)),
                            "800 hU"));
            var fertilizer = WorkshopPlatform.itemStack("ic2:fertilizer", 1);
            if (!fertilizer.isEmpty()) {
                result.add(
                        new MachineRecipe(
                                "fermenter_fertilizer",
                                List.of(),
                                List.of(new FluidAmount("ic2:biomass", 500)),
                                List.of(fertilizer),
                                List.of(new FluidAmount("ic2:biogas", 10_000)),
                                "25x"));
            }
        }
        return result;
    }

    private static List<MachineRecipe> massFabricatorRecipes() {
        var result = new ArrayList<MachineRecipe>();
        var scrap = WorkshopPlatform.itemStack("ic2:scrap", 1);
        var scrapBox = WorkshopPlatform.itemStack("ic2:scrap_box", 1);
        var uuItem = WorkshopPlatform.itemStack("ic2:uu_matter", 1);
        boolean hasUuFluid = !WorkshopPlatform.fluid("ic2:uu_matter", 1).isEmpty();
        if (!scrap.isEmpty() && !uuItem.isEmpty()) {
            result.add(
                    new MachineRecipe(
                            "mass_fab_scrap",
                            List.of(new ItemInput(null, 1, List.of(scrap))),
                            List.of(),
                            List.of(uuItem),
                            List.of(),
                            "150k amp"));
        }
        if (!scrapBox.isEmpty() && !uuItem.isEmpty()) {
            result.add(
                    new MachineRecipe(
                            "mass_fab_scrap_box",
                            List.of(new ItemInput(null, 1, List.of(scrapBox))),
                            List.of(),
                            List.of(uuItem.copy()),
                            List.of(),
                            "1.35M amp"));
        }
        if (!scrap.isEmpty() && hasUuFluid) {
            result.add(
                    new MachineRecipe(
                            "matter_gen_scrap",
                            List.of(new ItemInput(null, 1, List.of(scrap.copy()))),
                            List.of(),
                            List.of(),
                            List.of(new FluidAmount("ic2:uu_matter", 1)),
                            "150k amp"));
        }
        if (!scrapBox.isEmpty() && hasUuFluid) {
            result.add(
                    new MachineRecipe(
                            "matter_gen_scrap_box",
                            List.of(new ItemInput(null, 1, List.of(scrapBox.copy()))),
                            List.of(),
                            List.of(),
                            List.of(new FluidAmount("ic2:uu_matter", 1)),
                            "1.35M amp"));
        }
        return result;
    }

    private static List<MachineRecipe> uuAssemblyRecipes() {
        var result = new ArrayList<MachineRecipe>();
        int idx = 0;
        for (var entry : UuValues.allAssemblyRecipes()) {
            var template = WorkshopPlatform.itemStack(entry.templateId(), 1);
            var uu = WorkshopPlatform.itemStack("ic2:uu_matter", entry.uuCost());
            var output = WorkshopPlatform.itemStack(entry.outputId(), entry.outputCount());
            if (!template.isEmpty() && !uu.isEmpty() && !output.isEmpty()) {
                result.add(
                        new MachineRecipe(
                                "uu_assembly_" + (idx++),
                                List.of(
                                        new ItemInput(null, 1, List.of(template)),
                                        new ItemInput(null, entry.uuCost(), List.of(uu))),
                                List.of(),
                                List.of(output),
                                List.of(),
                                ""));
            }
        }
        return result;
    }

    private static void addFluidIfRegistered(
            List<FluidAmount> target, String fluidId, int amountMb) {
        if (amountMb > 0 && !WorkshopPlatform.fluid(fluidId, amountMb).isEmpty()) {
            target.add(new FluidAmount(fluidId, amountMb));
        }
    }

    static ItemStack icon(CategorySpec category) {
        return WorkshopPlatform.itemStack(category.iconItemId(), 1);
    }

    static List<ItemStack> catalysts(CategorySpec category) {
        var list = new ArrayList<ItemStack>();
        for (String id : category.catalystItemIds()) {
            var stack = WorkshopPlatform.itemStack(id, 1);
            if (!stack.isEmpty()) list.add(stack);
        }
        return list;
    }

    private static ItemInput resolveInput(
            String spec, int count, Map<String, List<Item>> tagCache) {
        if (!spec.startsWith("#")) {
            var stack = WorkshopPlatform.itemStack(spec, count);
            return stack.isEmpty() ? null : new ItemInput(null, count, List.of(stack));
        }
        String rawTag = spec.substring(1);
        var items = resolveTagItems(rawTag, tagCache);
        if (items.isEmpty()) return null;
        var stacks = new ArrayList<ItemStack>(items.size());
        for (Item item : items) {
            stacks.add(new ItemStack(item, count));
        }
        return new ItemInput(displayTag(rawTag), count, List.copyOf(stacks));
    }

    private static List<Item> resolveTagItems(String tag, Map<String, List<Item>> tagCache) {
        return tagCache.computeIfAbsent(
                tag,
                key -> {
                    var items = new LinkedHashSet<Item>();
                    for (Item item : BuiltInRegistries.ITEM) {
                        if (item == Items.AIR) continue;
                        var probe = new ItemStack(item);
                        var id = BuiltInRegistries.ITEM.getKey(item).toString();
                        if (WorkshopPlatform.tagged(probe, key)
                                || WorkshopPlatform.knownTagMember(key, id)) {
                            items.add(item);
                        }
                    }
                    return List.copyOf(items);
                });
    }

    private static String displayTag(String tag) {
        if (!tag.startsWith("forge:")) return tag;
        String path = tag.substring(6);
        if (path.equals("raw_ores")) path = "raw_materials";
        else if (path.startsWith("raw_ores/")) path = "raw_materials/" + path.substring(9);
        else if (path.equals("blocks")) path = "storage_blocks";
        else if (path.startsWith("blocks/")) path = "storage_blocks/" + path.substring(7);
        else if (path.equals("sand")) path = "sands";
        else if (path.startsWith("sand/")) path = "sands/" + path.substring(5);
        else {
            path =
                    switch (path) {
                        case "obsidian" -> "obsidians";
                        case "glass" -> "glass_blocks";
                        case "stone" -> "stones";
                        case "cobblestone" -> "cobblestones";
                        case "gravel" -> "gravels";
                        case "netherrack" -> "netherracks";
                        case "sandstone" -> "sandstone/blocks";
                        case "bone" -> "bones";
                        case "gunpowder" -> "gunpowders";
                        case "shears" -> "tools/shear";
                        case "slimeball", "slimeballs" -> "slime_balls";
                        case "leather" -> "leathers";
                        case "string" -> "strings";
                        case "feather" -> "feathers";
                        case "wrenches", "tools/wrenches" -> "tools/wrench";
                        default -> path;
                    };
        }
        return "c:" + path;
    }
}
