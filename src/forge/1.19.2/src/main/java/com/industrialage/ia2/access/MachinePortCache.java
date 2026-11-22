package com.industrialage.ia2.access;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

/** Holds Forge's invalidation tokens for the actual machine and physical side. */
public final class MachinePortCache {
    private final BlockEntity owner;
    private final Map<Direction, LazyOptional<IItemHandler>> inventories = new HashMap<>();
    private final Map<Direction, LazyOptional<IFluidHandler>> tanks = new HashMap<>();

    /** Starts an empty cache, including an independent entry for unsided requests. */
    public MachinePortCache(BlockEntity owner) {
        this.owner = owner;
    }

    /** Returns an invalidatable native inventory view. */
    public LazyOptional<IItemHandler> item(Direction face, Supplier<IItemHandler> factory) {
        if (!MachineAccess.connects(owner, face)) return LazyOptional.empty();
        return inventories.computeIfAbsent(
                face, key -> LazyOptional.of(() -> MachinePorts.item(owner, key, factory.get())));
    }

    /** Returns an invalidatable native fluid view. */
    public LazyOptional<IFluidHandler> fluid(Direction face, Supplier<IFluidHandler> factory) {
        if (!MachineAccess.connects(owner, face)) return LazyOptional.empty();
        return tanks.computeIfAbsent(
                face, key -> LazyOptional.of(() -> MachinePorts.fluid(owner, key, factory.get())));
    }

    /** Invalidates inventory and tank listeners without disturbing energy capabilities. */
    public void invalidate() {
        var previousItems = java.util.List.copyOf(inventories.values());
        var previousFluids = java.util.List.copyOf(tanks.values());
        inventories.clear();
        tanks.clear();
        previousItems.forEach(LazyOptional::invalidate);
        previousFluids.forEach(LazyOptional::invalidate);
    }
}
