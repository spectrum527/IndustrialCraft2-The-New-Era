package com.industrialage.ia2.world;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.access.MachinePorts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

final class WorldFluidAccess {
    private WorldFluidAccess() {}

    static boolean handFluid(WorldMachine node, Player player, InteractionHand hand) {
        return net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(
                player, hand, new Port(node));
    }

    static void install(IEventBus bus) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event -> {
                    event.registerBlockEntity(
                            Capabilities.ItemHandler.BLOCK,
                            WorldPlatform.machineType(),
                            (machine, side) ->
                                    MachineAccess.connects(machine, side)
                                            ? MachinePorts.item(
                                                    machine,
                                                    side,
                                                    itemPort(machine.inventory, side))
                                            : null);
                    event.registerBlockEntity(
                            Capabilities.FluidHandler.BLOCK,
                            WorldPlatform.machineType(),
                            (machine, side) ->
                                    machine.kind == WorldMachineBlock.Kind.CROPMATRON
                                                    && MachineAccess.connects(machine, side)
                                            ? MachinePorts.fluid(machine, side, new Port(machine))
                                            : null);
                });
    }

    private static SidedInvWrapper itemPort(WorldInventory inventory, Direction side) {
        Direction face = side == null ? Direction.UP : side;
        return new SidedInvWrapper(inventory, face) {
            @Override
            public boolean isItemValid(int slot, ItemStack item) {
                return inventory.canPlaceItemThroughFace(slot, item, face);
            }
        };
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
        var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        var resource = fluid(liquid);
        if (handler == null || resource == null || resource == Fluids.EMPTY) return 0;
        return handler.drain(new FluidStack(resource, maximum), IFluidHandler.FluidAction.EXECUTE)
                .getAmount();
    }

    private static Fluid fluid(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.get(WorldNative.id(id));
    }

    private static Fluid kind(int slot) {
        return slot == 0 ? Fluids.WATER : fluid("ic2:weed_ex");
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
            return getTankCapacity(slot) > 0 && stack.getFluid() == kind(slot);
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
