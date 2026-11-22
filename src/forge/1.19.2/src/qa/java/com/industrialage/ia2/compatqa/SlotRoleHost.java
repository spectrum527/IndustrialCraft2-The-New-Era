package com.industrialage.ia2.compatqa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

final class SlotRoleHost {
    private SlotRoleHost() {}

    static MachineSlotRoleChecks.ItemsPort item(ServerLevel level, BlockPos pos, Direction side) {
        var port =
                level.getBlockEntity(pos)
                        .getCapability(ForgeCapabilities.ITEM_HANDLER, side)
                        .orElse(null);
        if (port == null) return null;
        return new MachineSlotRoleChecks.ItemsPort() {
            @Override
            public boolean accepts(int slot, ItemStack stack) {
                return port.isItemValid(slot, stack);
            }

            @Override
            public int insert(int slot, ItemStack stack, boolean simulate) {
                return stack.getCount() - port.insertItem(slot, stack, simulate).getCount();
            }

            @Override
            public ItemStack take(int slot, int maximum, boolean simulate) {
                return port.extractItem(slot, maximum, simulate);
            }
        };
    }

    static void charge(ItemStack stack, double amount) {
        com.industrialage.ia2.forge.equipment.ForgeGear.restoreEnergy(stack, amount);
    }

    static double energy(ItemStack stack) {
        return com.industrialage.ia2.forge.equipment.ForgeGear.stored(stack);
    }

    static void wear(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt("ic2_reactor_use", value);
    }
}
