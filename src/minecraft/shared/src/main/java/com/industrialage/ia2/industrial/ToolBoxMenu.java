package com.industrialage.ia2.industrial;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The portable box stays in its owner's hand while its persistent contents are open. */
final class ToolBoxMenu extends AbstractContainerMenu {
    final WorkshopToolBox.Kind kind;
    private final Container contents;
    private final ItemStack ownerStack;

    ToolBoxMenu(
            MenuType<ToolBoxMenu> type,
            int id,
            Inventory player,
            WorkshopToolBox.Kind kind,
            Container saved,
            ItemStack ownerStack) {
        super(type, id);
        this.kind = kind;
        this.ownerStack = ownerStack;
        contents = saved == null ? new SimpleContainer(kind.capacity) : saved;
        for (int i = 0; i < kind.capacity; i++) {
            int x = kind == WorkshopToolBox.Kind.TOOLS ? 8 + 18 * i : 53 + 18 * (i % 4);
            int y = kind == WorkshopToolBox.Kind.TOOLS ? 41 : 19 + 18 * (i / 4);
            addSlot(
                    new Slot(contents, i, x, y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return kind.accepts(stack);
                        }
                    });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addPlayer(player, 9 + row * 9 + col, 8 + 18 * col, 84 + 18 * row);
        for (int col = 0; col < 9; col++) addPlayer(player, col, 8 + 18 * col, 142);
    }

    private void addPlayer(Inventory inventory, int index, int x, int y) {
        addSlot(
                new Slot(inventory, index, x, y) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return ownerStack == null || getItem() != ownerStack;
                    }
                });
    }

    @Override
    public boolean stillValid(Player player) {
        return contents.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), prior = stack.copy();
        boolean moved =
                index < kind.capacity
                        ? com.industrialage.ia2.gui.MenuTransfer.move(
                                stack,
                                slots,
                                kind.capacity,
                                slots.size(),
                                true,
                                WorkshopPlatform::sameItem)
                        : com.industrialage.ia2.gui.MenuTransfer.move(
                                stack, slots, 0, kind.capacity, false, WorkshopPlatform::sameItem);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return prior;
    }
}
