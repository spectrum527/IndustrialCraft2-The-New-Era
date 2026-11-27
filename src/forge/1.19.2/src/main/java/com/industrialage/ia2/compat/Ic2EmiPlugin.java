package com.industrialage.ia2.compat;

import com.industrialage.ia2.compat.Ic2RecipeCatalog.MachineRecipe;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Forge 1.19.2 EMI plugin exposing all 12 IC2 machine recipe categories and furnace stations. */
@EmiEntrypoint
public final class Ic2EmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        for (var spec : Ic2RecipeCatalog.CATEGORIES) {
            var category =
                    new EmiRecipeCategory(
                            ResourceLocation.fromNamespaceAndPath("ic2", spec.id()),
                            EmiStack.of(Ic2RecipeCatalog.icon(spec)));
            registry.addCategory(category);
            for (ItemStack catalyst : Ic2RecipeCatalog.catalysts(spec)) {
                registry.addWorkstation(category, EmiStack.of(catalyst));
            }
            for (MachineRecipe recipe : Ic2RecipeCatalog.recipesFor(spec)) {
                registry.addRecipe(new Ic2MachineEmiRecipe(category, recipe));
            }
        }
        for (String furnace :
                new String[] {
                    "ic2:iron_furnace", "ic2:electric_furnace", "ic2:induction_furnace"
                }) {
            var stack = WorkshopPlatform.itemStack(furnace, 1);
            if (!stack.isEmpty()) {
                registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(stack));
            }
        }
    }

    private static EmiIngredient toIngredient(List<ItemStack> choices) {
        if (choices.size() == 1) return EmiStack.of(choices.get(0));
        return EmiIngredient.of(choices.stream().map(EmiStack::of).toList());
    }

    private static final class Ic2MachineEmiRecipe extends BasicEmiRecipe {
        Ic2MachineEmiRecipe(EmiRecipeCategory category, MachineRecipe recipe) {
            super(
                    category,
                    ResourceLocation.fromNamespaceAndPath(
                            "ic2", "/" + recipe.id().replaceAll("[^a-z0-9/._-]", "_")),
                    Math.max(146, 36 + recipe.inputs().size() * 20 + recipe.outputs().size() * 20),
                    34);
            for (var in : recipe.inputs()) {
                this.inputs.add(toIngredient(in));
            }
            for (var out : recipe.outputs()) {
                this.outputs.add(EmiStack.of(out));
            }
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            int x = 6;
            for (var in : inputs) {
                widgets.addSlot(in, x, 8);
                x += 20;
            }
            widgets.addFillingArrow(x + 4, 8, 3000);
            int outX = x + 34;
            for (var out : outputs) {
                widgets.addSlot(out, outX, 8).recipeContext(this);
                outX += 20;
            }
        }
    }
}
