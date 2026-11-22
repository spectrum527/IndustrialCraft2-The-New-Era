package com.industrialage.ia2.compatqa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class MachineSideHost {
    private MachineSideHost() {}

    static java.util.List<String> registeredBlocks() {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.keySet().stream()
                .filter(id -> id.getNamespace().equals("ic2"))
                .map(id -> id.getPath())
                .sorted()
                .toList();
    }

    static MachineSideChecks.Transfer item(ServerLevel level, BlockPos pos, Direction face) {
        return view(
                level.getCapability(Capabilities.Item.BLOCK, pos, face),
                ItemResource.of(new ItemStack(Items.COBBLESTONE)));
    }

    static MachineSideChecks.Transfer fluid(ServerLevel level, BlockPos pos, Direction face) {
        return view(
                level.getCapability(Capabilities.Fluid.BLOCK, pos, face),
                FluidResource.of(Fluids.WATER));
    }

    private static <T extends Resource> MachineSideChecks.Transfer view(
            ResourceHandler<T> port, T resource) {
        if (port == null) return null;
        return new MachineSideChecks.Transfer() {
            public int insert(int amount, boolean simulate) {
                try (var tx = Transaction.openRoot()) {
                    int moved = port.insert(resource, amount, tx);
                    if (!simulate) tx.commit();
                    return moved;
                }
            }

            public int extract(int amount, boolean simulate) {
                try (var tx = Transaction.openRoot()) {
                    int moved = port.extract(resource, amount, tx);
                    if (!simulate) tx.commit();
                    return moved;
                }
            }
        };
    }
}
