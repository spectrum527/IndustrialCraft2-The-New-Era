package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.content.TransportSpec.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.HitResult;
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
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sided item and fluid handlers for NeoForge 21.1. */
final class NeoTransport {
    private static final int CELL_CAPACITY = 1_000;

    private NeoTransport() {}

    static DeferredHolder<Item, Item> fluidCell(DeferredRegister.Items items) {
        return items.registerItem(
                "fluid_cell", properties -> new FluidCell(properties.stacksTo(1)));
    }

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
                            Capabilities.FluidHandler.BLOCK,
                            context.getClickedPos(),
                            context.getClickedFace());
            return tank != null
                            && FluidUtil.interactWithFluidHandler(
                                    player,
                                    context.getHand(),
                                    context.getLevel(),
                                    context.getClickedPos(),
                                    context.getClickedFace())
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(
                Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            var fluid = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
            boolean pickup = fluid.isEmpty();
            if (!pickup && fluid.getAmount() < CELL_CAPACITY) {
                return InteractionResultHolder.pass(stack);
            }
            var hit =
                    getPlayerPOVHitResult(
                            level,
                            player,
                            pickup ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
            if (hit.getType() != HitResult.Type.BLOCK) {
                return InteractionResultHolder.pass(stack);
            }
            var pos = hit.getBlockPos();
            var side = hit.getDirection();
            var target = pickup ? pos : pos.relative(side);
            if (!level.mayInteract(player, pos) || !player.mayUseItemAt(target, side, stack)) {
                return InteractionResultHolder.fail(stack);
            }
            if (!(level instanceof ServerLevel)) {
                return InteractionResultHolder.success(stack);
            }
            var moved =
                    pickup
                            ? FluidUtil.tryPickUpFluid(stack, player, level, pos, side)
                            : FluidUtil.tryPlaceFluid(player, level, hand, target, stack, fluid);
            return moved.isSuccess()
                    ? InteractionResultHolder.consume(
                            ItemUtils.createFilledResult(stack, player, moved.getResult()))
                    : InteractionResultHolder.pass(stack);
        }
    }
}
