package com.industrialage.ia2.compat;

import com.industrialage.ia2.content.ProcessingMachine;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shared recipe catalog for IC2's 12 machine processing categories, consumed by both JEI and EMI
 * plugins across NeoForge 1.21.1, 26.1.2, and 26.2 without importing any viewer API.
 */
final class Ic2RecipeCatalog {
    record CategorySpec(
            String id, String iconItemId, List<String> catalystItemIds, boolean processing) {}

    record MachineRecipe(String id, List<List<ItemStack>> inputs, List<ItemStack> outputs) {}

    static final List<CategorySpec> CATEGORIES =
            List.of(
                    new CategorySpec("macerator", "ic2:macerator", List.of("ic2:macerator"), true),
                    new CategorySpec("extractor", "ic2:extractor", List.of("ic2:extractor"), true),
                    new CategorySpec(
                            "compressor", "ic2:compressor", List.of("ic2:compressor"), true),
                    new CategorySpec(
                            "metal_former_rolling",
                            "ic2:metal_former",
                            List.of("ic2:metal_former"),
                            false),
                    new CategorySpec(
                            "metal_former_extruding",
                            "ic2:metal_former",
                            List.of("ic2:metal_former"),
                            false),
                    new CategorySpec(
                            "metal_former_cutting",
                            "ic2:metal_former",
                            List.of("ic2:metal_former"),
                            false),
                    new CategorySpec(
                            "ore_washer",
                            "ic2:ore_washing_plant",
                            List.of("ic2:ore_washing_plant"),
                            false),
                    new CategorySpec(
                            "centrifuge", "ic2:centrifuge", List.of("ic2:centrifuge"), false),
                    new CategorySpec(
                            "blast_furnace",
                            "ic2:blast_furnace",
                            List.of("ic2:blast_furnace"),
                            false),
                    new CategorySpec(
                            "block_cutter", "ic2:block_cutter", List.of("ic2:block_cutter"), false),
                    new CategorySpec(
                            "canner_bottle",
                            "ic2:canner",
                            List.of("ic2:canner", "ic2:solid_canner", "ic2:classic_canner"),
                            false),
                    new CategorySpec("canner_enrich", "ic2:canner", List.of("ic2:canner"), false));

    private Ic2RecipeCatalog() {}

    static List<MachineRecipe> recipesFor(CategorySpec category) {
        var result = new ArrayList<MachineRecipe>();
        if (category.processing()) {
            int idx = 0;
            for (var recipe : ProcessingMachine.allRecipes(category.id())) {
                var inputStacks = resolveInput(recipe.input(), recipe.inputCount());
                var output = WorkshopPlatform.itemStack(recipe.output(), recipe.outputCount());
                if (!inputStacks.isEmpty() && !output.isEmpty()) {
                    result.add(
                            new MachineRecipe(
                                    category.id() + "_" + (idx++),
                                    List.of(inputStacks),
                                    List.of(output)));
                }
            }
            return result;
        }
        int idx = 0;
        for (var recipe : WorkshopPlatform.workshopRecipes(category.id())) {
            var inputs = new ArrayList<List<ItemStack>>();
            boolean valid = true;
            for (var in : recipe.inputs()) {
                var resolved = resolveInput(in.id(), in.count());
                if (resolved.isEmpty()) {
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
            if (!outputs.isEmpty()) {
                result.add(new MachineRecipe(category.id() + "_" + (idx++), inputs, outputs));
            }
        }
        return result;
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

    private static List<ItemStack> resolveInput(String spec, int count) {
        if (!spec.startsWith("#")) {
            var stack = WorkshopPlatform.itemStack(spec, count);
            return stack.isEmpty() ? List.of() : List.of(stack);
        }
        String tag = spec.substring(1);
        var items = new LinkedHashSet<Item>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            var probe = new ItemStack(item);
            var key = BuiltInRegistries.ITEM.getKey(item).toString();
            if (WorkshopPlatform.tagged(probe, tag) || WorkshopPlatform.knownTagMember(tag, key)) {
                items.add(item);
            }
        }
        var result = new ArrayList<ItemStack>(items.size());
        for (Item item : items) {
            result.add(new ItemStack(item, count));
        }
        return result;
    }
}
