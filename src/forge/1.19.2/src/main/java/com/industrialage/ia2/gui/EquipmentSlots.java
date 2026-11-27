package com.industrialage.ia2.gui;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Native equipment selection and removal rules shared by machine-side worn slots. */
public final class EquipmentSlots {
    private EquipmentSlots() {}

    /** Slot that points directly to a worn item and honors the player's equipment restrictions. */
    public static Slot worn(Inventory player, EquipmentSlot armor, int x, int y) {
        return new Slot(player, armor.getIndex(36), x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.canEquip(armor, player.player);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPickup(Player wearer) {
                return wearer.isCreative() || !EnchantmentHelper.hasBindingCurse(getItem());
            }
        };
    }
}
