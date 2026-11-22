package com.industrialage.ia2.compatqa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class SlotRoleHost {
    private SlotRoleHost() {}

    static MachineSlotRoleChecks.ItemsPort item(ServerLevel level, BlockPos pos, Direction side) {
        var port = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (port == null) return null;
        return new MachineSlotRoleChecks.ItemsPort() {
            @Override
            public boolean accepts(int slot, ItemStack stack) {
                return port.isValid(slot, ItemResource.of(stack));
            }

            @Override
            public int insert(int slot, ItemStack stack, boolean simulate) {
                try (var tx = Transaction.openRoot()) {
                    int moved = port.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
                    if (!simulate) tx.commit();
                    return moved;
                }
            }

            @Override
            public ItemStack take(int slot, int maximum, boolean simulate) {
                var item = port.getResource(slot);
                if (item.isEmpty()) return ItemStack.EMPTY;
                try (var tx = Transaction.openRoot()) {
                    int moved = port.extract(slot, item, maximum, tx);
                    if (!simulate) tx.commit();
                    return moved == 0 ? ItemStack.EMPTY : item.toStack(moved);
                }
            }
        };
    }

    static void charge(ItemStack stack, double amount) {
        com.industrialage.ia2.gameplay.PortableGear.restoreEnergy(stack, amount);
    }

    static double energy(ItemStack stack) {
        return com.industrialage.ia2.gameplay.PortableGear.stored(stack);
    }

    @SuppressWarnings("unchecked")
    static void wear(ItemStack stack, int value) {
        var type =
                BuiltInRegistries.DATA_COMPONENT_TYPE.stream()
                        .filter(
                                key ->
                                        BuiltInRegistries.DATA_COMPONENT_TYPE
                                                .getKey(key)
                                                .toString()
                                                .equals("ic2:reactor_use"))
                        .findFirst()
                        .orElseThrow();
        stack.set((DataComponentType<Integer>) type, value);
    }
}
