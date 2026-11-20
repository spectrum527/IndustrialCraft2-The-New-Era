package com.industrialage.ia2.industrial;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

/** Simulates the same item-specific reactor limit that the real inventory applies on commit. */
final class PowerItemPort extends SidedInvWrapper {
    private final PowerNode node;

    PowerItemPort(PowerNode node, Direction side) {
        super(node.automationInventory(), side);
        this.node = node;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack item, boolean simulate) {
        if (item.isEmpty()) return ItemStack.EMPTY;
        int room = Math.max(0, node.automationLimit(item) - getStackInSlot(slot).getCount());
        if (room == 0) return item;
        if (item.getCount() <= room) return super.insertItem(slot, item, simulate);
        var offer = item.copy();
        offer.setCount(room);
        int accepted = room - super.insertItem(slot, offer, simulate).getCount();
        if (accepted == 0) return item;
        var remainder = item.copy();
        remainder.shrink(accepted);
        return remainder;
    }
}
