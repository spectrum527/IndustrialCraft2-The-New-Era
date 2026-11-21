package com.industrialage.ia2.logistics;

import com.industrialage.ia2.logistics.CellCrafting.Use;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Wraps normal shaped and shapeless recipes with native cell-content checks and remainders. */
final class CellRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> REGISTRY =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, "ic2");
    private static final RecipeSerializer<CellRecipe> SHAPED =
            new Serializer<>(ShapedRecipe.class, RecipeSerializer.SHAPED_RECIPE);
    private static final RecipeSerializer<CellRecipe> SHAPELESS =
            new Serializer<>(ShapelessRecipe.class, RecipeSerializer.SHAPELESS_RECIPE);

    private CellRecipes() {}

    static void install(IEventBus bus) {
        REGISTRY.register("cell_shaped", () -> SHAPED);
        REGISTRY.register("cell_shapeless", () -> SHAPELESS);
        REGISTRY.register(bus);
    }

    private static <T extends CraftingRecipe> MapCodec<CellRecipe> codec(
            Class<T> type, MapCodec<T> vanilla) {
        return RecordCodecBuilder.mapCodec(
                instance ->
                        instance.group(
                                        vanilla.forGetter(value -> type.cast(value.recipe())),
                                        Codec.STRING
                                                .xmap(Use::parse, Use::key)
                                                .fieldOf("cell_use")
                                                .forGetter(CellRecipe::use))
                                .apply(instance, CellRecipe::new));
    }

    private static <T extends CraftingRecipe>
            StreamCodec<RegistryFriendlyByteBuf, CellRecipe> stream(
                    Class<T> type, StreamCodec<RegistryFriendlyByteBuf, T> vanilla) {
        return StreamCodec.of(
                (output, value) -> {
                    vanilla.encode(output, type.cast(value.recipe()));
                    output.writeEnum(value.use());
                },
                input -> new CellRecipe(vanilla.decode(input), input.readEnum(Use.class)));
    }

    private record Serializer<T extends CraftingRecipe>(Class<T> type, RecipeSerializer<T> vanilla)
            implements RecipeSerializer<CellRecipe> {
        @Override
        public MapCodec<CellRecipe> codec() {
            return CellRecipes.codec(type, vanilla.codec());
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CellRecipe> streamCodec() {
            return stream(type, vanilla.streamCodec());
        }
    }

    private record CellRecipe(CraftingRecipe recipe, Use use) implements CraftingRecipe {
        @Override
        public boolean matches(CraftingInput input, Level level) {
            return recipe.matches(input, level) && CellCrafting.matches(use, input.items());
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
            return CellCrafting.assemble(use, input.items(), recipe.assemble(input, lookup));
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            return CellCrafting.remainders(use, input.items(), recipe.getRemainingItems(input));
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return recipe instanceof ShapedRecipe ? SHAPED : SHAPELESS;
        }

        @Override
        public CraftingBookCategory category() {
            return recipe.category();
        }

        @Override
        public boolean showNotification() {
            return recipe.showNotification();
        }

        @Override
        public NonNullList<Ingredient> getIngredients() {
            return recipe.getIngredients();
        }

        @Override
        public ItemStack getResultItem(HolderLookup.Provider lookup) {
            return recipe.getResultItem(lookup);
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return recipe.canCraftInDimensions(width, height);
        }

        @Override
        public String getGroup() {
            return recipe.getGroup();
        }
    }
}
