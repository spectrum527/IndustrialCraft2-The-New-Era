package com.industrialage.ia2.compat;

import com.industrialage.ia2.compat.Ic2RecipeCatalog.CategorySpec;
import com.industrialage.ia2.compat.Ic2RecipeCatalog.MachineRecipe;
import java.util.LinkedHashMap;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** NeoForge 26.x JEI plugin exposing all 12 IC2 machine recipe categories and furnace catalysts. */
@JeiPlugin
public final class Ic2JeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath("ic2", "jei_plugin");
    private static final Map<String, IRecipeType<MachineRecipe>> TYPES = createTypes();

    private static Map<String, IRecipeType<MachineRecipe>> createTypes() {
        var map = new LinkedHashMap<String, IRecipeType<MachineRecipe>>();
        for (var spec : Ic2RecipeCatalog.CATEGORIES) {
            map.put(spec.id(), IRecipeType.create("ic2", spec.id(), MachineRecipe.class));
        }
        return Map.copyOf(map);
    }

    @Override
    public Identifier getPluginUid() {
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
                registration.addCraftingStation(type, catalyst);
            }
        }
        for (String furnace :
                new String[] {
                    "ic2:iron_furnace", "ic2:electric_furnace", "ic2:induction_furnace"
                }) {
            var stack = com.industrialage.ia2.industrial.WorkshopPlatform.itemStack(furnace, 1);
            if (!stack.isEmpty()) {
                registration.addCraftingStation(RecipeTypes.SMELTING, stack);
            }
        }
    }

    private static final class MachineCategory implements IRecipeCategory<MachineRecipe> {
        private static final int WIDTH = 152;
        private static final int HEIGHT = 42;
        private final CategorySpec spec;
        private final IRecipeType<MachineRecipe> type;
        private final IDrawable icon;

        MachineCategory(IGuiHelper gui, CategorySpec spec, IRecipeType<MachineRecipe> type) {
            this.spec = spec;
            this.type = type;
            this.icon = gui.createDrawableItemStack(Ic2RecipeCatalog.icon(spec));
        }

        @Override
        public IRecipeType<MachineRecipe> getRecipeType() {
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
                        .setStandardSlotBackground()
                        .addItemStacks(inputChoices);
                inX += 20;
            }
            int outX = Math.max(76, inX + 26);
            for (var output : recipe.outputs()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, outX, 12)
                        .setStandardSlotBackground()
                        .add(output);
                outX += 20;
            }
        }

        @Override
        public void draw(
                MachineRecipe recipe,
                IRecipeSlotsView slotsView,
                GuiGraphicsExtractor graphics,
                double mouseX,
                double mouseY) {
            int arrowX = Math.max(46, 8 + recipe.inputs().size() * 20 + 6);
            graphics.text(Minecraft.getInstance().font, "->", arrowX, 16, 0xFF404040, false);
        }
    }
}
