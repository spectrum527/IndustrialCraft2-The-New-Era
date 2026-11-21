package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.content.TransportSpec.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sided item and fluid handlers for NeoForge 21.1. */
final class NeoTransport {
    static final int CELL_CAPACITY = 1_000;

    private NeoTransport() {}

    static BlockEntityType<TransportNode> type(Block[] blocks) {
        return BlockEntityType.Builder.of(TransportNode::new, blocks).build(null);
    }

    static DeferredBlock<TransportBlock> block(DeferredRegister.Blocks blocks, TransportSpec spec) {
        return blocks.registerBlock(
                spec.path(),
                props -> TransportBlock.create(props, spec),
                TransportPlatform.properties(spec));
    }

    static void registerCapabilities(IEventBus bus) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event -> {
                    event.registerBlockEntity(
                            Capabilities.ItemHandler.BLOCK,
                            TransportPlatform.type(),
                            (node, side) ->
                                    node.spec.kind() == Kind.ITEM_PIPE
                                            ? new ItemPort(node, side)
                                            : null);
                    event.registerBlockEntity(
                            Capabilities.FluidHandler.BLOCK,
                            TransportPlatform.type(),
                            (node, side) ->
                                    node.spec.kind() != Kind.ITEM_PIPE
                                            ? new FluidPort(node, side)
                                            : null);
                    event.registerItem(
                            Capabilities.FluidHandler.ITEM,
                            (stack, ignored) ->
                                    new FluidHandlerItemStack(
                                            TransportPlatform.CELL_FLUID, stack, CELL_CAPACITY),
                            TransportPlatform.CELL.get(),
                            TransportPlatform.FLUID_CELL.get());
                });
    }

    static int offerItem(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (handler == null) {
            return 0;
        }
        return stack.getCount()
                - ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false).getCount();
    }

    static ItemStack takeItem(ServerLevel level, BlockPos pos, Direction side, int amount) {
        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (handler != null) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                var stack = handler.extractItem(slot, amount, false);
                if (!stack.isEmpty()) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    static boolean hasFluidPort(ServerLevel level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null;
    }

    static int offerFluid(
            ServerLevel level,
            BlockPos pos,
            Direction side,
            FluidKey key,
            int amount,
            boolean simulate) {
        var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        return handler == null
                ? 0
                : handler.fill(
                        key.stack(amount),
                        simulate
                                ? IFluidHandler.FluidAction.SIMULATE
                                : IFluidHandler.FluidAction.EXECUTE);
    }

    static LiquidNetwork.Batch takeFluid(
            ServerLevel level, BlockPos pos, Direction side, FluidKey requested, int amount) {
        var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        if (handler == null) {
            return null;
        }
        var liquid =
                requested == null
                        ? handler.drain(amount, IFluidHandler.FluidAction.EXECUTE)
                        : handler.drain(requested.stack(amount), IFluidHandler.FluidAction.EXECUTE);
        return liquid.isEmpty()
                ? null
                : new LiquidNetwork.Batch(new FluidKey(liquid), liquid.getAmount());
    }

    static ItemStack exchangeCursor(TransportNode node, ItemStack carried, Player who) {
        var single = carried.copy();
        single.setCount(1);
        var port = new FluidPort(node, Direction.UP);
        var result = FluidUtil.tryEmptyContainer(single, port, Integer.MAX_VALUE, who, true);
        if (!result.isSuccess())
            result = FluidUtil.tryFillContainer(single, port, Integer.MAX_VALUE, who, true);
        return result.isSuccess() ? result.getResult() : null;
    }

    static boolean containerInteraction(
            TransportNode node, Direction side, Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        if (held.isEmpty() || held.getCapability(Capabilities.FluidHandler.ITEM) == null) {
            return false;
        }
        if (node.getLevel() instanceof ServerLevel) {
            FluidUtil.interactWithFluidHandler(player, hand, new FluidPort(node, side));
        }
        return true;
    }

    private record ItemPort(TransportNode node, Direction side) implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 && TransportPlatform.available(node)
                    ? node.cargo.copy()
                    : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            int inserted = node.insertItem(side, stack, simulate, node.getLevel().getGameTime());
            return stack.copyWithCount(stack.getCount() - inserted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0
                    ? TransportPlatform.extract(node, side, amount, simulate)
                    : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? node.spec.capacity() : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0
                    && TransportPlatform.available(node)
                    && side != node.getBlockState().getValue(TransportBlock.OUT);
        }
    }

    private record FluidPort(TransportNode node, Direction side) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 && TransportPlatform.available(node)
                    ? TransportPlatform.fluid(node)
                    : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? node.spec.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0
                    && TransportPlatform.available(node)
                    && node.open(side)
                    && !stack.isEmpty();
        }

        @Override
        public int fill(FluidStack stack, FluidAction action) {
            return isFluidValid(0, stack)
                    ? node.fill(side, new FluidKey(stack), stack.getAmount(), action.simulate())
                    : 0;
        }

        @Override
        public FluidStack drain(FluidStack stack, FluidAction action) {
            return stack.isEmpty()
                    ? FluidStack.EMPTY
                    : drain(new FluidKey(stack), stack.getAmount(), action);
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return drain(node.liquid, amount, action);
        }

        private FluidStack drain(FluidKey key, int amount, FluidAction action) {
            if (key == null || !TransportPlatform.available(node)) {
                return FluidStack.EMPTY;
            }
            int moved = node.drain(side, key, amount, action.simulate());
            return moved == 0 ? FluidStack.EMPTY : key.stack(moved);
        }
    }
}
