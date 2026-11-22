package com.industrialage.ia2.industrial;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Applies per-component limits to the native transaction before reserving the source count. */
final class PowerItemPort extends WorldlyContainerWrapper {
    private final PowerNode node;
    private final net.minecraft.world.WorldlyContainer inventory;
    private final Direction side;

    PowerItemPort(PowerNode node, Direction side) {
        this(node, node.automationInventory(), side);
    }

    private PowerItemPort(
            PowerNode node, net.minecraft.world.WorldlyContainer inventory, Direction side) {
        super(inventory, side);
        this.node = node;
        this.inventory = inventory;
        this.side = side;
    }

    @Override
    public boolean isValid(int slot, ItemResource item) {
        return !item.isEmpty() && inventory.canPlaceItemThroughFace(slot, item.toStack(1), side);
    }

    @Override
    public long getCapacityAsLong(int slot, ItemResource item) {
        return Math.min(
                node.automationLimit(slot, item.toStack(1)), super.getCapacityAsLong(slot, item));
    }

    @Override
    public int insert(int slot, ItemResource item, int amount, TransactionContext tx) {
        if (amount <= 0 || !isValid(slot, item)) return 0;
        int room = (int) Math.max(0, getCapacityAsLong(slot, item) - getAmountAsLong(slot));
        return room == 0 ? 0 : super.insert(slot, item, Math.min(room, amount), tx);
    }
}
