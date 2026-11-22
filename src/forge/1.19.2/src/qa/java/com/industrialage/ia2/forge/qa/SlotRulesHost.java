package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.slottest.SlotRulesChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

final class SlotRulesHost implements SlotRulesChecks.Host {
    @Override
    public SlotRulesChecks.Port port(ServerLevel level, BlockPos pos, Direction side) {
        var handler =
                level.getBlockEntity(pos)
                        .getCapability(ForgeCapabilities.ITEM_HANDLER, side)
                        .orElse(null);
        if (handler == null) return null;
        return new SlotRulesChecks.Port() {
            @Override
            public int offer(ItemStack stack, boolean simulate) {
                var left = stack.copy();
                for (int slot = 0; slot < handler.getSlots() && !left.isEmpty(); slot++) {
                    left = handler.insertItem(slot, left, simulate);
                }
                return stack.getCount() - left.getCount();
            }

            @Override
            public int offerAt(int slot, ItemStack stack, boolean simulate) {
                return stack.getCount() - handler.insertItem(slot, stack, simulate).getCount();
            }

            @Override
            public ItemStack take(boolean simulate) {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    var product = handler.extractItem(slot, 64, simulate);
                    if (!product.isEmpty()) return product;
                }
                return ItemStack.EMPTY;
            }
        };
    }

    @Override
    public void charge(ItemStack battery, double eu) {
        com.industrialage.ia2.forge.equipment.ForgeGear.restoreEnergy(battery, eu);
    }
}
