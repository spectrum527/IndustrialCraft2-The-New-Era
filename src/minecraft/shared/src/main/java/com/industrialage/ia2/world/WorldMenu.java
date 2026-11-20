package com.industrialage.ia2.world;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class WorldMenu extends ChestMenu {
    private final int deviceSlots;

    WorldMenu(int id, Inventory playerInventory, WorldInventory device) {
        super(
                rows(device.getContainerSize()),
                id,
                playerInventory,
                device,
                device.getContainerSize() / 9);
        deviceSlots = device.getContainerSize();
        for (int index = 0; index < device.getContainerSize(); index++) {
            var position = slots.get(index);
            final int deviceSlot = index;
            var restricted =
                    new Slot(device, index, position.x, position.y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return device.canPlaceItem(deviceSlot, stack);
                        }
                    };
            restricted.index = index;
            slots.set(index, restricted);
        }
    }

    private static MenuType<ChestMenu> rows(int size) {
        return switch (size) {
            case 9 -> MenuType.GENERIC_9x1;
            case 18 -> MenuType.GENERIC_9x2;
            default -> MenuType.GENERIC_9x3;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var moved = stack.copy();
        boolean success =
                index < deviceSlots
                        ? moveItemStackTo(stack, deviceSlots, slots.size(), true)
                        : moveItemStackTo(stack, 0, deviceSlots, false);
        if (!success) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return moved;
    }
}
