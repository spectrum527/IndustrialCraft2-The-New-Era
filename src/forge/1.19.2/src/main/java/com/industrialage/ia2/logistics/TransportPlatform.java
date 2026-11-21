package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.forge.content.ForgeContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Wires the shared transport devices to Forge registration, item handlers and fluid handlers. */
public final class TransportPlatform {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "ic2");
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "ic2");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "ic2");
    private static final List<RegistryObject<TransportBlock>> DEVICES = new ArrayList<>();
    private static final RegistryObject<BlockEntityType<TransportNode>> NODE =
            ENTITIES.register(
                    "transport",
                    () ->
                            BlockEntityType.Builder.of(
                                            TransportNode::new,
                                            DEVICES.stream()
                                                    .map(RegistryObject::get)
                                                    .toArray(Block[]::new))
                                    .build(null));
    private static final RegistryObject<Item> PUMP_LV =
            ITEMS.register("pump_cover_lv", () -> new Item(ForgeContent.itemProperties()));
    private static final RegistryObject<Item> PUMP_MV =
            ITEMS.register("pump_cover_mv", () -> new Item(ForgeContent.itemProperties()));

    private TransportPlatform() {}

    /** Registers placeable logistics blocks, tools and fluid containers with the mod bus. */
    public static void register(IEventBus modBus) {
        TransportSpec.DEVICES.forEach(
                (path, spec) -> {
                    var block =
                            BLOCKS.register(
                                    path,
                                    () ->
                                            TransportBlock.create(
                                                    BlockBehaviour.Properties.of(Material.METAL)
                                                            .strength(2, 6)
                                                            .noOcclusion()
                                                            .sound(SoundType.METAL)
                                                            .requiresCorrectToolForDrops(),
                                                    spec));
                    DEVICES.add(block);
                    ITEMS.register(
                            path, () -> new BlockItem(block.get(), ForgeContent.itemProperties()));
                });
        ITEMS.register("wrench", () -> new ServiceTool(true));
        ITEMS.register("crowbar", () -> new ServiceTool(false));
        ITEMS.register("empty_cell", () -> new CellItem(false));
        ITEMS.register("fluid_cell", () -> new CellItem(true));
        TankMenus.install(modBus);
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist
                == net.minecraftforge.api.distmarker.Dist.CLIENT) TankMenus.clients(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
    }

    static BlockEntityType<TransportNode> type() {
        return NODE.get();
    }

    static String itemId(ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    static boolean sameItem(ItemStack left, ItemStack right) {
        return ItemStack.isSameItemSameTags(left, right) && left.areCapsCompatible(right);
    }

    static ItemStack coverItem(int rate) {
        return new ItemStack((rate == 32 ? PUMP_LV : PUMP_MV).get());
    }

    private static Optional<IItemHandler> itemHandler(Level world, BlockPos pos, Direction side) {
        if (world.hasChunkAt(pos)) {
            var entity = world.getBlockEntity(pos);
            if (entity != null) {
                return entity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).resolve();
            }
        }
        return Optional.empty();
    }

    static int offerItem(Level world, BlockPos pos, Direction side, ItemStack offered) {
        return itemHandler(world, pos, side)
                .map(
                        port -> {
                            var remaining =
                                    ItemHandlerHelper.insertItemStacked(
                                            port, offered.copy(), false);
                            return offered.getCount() - remaining.getCount();
                        })
                .orElse(0);
    }

    static ItemStack takeItem(Level world, BlockPos pos, Direction side, int count) {
        var port = itemHandler(world, pos, side);
        if (port.isPresent()) {
            var handler = port.get();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                var result = handler.extractItem(slot, count, false);
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static Optional<IFluidHandler> fluidHandler(Level world, BlockPos pos, Direction side) {
        return world.hasChunkAt(pos)
                ? FluidUtil.getFluidHandler(world, pos, side).resolve()
                : Optional.empty();
    }

    static boolean hasFluidPort(Level world, BlockPos pos, Direction side) {
        return fluidHandler(world, pos, side).isPresent();
    }

    static int offerFluid(
            Level world, BlockPos pos, Direction side, FluidKey key, int amount, boolean simulate) {
        return fluidHandler(world, pos, side)
                .map(port -> port.fill(key.stack(amount), action(simulate)))
                .orElse(0);
    }

    static LiquidNetwork.Batch takeFluid(
            Level world, BlockPos pos, Direction side, FluidKey key, int amount) {
        var result =
                fluidHandler(world, pos, side)
                        .map(
                                port ->
                                        key == null
                                                ? port.drain(
                                                        amount, IFluidHandler.FluidAction.EXECUTE)
                                                : port.drain(
                                                        key.stack(amount),
                                                        IFluidHandler.FluidAction.EXECUTE))
                        .orElse(FluidStack.EMPTY);
        return result.isEmpty()
                ? null
                : new LiquidNetwork.Batch(new FluidKey(result), result.getAmount());
    }

    /** A classic empty shell with no additional item data. */
    public static boolean isPristineCell(ItemStack cell) {
        return sameItem(
                cell,
                new ItemStack(
                        ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("ic2:empty_cell"))));
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
        return node.open(side)
                && FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()
                && (!(node.getLevel() instanceof ServerLevel)
                        || FluidUtil.interactWithFluidHandler(
                                player, hand, new FluidPort(node, side)));
    }

    private static IFluidHandler.FluidAction action(boolean simulate) {
        return simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
    }

    record ItemPort(TransportNode node, Direction side) implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? node.cargo.copy() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack incoming, boolean simulate) {
            if (!isItemValid(slot, incoming) || node.getLevel() == null) {
                return incoming;
            }
            int accepted =
                    node.insertItem(
                            side,
                            incoming,
                            simulate || !(node.getLevel() instanceof ServerLevel),
                            node.getLevel().getGameTime());
            if (accepted == 0) {
                return incoming;
            }
            var leftover = incoming.copy();
            leftover.shrink(accepted);
            return leftover.isEmpty() ? ItemStack.EMPTY : leftover;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0
                    || amount <= 0
                    || node.cargo.isEmpty()
                    || (side != null
                            && side != node.getBlockState().getValue(TransportBlock.OUT))) {
                return ItemStack.EMPTY;
            }
            var output = node.cargo.copy();
            output.setCount(Math.min(amount, output.getCount()));
            if (!simulate && node.getLevel() instanceof ServerLevel) {
                node.cargo.shrink(output.getCount());
                if (node.cargo.isEmpty()) {
                    node.cargo = ItemStack.EMPTY;
                }
                node.setChanged();
            }
            return output;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? node.spec.capacity() : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0
                    && !stack.isEmpty()
                    && side != node.getBlockState().getValue(TransportBlock.OUT);
        }
    }

    record FluidPort(TransportNode node, Direction side) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank != 0 || node.liquid == null
                    ? FluidStack.EMPTY
                    : node.liquid.stack(node.amount);
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? node.spec.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack fluid) {
            return tank == 0 && !fluid.isEmpty();
        }

        @Override
        public int fill(FluidStack offered, FluidAction action) {
            return offered.isEmpty()
                    ? 0
                    : node.fill(side, new FluidKey(offered), offered.getAmount(), simulate(action));
        }

        @Override
        public FluidStack drain(FluidStack requested, FluidAction action) {
            if (requested.isEmpty()) {
                return FluidStack.EMPTY;
            }
            var key = new FluidKey(requested);
            return drained(key, node.drain(side, key, requested.getAmount(), simulate(action)));
        }

        @Override
        public FluidStack drain(int requested, FluidAction action) {
            var key = node.liquid;
            return key == null
                    ? FluidStack.EMPTY
                    : drained(key, node.drain(side, key, requested, simulate(action)));
        }

        private boolean simulate(FluidAction action) {
            return action.simulate() || !(node.getLevel() instanceof ServerLevel);
        }

        private static FluidStack drained(FluidKey key, int amount) {
            return amount > 0 ? key.stack(amount) : FluidStack.EMPTY;
        }
    }

    private static final class ServiceTool extends Item {
        private final boolean wrench;

        ServiceTool(boolean wrench) {
            super(ForgeContent.itemProperties().durability(wrench ? 160 : 240));
            this.wrench = wrench;
        }

        @Override
        public boolean doesSneakBypassUse(
                ItemStack stack, LevelReader level, BlockPos pos, Player player) {
            return true;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (!wrench) return InteractionResult.PASS;
            var block = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
            if (!(block instanceof com.industrialage.ia2.industrial.BoxBlock box))
                return ForgeContent.wrenchBlock(context);
            var action = box.wrench(context);
            var player = context.getPlayer();
            if (action.consumesAction()
                    && !context.getLevel().isClientSide
                    && player != null
                    && !player.isCreative()) {
                context.getItemInHand()
                        .hurtAndBreak(
                                1, player, holder -> holder.broadcastBreakEvent(context.getHand()));
            }
            return action;
        }
    }
}
