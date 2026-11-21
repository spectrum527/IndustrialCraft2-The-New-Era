package com.industrialage.ia2.logistics;

import com.industrialage.ia2.logistics.CellCrafting.Use;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Wraps normal shaped and shapeless recipes with native cell-content checks and remainders. */
final class CellRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> REGISTRY =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, "ic2");
    private static final RecipeSerializer<CellRecipe> SHAPED =
            serializer(ShapedRecipe.class, ShapedRecipe.MAP_CODEC, ShapedRecipe.STREAM_CODEC);
    private static final RecipeSerializer<CellRecipe> SHAPELESS =
            serializer(
                    ShapelessRecipe.class, ShapelessRecipe.MAP_CODEC, ShapelessRecipe.STREAM_CODEC);

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
                                                .forGetter(CellRecipe::use),
                                        Codec.STRING
                                                .optionalFieldOf("cell_preview_fluid", "")
                                                .forGetter(CellRecipe::previewFluid))
                                .apply(instance, CellRecipe::new));
    }

    private static <T extends CraftingRecipe>
            StreamCodec<RegistryFriendlyByteBuf, CellRecipe> stream(
                    Class<T> type, StreamCodec<RegistryFriendlyByteBuf, T> vanilla) {
        return StreamCodec.of(
                (output, value) -> {
                    vanilla.encode(output, type.cast(value.recipe()));
                    output.writeEnum(value.use());
                    output.writeUtf(value.previewFluid());
                },
                input ->
                        new CellRecipe(
                                vanilla.decode(input), input.readEnum(Use.class), input.readUtf()));
    }

    private static <T extends CraftingRecipe> RecipeSerializer<CellRecipe> serializer(
            Class<T> type, MapCodec<T> vanilla, StreamCodec<RegistryFriendlyByteBuf, T> network) {
        return new RecipeSerializer<>(codec(type, vanilla), stream(type, network));
    }

    private record CellRecipe(CraftingRecipe recipe, Use use, String previewFluid)
            implements CraftingRecipe {
        @Override
        public boolean matches(CraftingInput input, Level level) {
            return recipe.matches(input, level) && CellCrafting.matches(use, input.items());
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            return CellCrafting.assemble(use, input.items(), recipe.assemble(input));
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            return CellCrafting.remainders(use, input.items(), recipe.getRemainingItems(input));
        }

        @Override
        public RecipeSerializer<CellRecipe> getSerializer() {
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
        public PlacementInfo placementInfo() {
            return recipe.placementInfo();
        }

        @Override
        public List<RecipeDisplay> display() {
            var defaults = recipe.display();
            if (use != Use.FILL || previewFluid.isEmpty()) return defaults;
            var filled = TransportPlatform.previewFilledCell(previewFluid);
            if (filled.isEmpty()) return defaults;
            var output =
                    new SlotDisplay.ItemStackSlotDisplay(
                            ItemStackTemplate.fromNonEmptyStack(filled));
            return defaults.stream()
                    .<RecipeDisplay>map(
                            display ->
                                    switch (display) {
                                        case ShapedCraftingRecipeDisplay shaped ->
                                                new ShapedCraftingRecipeDisplay(
                                                        shaped.width(),
                                                        shaped.height(),
                                                        shaped.ingredients(),
                                                        output,
                                                        shaped.craftingStation());
                                        case ShapelessCraftingRecipeDisplay shapeless ->
                                                new ShapelessCraftingRecipeDisplay(
                                                        shapeless.ingredients(),
                                                        output,
                                                        shapeless.craftingStation());
                                        default -> display;
                                    })
                    .toList();
        }

        @Override
        public RecipeBookCategory recipeBookCategory() {
            return recipe.recipeBookCategory();
        }

        @Override
        public String group() {
            return recipe.group();
        }
    }
}
