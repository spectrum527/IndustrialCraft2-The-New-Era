package com.industrialage.ia2.equipment;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

/** Recipe codec and wearable recognition on the 1.21.1 crafting API. */
final class NativeMount {
    private NativeMount() {}

    static RecipeSerializer<?> recipe() {
        return new SimpleCraftingRecipeSerializer<>(AttachRecipe::new);
    }

    static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    static boolean chestArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armor
                && armor.getEquipmentSlot() == EquipmentSlot.CHEST;
    }

    static ItemStack electricJetpack() {
        return new ItemStack(
                BuiltInRegistries.ITEM.get(
                        ResourceLocation.fromNamespaceAndPath("ic2", "electric_jetpack")));
    }

    private static final class AttachRecipe extends CustomRecipe {
        AttachRecipe(CraftingBookCategory category) {
            super(category);
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return !JetpackMount.craft(input.items()).isEmpty();
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
            return JetpackMount.craft(input.items());
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= 3;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return MountPlatform.serializer();
        }
    }
}
