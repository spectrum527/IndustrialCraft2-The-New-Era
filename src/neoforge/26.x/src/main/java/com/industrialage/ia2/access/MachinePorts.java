package com.industrialage.ia2.access;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Live, directional views of the machine's own transactional storage. */
public final class MachinePorts {
    private MachinePorts() {}

    /** Binds an item handler and preserves the original transaction and slot restrictions. */
    public static ResourceHandler<ItemResource> item(
            BlockEntity machine, Direction side, ResourceHandler<ItemResource> source) {
        return new Sided<>(machine, side, source);
    }

    /** Binds a fluid handler and preserves the original transaction and resource identity. */
    public static ResourceHandler<FluidResource> fluid(
            BlockEntity machine, Direction side, ResourceHandler<FluidResource> source) {
        return new Sided<>(machine, side, source);
    }

    private record Sided<T extends Resource>(
            BlockEntity machine, Direction side, ResourceHandler<T> source)
            implements ResourceHandler<T> {
        @Override
        public int size() {
            return source.size();
        }

        @Override
        public T getResource(int slot) {
            return source.getResource(slot);
        }

        @Override
        public long getAmountAsLong(int slot) {
            return source.getAmountAsLong(slot);
        }

        @Override
        public long getCapacityAsLong(int slot, T type) {
            return source.getCapacityAsLong(slot, type);
        }

        @Override
        public boolean isValid(int slot, T type) {
            return MachineAccess.allowsInput(machine, side) && source.isValid(slot, type);
        }

        @Override
        public int insert(int slot, T type, int amount, TransactionContext tx) {
            return MachineAccess.allowsInput(machine, side)
                    ? source.insert(slot, type, amount, tx)
                    : 0;
        }

        @Override
        public int extract(int slot, T type, int amount, TransactionContext tx) {
            return MachineAccess.allowsOutput(machine, side)
                    ? source.extract(slot, type, amount, tx)
                    : 0;
        }

        @Override
        public int insert(T type, int amount, TransactionContext tx) {
            return MachineAccess.allowsInput(machine, side) ? source.insert(type, amount, tx) : 0;
        }

        @Override
        public int extract(T type, int amount, TransactionContext tx) {
            return MachineAccess.allowsOutput(machine, side) ? source.extract(type, amount, tx) : 0;
        }
    }
}
