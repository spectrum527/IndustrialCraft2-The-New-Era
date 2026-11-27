package com.industrialage.ia2.world;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.access.MachinePorts;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

final class WorldFluidAccess {
    private WorldFluidAccess() {}

    static boolean handFluid(WorldMachine node, Player player, InteractionHand hand) {
        return net.neoforged.neoforge.transfer.fluid.FluidUtil.interactWithFluidHandler(
                player, hand, node.getBlockPos(), new Port(node));
    }

    static void install(IEventBus bus) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event -> {
                    event.registerBlockEntity(
                            Capabilities.Item.BLOCK,
                            WorldPlatform.machineType(),
                            (machine, side) ->
                                    MachineAccess.connects(machine, side)
                                            ? MachinePorts.item(
                                                    machine,
                                                    side,
                                                    itemPort(machine.inventory, side))
                                            : null);
                    event.registerBlockEntity(
                            Capabilities.Fluid.BLOCK,
                            WorldPlatform.machineType(),
                            (machine, side) ->
                                    machine.kind == WorldMachineBlock.Kind.CROPMATRON
                                                    && MachineAccess.connects(machine, side)
                                            ? MachinePorts.fluid(machine, side, new Port(machine))
                                            : null);
                });
    }

    private static WorldlyContainerWrapper itemPort(WorldInventory inventory, Direction side) {
        Direction face = side == null ? Direction.UP : side;
        return new WorldlyContainerWrapper(inventory, face) {
            @Override
            public boolean isValid(int slot, ItemResource item) {
                return !item.isEmpty()
                        && inventory.canPlaceItemThroughFace(slot, item.toStack(1), face);
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
        var handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        if (handler == null) return 0;
        var resource = fluid(liquid);
        if (resource != null && resource != Fluids.EMPTY) {
            try (var transaction = Transaction.openRoot()) {
                int drained = handler.extract(FluidResource.of(resource), maximum, transaction);
                if (drained > 0) {
                    transaction.commit();
                    return drained;
                }
            }
        }
        try (var transaction = Transaction.openRoot()) {
            var batch =
                    ResourceHandlerUtil.extractFirst(
                            handler,
                            candidate ->
                                    candidate.getComponentsPatch().isEmpty()
                                            && WorkshopThermal.matchesCanonical(
                                                    fluidId(candidate.getFluid()), liquid),
                            maximum,
                            transaction);
            if (batch == null) return 0;
            transaction.commit();
            return batch.amount();
        }
    }

    private static Fluid fluid(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(WorldNative.id(id));
    }

    private static String fluidId(Fluid fluid) {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid).toString();
    }

    private static Fluid kind(int slot) {
        return slot == 0 ? Fluids.WATER : fluid("ic2:weed_ex");
    }

    private static String canonical(int slot) {
        return slot == 0 ? "minecraft:water" : "ic2:weed_ex";
    }

    private static final class Port extends SnapshotJournal<int[]>
            implements ResourceHandler<FluidResource> {
        private final WorldMachine machine;

        Port(WorldMachine machine) {
            this.machine = machine;
        }

        @Override
        protected int[] createSnapshot() {
            return new int[] {machine.fluid(false), machine.fluid(true)};
        }

        @Override
        protected void revertToSnapshot(int[] snapshot) {
            machine.setFluid(false, snapshot[0]);
            machine.setFluid(true, snapshot[1]);
        }

        @Override
        protected void onRootCommit(int[] snapshot) {
            machine.setChanged();
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public FluidResource getResource(int slot) {
            return machine.fluid(slot == 1) == 0
                    ? FluidResource.EMPTY
                    : FluidResource.of(kind(slot));
        }

        @Override
        public long getAmountAsLong(int slot) {
            return machine.fluid(slot == 1);
        }

        @Override
        public long getCapacityAsLong(int slot, FluidResource resource) {
            return 4000;
        }

        @Override
        public boolean isValid(int slot, FluidResource resource) {
            return slot >= 0
                    && slot < 2
                    && !resource.isEmpty()
                    && resource.getComponentsPatch().isEmpty()
                    && WorkshopThermal.matchesCanonical(
                            fluidId(resource.getFluid()), canonical(slot));
        }

        @Override
        public int insert(int slot, FluidResource resource, int amount, TransactionContext tx) {
            if (!isValid(slot, resource)) return 0;
            int offered = machine.fill(slot == 1, amount, false);
            if (offered > 0) {
                updateSnapshots(tx);
                machine.setFluid(slot == 1, machine.fluid(slot == 1) + offered);
            }
            return offered;
        }

        @Override
        public int extract(int slot, FluidResource resource, int amount, TransactionContext tx) {
            return 0;
        }
    }
}
