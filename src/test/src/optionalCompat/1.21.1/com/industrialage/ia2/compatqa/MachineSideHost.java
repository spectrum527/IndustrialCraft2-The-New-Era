package com.industrialage.ia2.compatqa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.ItemHandlerHelper;

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
        var port = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face);
        if (port == null) return null;
        return new MachineSideChecks.Transfer() {
            public int insert(int amount, boolean simulate) {
                return amount
                        - ItemHandlerHelper.insertItemStacked(
                                        port, new ItemStack(Items.COBBLESTONE, amount), simulate)
                                .getCount();
            }

            public int extract(int amount, boolean simulate) {
                for (int slot = 0; slot < port.getSlots(); slot++) {
                    var stack = port.extractItem(slot, amount, simulate);
                    if (!stack.isEmpty()) return stack.getCount();
                }
                return 0;
            }
        };
    }

    static MachineSideChecks.Transfer fluid(ServerLevel level, BlockPos pos, Direction face) {
        var port = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face);
        if (port == null) return null;
        return new MachineSideChecks.Transfer() {
            public int insert(int amount, boolean simulate) {
                return port.fill(
                        new FluidStack(Fluids.WATER, amount),
                        simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
            }

            public int extract(int amount, boolean simulate) {
                return port.drain(
                                new FluidStack(Fluids.WATER, amount),
                                simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE)
                        .getAmount();
            }
        };
    }
}
