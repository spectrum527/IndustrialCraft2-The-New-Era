package com.industrialage.ia2.access;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

/** The processor's native sided view with the same stack limits as its power slots. */
public final class ProcessingItemPort extends SidedInvWrapper {
    private final ProcessingSlots slots;

    /** Selects the top layout for unsided lookups without exposing the raw machine inventory. */
    public ProcessingItemPort(ProcessingSlots slots, Direction face) {
        super(slots, face == null ? Direction.UP : face);
        this.slots = slots;
    }

    /** Whether a portable energy item is currently able to supply power to this tier. */
    public static boolean canSupplyCharge(ItemStack stack, int tier) {
        return com.industrialage.ia2.gameplay.PortableGear.canSupplyCharge(stack, tier);
    }

    /** Whether this stack is a completed product of a charging slot at the specified tier. */
    public static boolean chargeComplete(ItemStack stack, int tier) {
        return com.industrialage.ia2.gameplay.PortableGear.chargeComplete(stack, tier);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int index = getSlot(slots, slot, side);
        if (index < 0) return stack;
        int room = slots.automationLimit(index, stack) - slots.getItem(index).getCount();
        if (room <= 0) return stack;
        if (room >= stack.getCount()) return super.insertItem(slot, stack, simulate);
        var offered = stack.copy();
        offered.setCount(room);
        int inserted = room - super.insertItem(slot, offered, simulate).getCount();
        var remainder = stack.copy();
        remainder.shrink(inserted);
        return remainder;
    }
}
