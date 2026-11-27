package com.industrialage.ia2.world;

import com.industrialage.ia2.industrial.WorkshopThermal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

final class WorldFluidAccess {
    private WorldFluidAccess() {}

    static boolean handFluid(WorldMachine node, Player player, InteractionHand hand) {
        return net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(
                player, hand, new Port(node));
    }

    static boolean refillFoam(
            Level level, BlockPos pos, Direction side, ItemStack stack, FoamEquip tool) {
        if (!(level instanceof ServerLevel server)) return false;
        int drawn = drain(server, pos, side, "ic2:construction_foam", tool.room(stack));
        if (drawn <= 0) return false;
        tool.add(stack, drawn);
        return true;
    }

    static int drain(ServerLevel level, BlockPos pos, Direction side, String liquid, int maximum) {
        if (maximum <= 0 || !level.hasChunkAt(pos)) return 0;
        var entity = level.getBlockEntity(pos);
        var handler =
                entity == null
                        ? null
                        : entity.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
        if (handler == null) return 0;
        var resource = fluid(liquid);
        if (resource != null && resource != Fluids.EMPTY) {
            int exact =
                    handler.drain(
                                    new FluidStack(resource, maximum),
                                    IFluidHandler.FluidAction.EXECUTE)
                            .getAmount();
            if (exact > 0) return exact;
        }
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            var stored = handler.getFluidInTank(tank);
            if (stored.isEmpty() || stored.hasTag()) continue;
            if (!WorkshopThermal.matchesCanonical(fluidId(stored.getFluid()), liquid)) continue;
            var drained =
                    handler.drain(
                            new FluidStack(
                                    stored.getFluid(), Math.min(maximum, stored.getAmount())),
                            IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty() && !drained.hasTag()) return drained.getAmount();
        }
        return 0;
    }

    private static Fluid fluid(String id) {
        return net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(WorldNative.id(id));
    }

    private static String fluidId(Fluid fluid) {
        var key = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(fluid);
        return key == null ? "" : key.toString();
    }

    private static Fluid kind(int slot) {
        return slot == 0 ? Fluids.WATER : fluid("ic2:weed_ex");
    }

    private static String canonical(int slot) {
        return slot == 0 ? "minecraft:water" : "ic2:weed_ex";
    }

    static final class Port implements IFluidHandler {
        private final WorldMachine machine;

        Port(WorldMachine machine) {
            this.machine = machine;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public int getTankCapacity(int slot) {
            return slot >= 0 && slot < 2 ? 4000 : 0;
        }

        @Override
        public FluidStack getFluidInTank(int slot) {
            return getTankCapacity(slot) == 0 || machine.fluid(slot == 1) == 0
                    ? FluidStack.EMPTY
                    : new FluidStack(kind(slot), machine.fluid(slot == 1));
        }

        @Override
        public boolean isFluidValid(int slot, FluidStack stack) {
            return getTankCapacity(slot) > 0
                    && !stack.isEmpty()
                    && !stack.hasTag()
                    && WorkshopThermal.matchesCanonical(fluidId(stack.getFluid()), canonical(slot));
        }

        @Override
        public int fill(FluidStack stack, FluidAction action) {
            if (isFluidValid(0, stack))
                return machine.fill(false, stack.getAmount(), action.execute());
            return isFluidValid(1, stack)
                    ? machine.fill(true, stack.getAmount(), action.execute())
                    : 0;
        }

        @Override
        public FluidStack drain(FluidStack stack, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int max, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
