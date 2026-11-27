package com.industrialage.ia2.forge.content;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

final class CraftingTool extends Item {
    CraftingTool(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        if (stack.getDamageValue() + 1 >= stack.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        var remainder = stack.copy();
        remainder.setDamageValue(stack.getDamageValue() + 1);
        return remainder;
    }

    static final ArmorMaterial BRONZE =
            new ArmorMaterial() {
                @Override
                public int getDurabilityForSlot(EquipmentSlot slot) {
                    return switch (slot) {
                        case FEET -> 195;
                        case LEGS -> 225;
                        case CHEST -> 240;
                        case HEAD -> 165;
                        default -> 0;
                    };
                }

                @Override
                public int getDefenseForSlot(EquipmentSlot slot) {
                    return switch (slot) {
                        case FEET, HEAD -> 2;
                        case LEGS -> 5;
                        case CHEST -> 6;
                        default -> 0;
                    };
                }

                @Override
                public int getEnchantmentValue() {
                    return 9;
                }

                @Override
                public SoundEvent getEquipSound() {
                    return SoundEvents.ARMOR_EQUIP_IRON;
                }

                @Override
                public Ingredient getRepairIngredient() {
                    return Ingredient.of(
                            ForgeRegistries.ITEMS.getValue(ForgeContent.id("bronze_ingot")));
                }

                @Override
                public String getName() {
                    return "ic2:bronze";
                }

                @Override
                public float getToughness() {
                    return 0;
                }

                @Override
                public float getKnockbackResistance() {
                    return 0;
                }
            };
}
