package com.industrialage.ia2.industrial;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Applies per-component limits to the native transaction before reserving the source count. */
final class PowerItemPort extends WorldlyContainerWrapper {
    private final PowerNode node;

    PowerItemPort(PowerNode node, Direction side) {
        super(node.automationInventory(), side);
        this.node = node;
    }

    @Override
    public long getCapacityAsLong(int slot, ItemResource item) {
        return Math.min(node.automationLimit(item.toStack(1)), super.getCapacityAsLong(slot, item));
    }

    @Override
    public int insert(int slot, ItemResource item, int amount, TransactionContext tx) {
        int room = (int) Math.max(0, getCapacityAsLong(slot, item) - getAmountAsLong(slot));
        return room == 0 ? 0 : super.insert(slot, item, Math.min(room, amount), tx);
    }
}
