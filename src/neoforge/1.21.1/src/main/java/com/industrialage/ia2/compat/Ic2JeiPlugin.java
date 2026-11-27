package com.industrialage.ia2.compat;

import com.industrialage.ia2.compat.Ic2RecipeCatalog.CategorySpec;
import com.industrialage.ia2.compat.Ic2RecipeCatalog.MachineRecipe;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * NeoForge 1.21.1 JEI plugin exposing all 18 IC2 machine recipe categories and furnace catalysts.
 */
@JeiPlugin
public final class Ic2JeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("ic2", "jei_plugin");
    private static final Map<String, RecipeType<MachineRecipe>> TYPES = createTypes();

    private static Map<String, RecipeType<MachineRecipe>> createTypes() {
        var map = new LinkedHashMap<String, RecipeType<MachineRecipe>>();
        for (var spec : Ic2RecipeCatalog.CATEGORIES) {
            map.put(spec.id(), RecipeType.create("ic2", spec.id(), MachineRecipe.class));
        }
        return Map.copyOf(map);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (var spec : Ic2RecipeCatalog.CATEGORIES) {
            registration.addRecipeCategories(new MachineCategory(gui, spec, TYPES.get(spec.id())));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var all = Ic2RecipeCatalog.buildAll();
        for (var spec : Ic2RecipeCatalog.CATEGORIES) {
            registration.addRecipes(TYPES.get(spec.id()), all.getOrDefault(spec, List.of()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (var spec : Ic2RecipeCatalog.CATEGORIES) {
            var type = TYPES.get(spec.id());
            for (ItemStack catalyst : Ic2RecipeCatalog.catalysts(spec)) {
                registration.addRecipeCatalyst(catalyst, type);
            }
        }
        for (String furnace :
                new String[] {
                    "ic2:iron_furnace", "ic2:electric_furnace", "ic2:induction_furnace"
                }) {
            var stack = WorkshopPlatform.itemStack(furnace, 1);
            if (!stack.isEmpty()) {
                registration.addRecipeCatalyst(stack, RecipeTypes.SMELTING);
            }
        }
    }

    private static final class MachineCategory implements IRecipeCategory<MachineRecipe> {
        private static final int WIDTH = 166;
        private static final int HEIGHT = 44;
        private final CategorySpec spec;
        private final RecipeType<MachineRecipe> type;
        private final IDrawable icon;
        private final IDrawableStatic slotBg;

        MachineCategory(IGuiHelper gui, CategorySpec spec, RecipeType<MachineRecipe> type) {
            this.spec = spec;
            this.type = type;
            this.icon = gui.createDrawableItemStack(Ic2RecipeCatalog.icon(spec));
            this.slotBg = gui.getSlotDrawable();
        }

        @Override
        public RecipeType<MachineRecipe> getRecipeType() {
            return type;
        }

        @Override
        public Component getTitle() {
            return Component.translatable(spec.titleKey());
        }

        @Override
        public int getWidth() {
            return WIDTH;
        }

        @Override
        public int getHeight() {
            return HEIGHT;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(
                IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
            int inX = 6;
            for (var fluidIn : recipe.fluidInputs()) {
                var fluid = WorkshopPlatform.fluid(fluidIn.fluidId(), fluidIn.amountMb());
                if (!fluid.isEmpty()) {
                    builder.addSlot(RecipeIngredientRole.INPUT, inX, 12)
                            .setBackground(slotBg, -1, -1)
                            .setFluidRenderer(Math.max(1000L, fluidIn.amountMb()), false, 16, 16)
                            .addFluidStack(fluid.getFluid(), fluidIn.amountMb());
                    inX += 20;
                }
            }
            for (var input : recipe.itemInputs()) {
                var slot =
                        builder.addSlot(RecipeIngredientRole.INPUT, inX, 12)
                                .setBackground(slotBg, -1, -1)
                                .addItemStacks(input.stacks());
                if (input.isTag()) {
                    String label = "#" + input.tag();
                    slot.addRichTooltipCallback(
                            (view, tooltip) ->
                                    tooltip.add(
                                            Component.literal(label)
                                                    .withStyle(ChatFormatting.GRAY)));
                }
                inX += 20;
            }
            int arrowX = Math.max(46, inX + 6);
            int outX = Math.max(76, arrowX + 22);
            for (var output : recipe.itemOutputs()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, outX, 12)
                        .setBackground(slotBg, -1, -1)
                        .addItemStack(output);
                outX += 20;
            }
            for (var fluidOut : recipe.fluidOutputs()) {
                var fluid = WorkshopPlatform.fluid(fluidOut.fluidId(), fluidOut.amountMb());
                if (!fluid.isEmpty()) {
                    builder.addSlot(RecipeIngredientRole.OUTPUT, outX, 12)
                            .setBackground(slotBg, -1, -1)
                            .setFluidRenderer(Math.max(1000L, fluidOut.amountMb()), false, 16, 16)
                            .addFluidStack(fluid.getFluid(), fluidOut.amountMb());
                    outX += 20;
                }
            }
        }

        @Override
        public void draw(
                MachineRecipe recipe,
                IRecipeSlotsView slotsView,
                GuiGraphics graphics,
                double mouseX,
                double mouseY) {
            int arrowX = Math.max(46, 6 + recipe.inputSlots() * 20 + 6);
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, "->", arrowX, 16, 0xFF404040, false);
            if (!recipe.note().isEmpty()) {
                graphics.drawString(
                        font, recipe.note(), Math.max(4, arrowX - 8), 32, 0xFF606060, false);
            }
        }
    }
}
