package com.industrialage.ia2.equipment;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Recipe codec and wearable recognition on the 26.x component-backed crafting API. */
final class NativeMount {
    private static final RecipeSerializer<AttachRecipe> SERIALIZER =
            new RecipeSerializer<>(
                    MapCodec.unit(AttachRecipe::new), StreamCodec.unit(new AttachRecipe()));

    private NativeMount() {}

    static RecipeSerializer<AttachRecipe> recipe() {
        return SERIALIZER;
    }

    static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    static boolean chestArmor(ItemStack stack) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null
                && equippable.slot() == EquipmentSlot.CHEST
                && !stack.has(DataComponents.GLIDER);
    }

    static ItemStack electricJetpack() {
        return new ItemStack(
                BuiltInRegistries.ITEM.getValue(
                        Identifier.fromNamespaceAndPath("ic2", "electric_jetpack")));
    }

    private static final class AttachRecipe extends CustomRecipe {
        @Override
        public boolean matches(CraftingInput input, Level level) {
            return !JetpackMount.craft(input.items()).isEmpty();
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            return JetpackMount.craft(input.items());
        }

        @Override
        public RecipeSerializer<? extends CustomRecipe> getSerializer() {
            return SERIALIZER;
        }
    }
}
