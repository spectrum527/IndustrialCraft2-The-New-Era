package com.industrialage.ia2.compat;

import com.industrialage.ia2.compat.Ic2RecipeCatalog.CategorySpec;
import com.industrialage.ia2.compat.Ic2RecipeCatalog.FluidAmount;
import com.industrialage.ia2.compat.Ic2RecipeCatalog.ItemInput;
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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/**
 * NeoForge 1.21.1 EMI plugin exposing all 18 IC2 machine recipe categories and furnace stations.
 */
@EmiEntrypoint
public final class Ic2EmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        var all = Ic2RecipeCatalog.buildAll();
        for (var spec : Ic2RecipeCatalog.CATEGORIES) {
            var category = createCategory(spec);
            registry.addCategory(category);
            for (ItemStack catalyst : Ic2RecipeCatalog.catalysts(spec)) {
                registry.addWorkstation(category, EmiStack.of(catalyst));
            }
            for (MachineRecipe recipe : all.getOrDefault(spec, List.of())) {
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

    private static EmiRecipeCategory createCategory(CategorySpec spec) {
        var id = ResourceLocation.fromNamespaceAndPath("ic2", spec.id());
        var icon = EmiStack.of(Ic2RecipeCatalog.icon(spec));
        if (!spec.blockTitle()) {
            return new EmiRecipeCategory(id, icon);
        }
        return new EmiRecipeCategory(id, icon) {
            @Override
            public Component getName() {
                return Component.translatable(spec.titleKey());
            }
        };
    }

    private record InputSlot(EmiIngredient ingredient, String fallbackTag) {}

    private record FluidSlot(EmiStack stack, int capacity) {}

    private static InputSlot toInputSlot(ItemInput input) {
        if (!input.isTag()) {
            return new InputSlot(EmiStack.of(input.stacks().get(0)), null);
        }
        var loc = ResourceLocation.tryParse(input.tag());
        if (loc != null) {
            var tagIngredient =
                    EmiIngredient.of(TagKey.create(Registries.ITEM, loc), input.count());
            if (!tagIngredient.isEmpty()
                    && tagIngredient.getEmiStacks().size() >= input.stacks().size()) {
                return new InputSlot(tagIngredient, null);
            }
        }
        var choices = input.stacks().stream().map(EmiStack::of).toList();
        return new InputSlot(EmiIngredient.of(choices, input.count()), input.tag());
    }

    private static FluidSlot toFluidSlot(FluidAmount amount) {
        var fluid = WorkshopPlatform.fluid(amount.fluidId(), amount.amountMb());
        if (fluid.isEmpty()) return null;
        return new FluidSlot(
                EmiStack.of(fluid.getFluid(), amount.amountMb()),
                Math.max(1000, amount.amountMb()));
    }

    private static final class Ic2MachineEmiRecipe extends BasicEmiRecipe {
        private final List<InputSlot> itemSlots = new ArrayList<>();
        private final List<FluidSlot> fluidInputSlots = new ArrayList<>();
        private final List<EmiStack> itemOutputSlots = new ArrayList<>();
        private final List<FluidSlot> fluidOutputSlots = new ArrayList<>();
        private final String note;

        Ic2MachineEmiRecipe(EmiRecipeCategory category, MachineRecipe recipe) {
            super(
                    category,
                    ResourceLocation.fromNamespaceAndPath(
                            "ic2", "/" + recipe.id().replaceAll("[^a-z0-9/._-]", "_")),
                    Math.max(154, 38 + recipe.inputSlots() * 20 + recipe.outputSlots() * 20),
                    recipe.note().isEmpty() ? 34 : 42);
            this.note = recipe.note();
            for (var fluidIn : recipe.fluidInputs()) {
                var slot = toFluidSlot(fluidIn);
                if (slot != null) {
                    fluidInputSlots.add(slot);
                    this.inputs.add(slot.stack());
                }
            }
            for (var in : recipe.itemInputs()) {
                var slot = toInputSlot(in);
                itemSlots.add(slot);
                this.inputs.add(slot.ingredient());
            }
            for (var out : recipe.itemOutputs()) {
                var stack = EmiStack.of(out);
                itemOutputSlots.add(stack);
                this.outputs.add(stack);
            }
            for (var fluidOut : recipe.fluidOutputs()) {
                var slot = toFluidSlot(fluidOut);
                if (slot != null) {
                    fluidOutputSlots.add(slot);
                    this.outputs.add(slot.stack());
                }
            }
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            int x = 6;
            for (var fluidIn : fluidInputSlots) {
                widgets.addTank(fluidIn.stack(), x, 8, 18, 18, fluidIn.capacity());
                x += 20;
            }
            for (var in : itemSlots) {
                var widget = widgets.addSlot(in.ingredient(), x, 8);
                if (in.fallbackTag() != null) {
                    widget.appendTooltip(
                            Component.literal("#" + in.fallbackTag())
                                    .withStyle(ChatFormatting.GRAY));
                }
                x += 20;
            }
            widgets.addFillingArrow(x + 4, 8, 3000);
            if (!note.isEmpty()) {
                widgets.addText(Component.literal(note), Math.max(4, x), 30, 0xFF606060, false);
            }
            int outX = x + 34;
            for (var out : itemOutputSlots) {
                widgets.addSlot(out, outX, 8).recipeContext(this);
                outX += 20;
            }
            for (var fluidOut : fluidOutputSlots) {
                widgets.addTank(fluidOut.stack(), outX, 8, 18, 18, fluidOut.capacity())
                        .recipeContext(this);
                outX += 20;
            }
        }
    }
}
