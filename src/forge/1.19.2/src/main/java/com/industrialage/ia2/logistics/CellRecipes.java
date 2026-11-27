package com.industrialage.ia2.logistics;

import com.google.gson.JsonObject;
import com.industrialage.ia2.logistics.CellCrafting.Use;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Adds cell rules to the normal Forge crafting, recipe book and network paths. */
final class CellRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "ic2");
    private static final Serializer<ShapedRecipe> SHAPED =
            new Serializer<>(RecipeSerializer.SHAPED_RECIPE, ShapedRecipe.class);
    private static final Serializer<net.minecraft.world.item.crafting.ShapelessRecipe> SHAPELESS =
            new Serializer<>(
                    RecipeSerializer.SHAPELESS_RECIPE,
                    net.minecraft.world.item.crafting.ShapelessRecipe.class);

    private CellRecipes() {}

    static void install(IEventBus bus) {
        REGISTRY.register("cell_shaped", () -> SHAPED);
        REGISTRY.register("cell_shapeless", () -> SHAPELESS);
        REGISTRY.register(bus);
    }

    private static class CellRecipe implements CraftingRecipe {
        final CraftingRecipe recipe;
        final Use use;

        CellRecipe(CraftingRecipe recipe, Use use) {
            this.recipe = recipe;
            this.use = use;
        }

        @Override
        public boolean matches(CraftingContainer input, Level level) {
            return recipe.matches(input, level) && CellCrafting.matches(use, items(input));
        }

        @Override
        public ItemStack assemble(CraftingContainer input) {
            return CellCrafting.assemble(use, items(input), recipe.assemble(input));
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
            return CellCrafting.remainders(use, items(input), recipe.getRemainingItems(input));
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return recipe.canCraftInDimensions(width, height);
        }

        @Override
        public NonNullList<Ingredient> getIngredients() {
            return recipe.getIngredients();
        }

        @Override
        public ItemStack getResultItem() {
            return recipe.getResultItem();
        }

        @Override
        public String getGroup() {
            return recipe.getGroup();
        }

        @Override
        public ResourceLocation getId() {
            return recipe.getId();
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return recipe instanceof ShapedRecipe ? SHAPED : SHAPELESS;
        }

        private static List<ItemStack> items(CraftingContainer input) {
            var items = new ArrayList<ItemStack>(input.getContainerSize());
            for (int slot = 0; slot < input.getContainerSize(); slot++)
                items.add(input.getItem(slot));
            return items;
        }
    }

    private static final class ShapedCellRecipe extends CellRecipe
            implements IShapedRecipe<CraftingContainer> {
        private final ShapedRecipe shaped;

        ShapedCellRecipe(ShapedRecipe shaped, Use use) {
            super(shaped, use);
            this.shaped = shaped;
        }

        @Override
        public int getRecipeWidth() {
            return shaped.getWidth();
        }

        @Override
        public int getRecipeHeight() {
            return shaped.getHeight();
        }
    }

    private record Serializer<T extends CraftingRecipe>(RecipeSerializer<T> vanilla, Class<T> type)
            implements RecipeSerializer<CellRecipe> {
        @Override
        public CellRecipe fromJson(ResourceLocation id, JsonObject data) {
            return wrap(
                    vanilla.fromJson(id, data),
                    Use.parse(GsonHelper.getAsString(data, "cell_use")));
        }

        @Override
        public CellRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf input) {
            return wrap(vanilla.fromNetwork(id, input), input.readEnum(Use.class));
        }

        @Override
        public void toNetwork(FriendlyByteBuf output, CellRecipe recipe) {
            vanilla.toNetwork(output, type.cast(recipe.recipe));
            output.writeEnum(recipe.use);
        }

        private CellRecipe wrap(T recipe, Use use) {
            return recipe instanceof ShapedRecipe shaped
                    ? new ShapedCellRecipe(shaped, use)
                    : new CellRecipe(recipe, use);
        }
    }
}
