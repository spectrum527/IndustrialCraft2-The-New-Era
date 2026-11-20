package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.content.TransportSpec.Kind;
import com.industrialage.ia2.gameplay.IndustrialContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NeoForge registration and native capability entry points for the transport network. */
public final class TransportPlatform {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("ic2");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ic2");
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "ic2");
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "ic2");

    private static final List<DeferredBlock<TransportBlock>> DEVICES = new ArrayList<>();
    private static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransportNode>> TYPE =
            TYPES.register(
                    "transport",
                    () ->
                            NeoTransport.type(
                                    DEVICES.stream()
                                            .map(DeferredBlock::get)
                                            .toArray(Block[]::new)));
    static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>>
            CELL_FLUID =
                    COMPONENTS.register(
                            "cell_fluid",
                            () ->
                                    DataComponentType.<SimpleFluidContent>builder()
                                            .persistent(SimpleFluidContent.CODEC)
                                            .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                                            .build());

    /** Component storing liquid in industrial cells and fluid item adapters. */
    public static DataComponentType<SimpleFluidContent> cellFluidComponent() {
        return CELL_FLUID.get();
    }

    static final DeferredHolder<Item, Item> CELL = ITEMS.registerSimpleItem("empty_cell");
    static final DeferredHolder<Item, Item> FLUID_CELL = NeoTransport.fluidCell(ITEMS);
    private static final DeferredHolder<Item, Item> LV = ITEMS.registerSimpleItem("pump_cover_lv");
    private static final DeferredHolder<Item, Item> MV = ITEMS.registerSimpleItem("pump_cover_mv");

    private TransportPlatform() {}

    /** Registers placeable pipes and tanks, their tools, and sided NeoForge capabilities. */
    public static void install(IEventBus bus) {
        for (var spec : TransportSpec.DEVICES.values()) {
            var block = NeoTransport.block(BLOCKS, spec);
            DEVICES.add(block);
            ITEMS.registerSimpleBlockItem(block);
        }
        ITEMS.registerItem("wrench", properties -> new Wrench(properties.durability(120)));
        ITEMS.registerItem("crowbar", properties -> new Item(properties.stacksTo(1)));
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TYPES.register(bus);
        COMPONENTS.register(bus);
        NeoTransport.registerCapabilities(bus);
    }

    /** Returns whether the universal cell carries a fluid that must not be replaced by a recipe. */
    public static boolean cellContainsFluid(ItemStack stack) {
        var contents = stack.get(CELL_FLUID.get());
        return stack.is(CELL.get()) && contents != null && !contents.isEmpty();
    }

    static Block.Properties properties(TransportSpec spec) {
        var properties = Block.Properties.of().sound(SoundType.METAL);
        return spec.kind() == Kind.TANK
                ? properties.strength(2, 10).requiresCorrectToolForDrops()
                : properties.strength(0.6F).noOcclusion();
    }

    static BlockEntityType<TransportNode> type() {
        return TYPE.get();
    }

    static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    static boolean sameItem(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameComponents(first, second);
    }

    static ItemStack coverItem(int rate) {
        return new ItemStack(rate == 128 ? MV.get() : LV.get());
    }

    static int offerItem(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
        return NeoTransport.offerItem(level, pos, side, stack);
    }

    static ItemStack takeItem(ServerLevel level, BlockPos pos, Direction side, int amount) {
        return NeoTransport.takeItem(level, pos, side, amount);
    }

    static boolean hasFluidPort(ServerLevel level, BlockPos pos, Direction side) {
        return NeoTransport.hasFluidPort(level, pos, side);
    }

    static int offerFluid(
            ServerLevel level,
            BlockPos pos,
            Direction side,
            FluidKey key,
            int amount,
            boolean simulate) {
        return NeoTransport.offerFluid(level, pos, side, key, amount, simulate);
    }

    static LiquidNetwork.Batch takeFluid(
            ServerLevel level, BlockPos pos, Direction side, FluidKey requested, int amount) {
        return NeoTransport.takeFluid(level, pos, side, requested, amount);
    }

    static boolean isPristineCell(ItemStack cell) {
        return sameItem(cell, new ItemStack(CELL.get()));
    }

    static boolean containerInteraction(
            TransportNode node, Direction side, Player player, InteractionHand hand) {
        return NeoTransport.containerInteraction(node, side, player, hand);
    }

    static boolean available(TransportNode node) {
        return !node.isRemoved() && node.getLevel() != null;
    }

    static ItemStack extract(TransportNode node, Direction side, int requested, boolean simulate) {
        if (!available(node)
                || node.cargo.isEmpty()
                || requested <= 0
                || side != null && side != node.getBlockState().getValue(TransportBlock.OUT)) {
            return ItemStack.EMPTY;
        }
        int amount = Math.min(node.cargo.getCount(), requested);
        var stack = node.cargo.copyWithCount(amount);
        if (!simulate) {
            node.cargo.shrink(amount);
            if (node.cargo.isEmpty()) {
                node.cargo = ItemStack.EMPTY;
            }
            node.setChanged();
        }
        return stack;
    }

    static FluidStack fluid(TransportNode node) {
        return node.liquid == null ? FluidStack.EMPTY : node.liquid.stack(node.amount);
    }

    static void restore(
            TransportNode node, ItemStack item, FluidStack fluid, int closed, int lv, int mv) {
        node.cargo = ItemStack.EMPTY;
        node.liquid = null;
        node.amount = 0;
        if (node.spec.kind() == Kind.ITEM_PIPE) {
            item.limitSize(Math.min(node.spec.capacity(), item.getMaxStackSize()));
            node.cargo = item;
        } else if (!fluid.isEmpty()) {
            node.liquid = new FluidKey(fluid);
            node.amount = Math.min(node.spec.capacity(), fluid.getAmount());
        }
        if (node.spec.kind() == Kind.FLUID_PIPE) {
            node.closed = closed & 63;
            node.lv = lv & 63;
            node.mv = mv & 63 & ~node.lv;
        }
    }

    private static final class Wrench extends Item {
        Wrench(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
            var action = IndustrialContent.wrenchBlock(context);
            if (action.consumesAction()
                    && context.getLevel() instanceof ServerLevel
                    && context.getPlayer() != null) {
                stack.hurtAndBreak(
                        1,
                        context.getPlayer(),
                        context.getHand() == InteractionHand.MAIN_HAND
                                ? EquipmentSlot.MAINHAND
                                : EquipmentSlot.OFFHAND);
            }
            return action;
        }
    }
}
