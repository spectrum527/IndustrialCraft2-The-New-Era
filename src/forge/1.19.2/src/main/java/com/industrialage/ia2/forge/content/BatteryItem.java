package com.industrialage.ia2.forge.content;

import com.industrialage.ia2.content.PortableEquipment;
import com.industrialage.ia2.forge.equipment.ForgeGear;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

final class BatteryItem extends ForgeGear.PoweredItem {
    static final double CAPACITY = PortableEquipment.RE_BATTERY.capacity;

    BatteryItem() {
        super(
                PortableEquipment.RE_BATTERY,
                ForgeContent.itemProperties()
                        .stacksTo(PortableEquipment.RE_BATTERY.emptyStackSize()));
    }

    static double energy(ItemStack stack) {
        return ForgeGear.stored(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.ic2.energy", (int) energy(stack), (int) CAPACITY));
    }
}
