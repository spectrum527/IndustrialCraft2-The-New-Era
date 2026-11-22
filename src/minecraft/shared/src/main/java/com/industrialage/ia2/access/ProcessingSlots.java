package com.industrialage.ia2.access;

import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

/** A machine's sided inventory, with a stack limit for each exposed processing slot. */
public interface ProcessingSlots extends WorldlyContainer {
    /** The maximum number of equivalent items an automation transfer may store in this slot. */
    int automationLimit(int slot, ItemStack stack);
}
