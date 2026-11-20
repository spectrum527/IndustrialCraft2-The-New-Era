package com.industrialage.ia2.logistics;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Synchronizes the tank's native fluid, including amounts larger than a 16-bit menu field. */
final class TankMenu extends AbstractContainerMenu {
    private final TransportNode node;
    private final SimpleContainerData data = new SimpleContainerData(5);
    final int upgradeCount;

    TankMenu(int id, Inventory player, TransportNode node, boolean industrial) {
        super(industrial ? TankMenus.UTILITY.get() : TankMenus.TYPE.get(), id);
        this.node = node;
        upgradeCount = industrial ? 4 : 0;
        var upgrades =
                node == null
                        ? new net.minecraft.world.SimpleContainer(upgradeCount)
                        : node.upgrades;
        for (int i = 0; i < upgradeCount; i++)
            addSlot(
                    new Slot(upgrades, i, 152, 8 + i * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return TransportNode.isFluidUpgrade(stack);
                        }
                    });
        for (int i = 0; i < 27; i++)
            addSlot(new Slot(player, 9 + i, 8 + 18 * (i % 9), 84 + 18 * (i / 9)));
        for (int i = 0; i < 9; i++) addSlot(new Slot(player, i, 8 + 18 * i, 142));
        addDataSlots(data);
        syncTank();
    }

    int amount() {
        return read(0);
    }

    int capacity() {
        return read(2);
    }

    int fluid() {
        return data.get(4);
    }

    private int read(int index) {
        return (data.get(index) & 65535) | (data.get(index + 1) << 16);
    }

    private void write(int index, int value) {
        data.set(index, value & 65535);
        data.set(index + 1, value >>> 16);
    }

    private void syncTank() {
        if (node != null) {
            write(0, node.amount);
            write(2, node.spec.capacity());
            data.set(4, node.liquid == null ? 0 : node.liquid.registryId());
        }
    }

    @Override
    public void broadcastChanges() {
        syncTank();
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return node == null
                || !node.isRemoved()
                        && player.distanceToSqr(
                                        node.getBlockPos().getX() + .5,
                                        node.getBlockPos().getY() + .5,
                                        node.getBlockPos().getZ() + .5)
                                <= 64;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || node == null || !stillValid(player) || getCarried().isEmpty())
            return false;
        var carried = getCarried();
        ItemStack changed;
        if (node.liquid != null
                && TransportPlatform.isPristineCell(carried)
                && node.amount >= 1000) {
            changed =
                    com.industrialage.ia2.industrial.WorkshopPlatform.namedFilledCell(
                            node.liquid.stack(1));
            if (!changed.isEmpty()) node.debit(1000);
            else changed = TransportPlatform.exchangeCursor(node, carried, player);
        } else changed = TransportPlatform.exchangeCursor(node, carried, player);
        if (changed == null) return false;
        if (carried.getCount() == 1) setCarried(changed);
        else {
            carried.shrink(1);
            if (!changed.isEmpty() && !player.getInventory().add(changed))
                player.drop(changed, false);
        }
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = getSlot(index);
        var stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        var result = stack.copy();
        boolean moved;
        if (index < upgradeCount) moved = moveItemStackTo(stack, upgradeCount, slots.size(), true);
        else if (upgradeCount > 0 && TransportNode.isFluidUpgrade(stack))
            moved = moveItemStackTo(stack, 0, upgradeCount, false);
        else if (index < upgradeCount + 27)
            moved = moveItemStackTo(stack, upgradeCount + 27, slots.size(), false);
        else moved = moveItemStackTo(stack, upgradeCount, upgradeCount + 27, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return result;
    }
}
