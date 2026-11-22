package com.industrialage.ia2.logistics;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.access.MachinePorts;
import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.content.TransportSpec.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Transactional item and fluid handlers for NeoForge 26.x. */
final class NeoTransport {
    static final int CELL_CAPACITY = 1_000;

    private NeoTransport() {}

    static BlockEntityType<TransportNode> type(Block[] blocks) {
        return new BlockEntityType<>(TransportNode::new, blocks);
    }

    static DeferredBlock<TransportBlock> block(DeferredRegister.Blocks blocks, TransportSpec spec) {
        return blocks.registerBlock(
                spec.path(),
                props -> TransportBlock.create(props, spec),
                () -> TransportPlatform.properties(spec));
    }

    static void registerCapabilities(IEventBus bus) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event -> {
                    event.registerBlockEntity(
                            Capabilities.Item.BLOCK,
                            TransportPlatform.type(),
                            (node, side) ->
                                    node.spec.kind() == Kind.ITEM_PIPE
                                            ? new ItemPort(node, side)
                                            : null);
                    event.registerBlockEntity(
                            Capabilities.Fluid.BLOCK,
                            TransportPlatform.type(),
                            (node, side) -> {
                                if (node.spec.kind() == Kind.ITEM_PIPE) return null;
                                var port = new FluidPort(node, side);
                                if (!node.exposesMachineAccess()) return port;
                                return MachineAccess.connects(node, side)
                                        ? MachinePorts.fluid(node, side, port)
                                        : null;
                            });
                    event.registerItem(
                            Capabilities.Fluid.ITEM,
                            (stack, access) -> new CellAccess(access),
                            TransportPlatform.cells());
                });
    }

    static FluidStack bucketFluid(ItemStack stack) {
        return FluidUtil.getFirstStackContained(stack.copyWithCount(1));
    }

    static ItemStack nozzleItem(TransportNode.Nozzle nozzle) {
        return BuiltInRegistries.ITEM
                .getOptional(Identifier.fromNamespaceAndPath("ic2", nozzle.path()))
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    static boolean hasItemPort(ServerLevel level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.Item.BLOCK, pos, side) != null;
    }

    static int offerItem(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null) {
            return 0;
        }
        try (var transaction = Transaction.openRoot()) {
            int moved =
                    ResourceHandlerUtil.insertStacking(
                            handler, ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
            return moved;
        }
    }

    static ItemStack takeItem(ServerLevel level, BlockPos pos, Direction side, int amount) {
        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null) {
            return ItemStack.EMPTY;
        }
        try (var transaction = Transaction.openRoot()) {
            var stack =
                    ResourceHandlerUtil.extractFirst(
                            handler, resource -> true, amount, transaction);
            if (stack == null) {
                return ItemStack.EMPTY;
            }
            transaction.commit();
            return stack.resource().toStack(stack.amount());
        }
    }

    static boolean hasFluidPort(ServerLevel level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.Fluid.BLOCK, pos, side) != null;
    }

    static int offerFluid(
            ServerLevel level,
            BlockPos pos,
            Direction side,
            FluidKey key,
            int amount,
            boolean simulate) {
        var handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        if (handler == null) {
            return 0;
        }
        try (var transaction = Transaction.openRoot()) {
            int accepted = handler.insert(FluidResource.of(key.stack(1)), amount, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return accepted;
        }
    }

    static LiquidNetwork.Batch takeFluid(
            ServerLevel level, BlockPos pos, Direction side, FluidKey requested, int amount) {
        var handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        if (handler == null) {
            return null;
        }
        var filter = requested == null ? null : FluidResource.of(requested.stack(1));
        try (var transaction = Transaction.openRoot()) {
            var liquid =
                    ResourceHandlerUtil.extractFirst(
                            handler,
                            resource -> filter == null || resource.equals(filter),
                            amount,
                            transaction);
            if (liquid == null || liquid.amount() == 0) {
                return null;
            }
            transaction.commit();
            return new LiquidNetwork.Batch(
                    new FluidKey(liquid.resource().toStack(1)), liquid.amount());
        }
    }

    static ItemStack exchangeCursor(TransportNode node, ItemStack carried, Player who) {
        var one = carried.copyWithCount(1);
        var container = new net.minecraft.world.SimpleContainer(one);
        var cell =
                ItemAccess.forHandlerIndex(
                                net.neoforged.neoforge.transfer.item.VanillaContainerWrapper.of(
                                        container),
                                0)
                        .getCapability(Capabilities.Fluid.ITEM);
        if (cell == null) return null;
        var tank = new FluidPort(node, Direction.UP);
        try (var tx = Transaction.openRoot()) {
            int moved =
                    ResourceHandlerUtil.move(cell, tank, resource -> true, Integer.MAX_VALUE, tx);
            if (moved == 0)
                moved =
                        ResourceHandlerUtil.move(
                                tank, cell, resource -> true, Integer.MAX_VALUE, tx);
            if (moved == 0) return null;
            tx.commit();
        }
        return container.getItem(0);
    }

    static boolean containerInteraction(
            TransportNode node, Direction side, Player player, InteractionHand hand) {
        var held = ItemAccess.forPlayerInteraction(player, hand);
        if (held.getCapability(Capabilities.Fluid.ITEM) == null) {
            return false;
        }
        if (node.getLevel() instanceof ServerLevel) {
            FluidUtil.interactWithFluidHandler(
                    player, hand, node.getBlockPos(), new FluidPort(node, side, true));
        }
        return true;
    }

    private record ItemPort(TransportNode node, Direction side)
            implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int slot) {
            return ItemResource.of(
                    slot == 0 && TransportPlatform.available(node) ? node.cargo : ItemStack.EMPTY);
        }

        @Override
        public long getAmountAsLong(int slot) {
            return slot == 0 && TransportPlatform.available(node) ? node.cargo.getCount() : 0;
        }

        @Override
        public long getCapacityAsLong(int slot, ItemResource resource) {
            return slot == 0 ? Math.min(node.spec.capacity(), resource.getMaxStackSize()) : 0;
        }

        @Override
        public boolean isValid(int slot, ItemResource resource) {
            return slot == 0
                    && TransportPlatform.available(node)
                    && !resource.isEmpty()
                    && node.permitsInbound(side);
        }

        @Override
        public int insert(
                int slot, ItemResource resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || !isValid(slot, resource)) {
                return 0;
            }
            var stack = resource.toStack(Math.min(amount, node.spec.capacity()));
            long now = node.getLevel().getGameTime();
            int inserted = node.insertItem(side, stack, true, now);
            if (inserted > 0) {
                node.journal.updateSnapshots(transaction);
                return node.insertItem(side, stack, false, now);
            }
            return 0;
        }

        @Override
        public int extract(
                int slot, ItemResource resource, int amount, TransactionContext transaction) {
            if (!resource.equals(getResource(slot))) {
                return 0;
            }
            var preview = TransportPlatform.extract(node, side, amount, true);
            if (!preview.isEmpty()) {
                node.journal.updateSnapshots(transaction);
                return TransportPlatform.extract(node, side, amount, false).getCount();
            }
            return 0;
        }
    }

    private record FluidPort(TransportNode node, Direction side, boolean manual)
            implements ResourceHandler<FluidResource> {
        FluidPort(TransportNode node, Direction side) {
            this(node, side, false);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int slot) {
            return slot == 0 && TransportPlatform.available(node)
                    ? FluidResource.of(TransportPlatform.fluid(node))
                    : FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int slot) {
            return slot == 0 && TransportPlatform.available(node) ? node.amount : 0;
        }

        @Override
        public long getCapacityAsLong(int slot, FluidResource resource) {
            return slot == 0 ? node.spec.capacity() : 0;
        }

        @Override
        public boolean isValid(int slot, FluidResource resource) {
            return slot == 0
                    && TransportPlatform.available(node)
                    && (manual ? node.open(side) : node.permitsInbound(side))
                    && !resource.isEmpty();
        }

        @Override
        public int insert(
                int slot, FluidResource resource, int amount, TransactionContext transaction) {
            if (!isValid(slot, resource) || amount <= 0) {
                return 0;
            }
            var key = new FluidKey(resource.toStack(1));
            int inserted = node.fill(side, key, amount, true);
            if (inserted > 0) {
                node.journal.updateSnapshots(transaction);
                return node.fill(side, key, inserted, false);
            }
            return 0;
        }

        @Override
        public int extract(
                int slot, FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty()
                    || !resource.equals(getResource(slot))
                    || (!manual && !node.permitsOutbound(side))) {
                return 0;
            }
            var key = new FluidKey(resource.toStack(1));
            int extracted = node.drain(side, key, amount, true);
            if (extracted > 0) {
                node.journal.updateSnapshots(transaction);
                return node.drain(side, key, extracted, false);
            }
            return 0;
        }
    }
}
