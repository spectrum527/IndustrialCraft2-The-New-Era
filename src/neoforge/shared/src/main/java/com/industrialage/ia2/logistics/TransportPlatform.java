package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.content.TransportSpec.Kind;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    private static final Map<String, DeferredHolder<Item, ? extends Item>> CELLS = new HashMap<>();
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
        ITEMS.registerItem("wrench", properties -> new Item(properties.durability(120)));
        ITEMS.registerItem("crowbar", properties -> new Item(properties.stacksTo(1)));
        for (var family : CellFamily.values()) {
            String shell = family.emptyPath();
            CELLS.put(shell, ITEMS.registerItem(shell, CellItem::new));
            family.namedLiquids()
                    .keySet()
                    .forEach(path -> CELLS.put(path, ITEMS.registerItem(path, CellItem::new)));
        }
        CellRecipes.install(bus);
        TankMenus.install(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TYPES.register(bus);
        COMPONENTS.register(bus);
        NeoTransport.registerCapabilities(bus);
    }

    /** Whether a cell already contains any liquid, including saved legacy contents. */
    public static boolean cellContainsFluid(ItemStack stack) {
        return cellContents(stack).amount() > 0;
    }

    /** Reads one cell's liquid without changing the stack or requiring an empty destination. */
    public static CellFamily.Contents cellContents(ItemStack cell) {
        return measure(cell.getItem() instanceof CellItem ? readCellFluid(cell) : FluidStack.EMPTY);
    }

    static CellFamily.Contents bucketContents(ItemStack stack) {
        return measure(NeoTransport.bucketFluid(stack));
    }

    private static CellFamily.Contents measure(FluidStack fluid) {
        return new CellFamily.Contents(fluid.isEmpty() ? "" : fluidId(fluid), fluid.getAmount());
    }

    static ItemStack fillFromBucket(ItemStack cell, ItemStack bucket) {
        var fluid = NeoTransport.bucketFluid(bucket);
        return fluid.getAmount() == CellFamily.CAPACITY && cellAdmits(cell, fluid)
                ? withCellContents(cell.copyWithCount(1), fluid)
                : ItemStack.EMPTY;
    }

    static ItemStack previewFilledCell(String id) {
        var fluid = WorkshopPlatform.fluid(id, CellFamily.CAPACITY);
        return fluid.isEmpty()
                ? ItemStack.EMPTY
                : withCellContents(
                        new ItemStack(CELLS.get(CellFamily.UNIVERSAL.emptyPath()).get()), fluid);
    }

    static ItemStack bindCell(ItemStack cell, ItemStack result) {
        result.applyComponents(emptyCellRemainder(cell).getComponentsPatch());
        return result;
    }

    /** Builds the reusable shell from one native fluid cell while retaining its item data. */
    public static ItemStack emptyCellRemainder(ItemStack cell) {
        return cell.getItem() instanceof CellItem
                ? withCellContents(cell.copyWithCount(1), FluidStack.EMPTY)
                : ItemStack.EMPTY;
    }

    /** Returns a universal shell for a specialized capsule consumed by its own device. */
    public static ItemStack emptyUniversalCell(ItemStack capsule) {
        if (capsule.isEmpty()) return ItemStack.EMPTY;
        return withCellContents(
                retype(capsule, CellFamily.UNIVERSAL.emptyPath()), FluidStack.EMPTY);
    }

    static boolean hasEncodedFluid(ItemStack stack) {
        var stored = stack.get(CELL_FLUID.get());
        return stored != null && !stored.isEmpty();
    }

    static Item[] cells() {
        return CELLS.values().stream().map(DeferredHolder::get).toArray(Item[]::new);
    }

    static boolean cellAdmits(ItemStack stack, FluidStack fluid) {
        var family = CellFamily.of(CellFamily.itemPath(itemId(stack)));
        return family != null && family.admits(fluidId(fluid));
    }

    static FluidStack readCellFluid(ItemStack stack) {
        var stored = stack.get(CELL_FLUID.get());
        if (stored != null && !stored.isEmpty()) return stored.copy();
        String path = CellFamily.itemPath(itemId(stack));
        var family = CellFamily.of(path);
        var liquid = family == null ? null : family.namedLiquids().get(path);
        return liquid == null
                ? FluidStack.EMPTY
                : WorkshopPlatform.fluid(liquid, CellFamily.CAPACITY);
    }

    static ItemStack withCellContents(ItemStack stack, FluidStack fluid) {
        String current = CellFamily.itemPath(itemId(stack));
        var family = CellFamily.of(current);
        String liquid = fluidId(fluid);
        String target = family.resultingPath(liquid, fluid.getAmount());
        if (!current.equals(target)) {
            stack = retype(stack, target);
        }
        if (!fluid.isEmpty()
                && (!fluid.getComponentsPatch().isEmpty()
                        || !liquid.equals(family.namedLiquids().get(target)))) {
            stack.set(CELL_FLUID.get(), SimpleFluidContent.copyOf(fluid));
        } else {
            stack.remove(CELL_FLUID.get());
        }
        return stack;
    }

    private static ItemStack retype(ItemStack stack, String target) {
        var replacement = new ItemStack(CELLS.get(target).get());
        replacement.applyComponents(stack.getComponentsPatch());
        return replacement;
    }

    private static String fluidId(FluidStack fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
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

    static ItemStack nozzleItem(TransportNode.Nozzle nozzle) {
        return NeoTransport.nozzleItem(nozzle);
    }

    static boolean hasItemPort(ServerLevel level, BlockPos pos, Direction side) {
        return NeoTransport.hasItemPort(level, pos, side);
    }

    static boolean canAcceptItem(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
        return NeoTransport.canAcceptItem(level, pos, side, stack);
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

    static void appendCellContents(FluidStack fluid, Consumer<Component> tooltip) {
        if (!fluid.isEmpty()) {
            tooltip.accept(fluid.getHoverName().copy().append(" " + fluid.getAmount() + " mB"));
        }
    }

    static ItemStack exchangeCursor(TransportNode node, ItemStack carried, Player who) {
        return NeoTransport.exchangeCursor(node, carried, who);
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
                || !node.permitsOutbound(side)) {
            return ItemStack.EMPTY;
        }
        int amount = Math.min(node.cargo.getCount(), requested);
        var stack = node.cargo.copyWithCount(amount);
        if (!simulate) {
            node.cargo.shrink(amount);
            if (node.cargo.isEmpty()) {
                node.cargo = ItemStack.EMPTY;
                node.cargoFrom = null;
                node.cargoTo = null;
                node.cargoStalled = false;
            }
            node.pendingItemVisualSync = true;
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
}
