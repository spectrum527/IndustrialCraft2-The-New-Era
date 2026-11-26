package com.industrialage.ia2.access;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** The processor's native sided view with the same stack limits as its power slots. */
public final class ProcessingItemPort extends WorldlyContainerWrapper {
    private final ProcessingSlots slots;
    private final Direction face;

    /** Selects the top layout for unsided lookups without exposing the raw machine inventory. */
    public ProcessingItemPort(ProcessingSlots slots, Direction face) {
        super(slots, face == null ? Direction.UP : face);
        this.slots = slots;
        this.face = face == null ? Direction.UP : face;
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
    public long getCapacityAsLong(int slot, ItemResource resource) {
        int[] mapped = slots.getSlotsForFace(face);
        if (slot < 0 || slot >= mapped.length) return 0;
        if (resource.isEmpty()) return super.getCapacityAsLong(slot, resource);
        return Math.min(
                super.getCapacityAsLong(slot, resource),
                slots.automationLimit(mapped[slot], resource.toStack()));
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        int[] mapped = slots.getSlotsForFace(face);
        return !resource.isEmpty()
                && slot >= 0
                && slot < mapped.length
                && slots.canPlaceItemThroughFace(mapped[slot], resource.toStack(), face);
    }

    @Override
    public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) return 0;
        long room = getCapacityAsLong(slot, resource) - getAmountAsLong(slot);
        return room <= 0
                ? 0
                : super.insert(slot, resource, (int) Math.min(amount, room), transaction);
    }
}
