package com.industrialage.ia2.compat;

import com.industrialage.ia2.compat.Ic2RecipeCatalog.CategorySpec;
import com.industrialage.ia2.compat.Ic2RecipeCatalog.MachineRecipe;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.LinkedHashMap;
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
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Forge 1.19.2 JEI plugin exposing all 12 IC2 machine recipe categories and furnace catalysts. */
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
        for (var spec : Ic2RecipeCatalog.CATEGORIES) {
            registration.addRecipes(TYPES.get(spec.id()), Ic2RecipeCatalog.recipesFor(spec));
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
        private static final int WIDTH = 152;
        private static final int HEIGHT = 42;
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
            return Component.translatable("jei.ic2.category." + spec.id());
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
            int inX = 8;
            for (var inputChoices : recipe.inputs()) {
                builder.addSlot(RecipeIngredientRole.INPUT, inX, 12)
                        .setBackground(slotBg, -1, -1)
                        .addItemStacks(inputChoices);
                inX += 20;
            }
            int outX = Math.max(76, inX + 26);
            for (var output : recipe.outputs()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, outX, 12)
                        .setBackground(slotBg, -1, -1)
                        .addItemStack(output);
                outX += 20;
            }
        }

        @Override
        public void draw(
                MachineRecipe recipe,
                IRecipeSlotsView slotsView,
                PoseStack poseStack,
                double mouseX,
                double mouseY) {
            int arrowX = Math.max(46, 8 + recipe.inputs().size() * 20 + 6);
            Minecraft.getInstance().font.draw(poseStack, "->", arrowX, 16, 0xFF404040);
        }
    }
}
