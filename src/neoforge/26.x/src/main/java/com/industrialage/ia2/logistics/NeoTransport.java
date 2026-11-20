package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.content.TransportSpec.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Transactional item and fluid handlers for NeoForge 26.x. */
final class NeoTransport {
    private static final int CELL_CAPACITY = 1_000;

    private NeoTransport() {}

    static DeferredHolder<Item, Item> fluidCell(DeferredRegister.Items items) {
        return items.registerItem(
                "fluid_cell", properties -> new FluidCell(properties.stacksTo(1)));
    }

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
                            (node, side) ->
                                    node.spec.kind() != Kind.ITEM_PIPE
                                            ? new FluidPort(node, side)
                                            : null);
                    event.registerItem(
                            Capabilities.Fluid.ITEM,
                            (stack, access) ->
                                    new ItemAccessFluidHandler(
                                            access,
                                            TransportPlatform.CELL_FLUID.get(),
                                            CELL_CAPACITY),
                            TransportPlatform.CELL.get(),
                            TransportPlatform.FLUID_CELL.get());
                });
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

    static boolean containerInteraction(
            TransportNode node, Direction side, Player player, InteractionHand hand) {
        var held = ItemAccess.forPlayerInteraction(player, hand);
        if (held.getCapability(Capabilities.Fluid.ITEM) == null) {
            return false;
        }
        if (node.getLevel() instanceof ServerLevel) {
            FluidUtil.interactWithFluidHandler(
                    player, hand, node.getBlockPos(), new FluidPort(node, side));
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
                    && side != node.getBlockState().getValue(TransportBlock.OUT);
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

    private record FluidPort(TransportNode node, Direction side)
            implements ResourceHandler<FluidResource> {
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
                    && node.open(side)
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
            if (resource.isEmpty() || !resource.equals(getResource(slot))) {
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

    private static final class FluidCell extends Item {
        FluidCell(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            var player = context.getPlayer();
            if (player == null) {
                return InteractionResult.PASS;
            }
            var level = context.getLevel();
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            var tank =
                    server.getCapability(
                            Capabilities.Fluid.BLOCK,
                            context.getClickedPos(),
                            context.getClickedFace());
            return tank != null
                            && FluidUtil.interactWithFluidHandler(
                                    player,
                                    context.getHand(),
                                    context.getLevel(),
                                    context.getClickedPos(),
                                    context.getClickedFace(),
                                    null)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            var fluid = FluidUtil.getFirstStackContained(stack);
            boolean pickup = fluid.isEmpty();
            if (!pickup && fluid.getAmount() < CELL_CAPACITY) {
                return InteractionResult.PASS;
            }
            var hit =
                    getPlayerPOVHitResult(
                            level,
                            player,
                            pickup ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
            if (hit.getType() != HitResult.Type.BLOCK) {
                return InteractionResult.PASS;
            }
            var pos = hit.getBlockPos();
            var side = hit.getDirection();
            var target = pickup ? pos : pos.relative(side);
            if (!level.mayInteract(player, pos) || !player.mayUseItemAt(target, side, stack)) {
                return InteractionResult.FAIL;
            }
            if (!(level instanceof ServerLevel)) {
                return InteractionResult.SUCCESS;
            }
            var port =
                    ItemAccess.forPlayerInteraction(player, hand)
                            .oneByOne()
                            .getCapability(Capabilities.Fluid.ITEM);
            var moved =
                    pickup
                            ? FluidUtil.tryPickupFluid(port, player, level, pos, side, null)
                            : FluidUtil.tryPlaceFluid(port, player, level, target, false, null);
            return moved.isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
    }
}
