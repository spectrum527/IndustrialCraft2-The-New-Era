package com.industrialage.ia2.compatqa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.ItemHandlerHelper;

final class MachineSideHost {
    private MachineSideHost() {}

    static java.util.List<String> registeredBlocks() {
        return net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKeys().stream()
                .filter(id -> id.getNamespace().equals("ic2"))
                .map(id -> id.getPath())
                .sorted()
                .toList();
    }

    static MachineSideChecks.Transfer item(ServerLevel level, BlockPos pos, Direction face) {
        return item(level, pos, face, new ItemStack(Items.COBBLESTONE));
    }

    static MachineSideChecks.Transfer item(
            ServerLevel level, BlockPos pos, Direction face, ItemStack pattern) {
        var port =
                level.getBlockEntity(pos)
                        .getCapability(ForgeCapabilities.ITEM_HANDLER, face)
                        .orElse(null);
        if (port == null) return null;
        return new MachineSideChecks.Transfer() {
            public int insert(int amount, boolean simulate) {
                var offered = pattern.copy();
                offered.setCount(amount);
                return amount
                        - ItemHandlerHelper.insertItemStacked(port, offered, simulate).getCount();
            }

            public int extract(int amount, boolean simulate) {
                for (int slot = 0; slot < port.getSlots(); slot++) {
                    if (!port.getStackInSlot(slot).is(pattern.getItem())) continue;
                    var stack = port.extractItem(slot, amount, simulate);
                    if (!stack.isEmpty()) return stack.getCount();
                }
                return 0;
            }
        };
    }

    static MachineSideChecks.Transfer fluid(ServerLevel level, BlockPos pos, Direction face) {
        var port =
                level.getBlockEntity(pos)
                        .getCapability(ForgeCapabilities.FLUID_HANDLER, face)
                        .orElse(null);
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
