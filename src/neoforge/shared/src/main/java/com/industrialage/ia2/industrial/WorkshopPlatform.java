package com.industrialage.ia2.industrial;

import com.industrialage.ia2.industrial.WorkshopPlans.ContainerResult;
import com.industrialage.ia2.industrial.WorkshopPlans.Liquid;
import com.industrialage.ia2.logistics.CellItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the same workshop blocks on all NeoForge targets and delegates native transfer. */
public final class WorkshopPlatform {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("ic2");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ic2");
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "ic2");
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, "ic2");
    private static final DeferredHolder<MenuType<?>, MenuType<WorkshopMenu>> MENU =
            MENUS.register(
                    "workshop",
                    () ->
                            new MenuType<>(
                                    (IContainerFactory<WorkshopMenu>) WorkshopMenu::fromNetwork,
                                    FeatureFlags.DEFAULT_FLAGS));
    private static final List<DeferredBlock<WorkshopBlock>> MACHINES = new ArrayList<>();
    private static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkshopNode>> TYPE =
            TYPES.register(
                    "workshop",
                    () ->
                            WorkshopNative.type(
                                    MACHINES.stream()
                                            .map(DeferredBlock::get)
                                            .toArray(Block[]::new)));

    private WorkshopPlatform() {}

    private static void registerCans() {
        ITEMS.registerSimpleItem("tin_can");
        WorkshopNative.filledCan(ITEMS);
    }

    /** Binds the workshop and its loader-specific item and fluid ports. */
    public static void install(IEventBus bus) {
        for (var kind : WorkshopKind.values()) {
            var block = WorkshopNative.block(BLOCKS, kind);
            MACHINES.add(block);
            ITEMS.registerSimpleBlockItem(block);
        }
        for (String blade : new String[] {"iron", "steel", "diamond"}) {
            ITEMS.registerSimpleItem(blade + "_cutting_blade");
        }
        WorkshopCells.FILLED.keySet().forEach(path -> ITEMS.registerItem(path, CellItem::new));
        registerCans();
        ITEMS.registerSimpleItem("electrolyzed_water_cell");
        ITEMS.registerSimpleItem("uu_matter");
        var scrapBox = ITEMS.registerItem("scrap_box", WorkshopNative::scrapBox);
        bus.addListener(
                FMLCommonSetupEvent.class,
                event ->
                        event.enqueueWork(
                                () ->
                                        DispenserBlock.registerBehavior(
                                                scrapBox.get(),
                                                new DefaultDispenseItemBehavior() {
                                                    @Override
                                                    protected ItemStack execute(
                                                            BlockSource source, ItemStack box) {
                                                        var direction =
                                                                source.state()
                                                                        .getValue(
                                                                                DispenserBlock
                                                                                        .FACING);
                                                        var output =
                                                                DispenserBlock.getDispensePosition(
                                                                        source);
                                                        scrapRewards(
                                                                source.level(),
                                                                reward ->
                                                                        spawnItem(
                                                                                source.level(),
                                                                                reward,
                                                                                6,
                                                                                direction,
                                                                                output));
                                                        box.shrink(1);
                                                        return box;
                                                    }
                                                })));
        for (String material : WorkshopKinetic.ROTOR_MATERIALS) {
            WorkshopNative.unstackable(ITEMS, material + "_rotor");
            ITEMS.registerSimpleItem(material + "_rotor_blade");
        }
        WorkshopNative.unstackable(ITEMS, "steam_turbine");
        ITEMS.registerSimpleItem("steam_turbine_blade");
        var casing = WorkshopNative.advancedCasing(BLOCKS);
        ITEMS.registerSimpleBlockItem(casing);
        registerFuelParts();
        WorkshopNative.unstackable(ITEMS, "crystal_memory");
        for (var kind : WorkshopToolBox.Kind.values()) {
            WorkshopNative.registerToolBox(ITEMS, kind);
        }
        BLOCKS.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        TYPES.register(bus);
        WorkshopNative.capabilities(bus);
    }

    static Block.Properties properties() {
        return Block.Properties.of()
                .strength(2, 10)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    static void scrapRewards(ServerLevel level, Consumer<ItemStack> reward) {
        var context = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        level.getServer()
                .reloadableRegistries()
                .getLootTable(WorkshopNative.scrapLootKey())
                .getRandomItems(context, reward);
    }

    private static void registerFuelParts() {
        for (String path :
                List.of(
                        "empty_fuel_can",
                        "filled_fuel_can",
                        "coalfuel_cell",
                        "biofuel_cell",
                        "hydrated_coal_cell",
                        "bio_cell",
                        "bronze_shaft",
                        "iron_shaft",
                        "steel_shaft",
                        "raw_crystal_memory")) {
            ITEMS.registerSimpleItem(path);
        }
    }

    static String blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    static ItemStack classicFuelCan(int value) {
        var result = stack("ic2:filled_fuel_can", 1);
        var data = new CompoundTag();
        data.putInt("value", value);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return result;
    }

    /** Returns the verified fuel time stored in a classic filled fuel can. */
    public static int classicFuelValue(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return itemId(stack).equals("ic2:filled_fuel_can") && data != null
                ? WorkshopNative.fuelCanValue(data.copyTag())
                : 0;
    }

    /** Returns the reusable unfilled can after its fuel is consumed. */
    public static ItemStack classicFuelRemainder(ItemStack stack) {
        return classicFuelValue(stack) > 0
                ? WorkshopPlatform.stack("ic2:empty_fuel_can", 1)
                : ItemStack.EMPTY;
    }

    static ItemStack[] loadToolBox(ItemStack stack, int size) {
        var contents = NonNullList.withSize(size, ItemStack.EMPTY);
        stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(contents);
        return contents.toArray(ItemStack[]::new);
    }

    static void saveToolBox(ItemStack stack, List<ItemStack> items) {
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    /** Menu type used by the registered native Workshop client. */
    public static MenuType<WorkshopMenu> menuType() {
        return MENU.get();
    }

    static int previewItemId(ItemStack stack) {
        return stack.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(stack.getItem());
    }

    static ItemStack previewItem(int id) {
        var item = BuiltInRegistries.ITEM.byId(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    static int previewFluidId(String name) {
        var liquid = fluid(name, 1);
        return liquid.isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(liquid.getFluid());
    }

    /** Localized fluid name shared with the material tank readout. */
    public static Component previewFluid(int id) {
        if (id != 0) {
            var fluid = BuiltInRegistries.FLUID.byId(id);
            if (fluid != null) {
                return fluid.getFluidType().getDescription();
            }
        }
        return Component.translatable("gui.ic2.workshop.empty");
    }

    static BlockEntityType<WorkshopNode> type() {
        return TYPE.get();
    }

    static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    static ItemStack stack(String id, int count) {
        return WorkshopNative.stack(id, count);
    }

    static boolean sameItem(ItemStack left, ItemStack right) {
        return ItemStack.isSameItemSameComponents(left, right);
    }

    static boolean tagged(ItemStack item, String tag) {
        return WorkshopNative.tagged(item, tag);
    }

    static boolean excludedFromRecycling(ItemStack item) {
        return item.is(Items.SNOWBALL)
                || item.is(Items.STICK)
                || item.is(Items.GLASS_PANE)
                || tagged(item, "ic2:recycler_blacklist");
    }

    static int solidFuelTime(ServerLevel level, ItemStack item) {
        return item.isEmpty() || item.is(Items.LAVA_BUCKET)
                ? 0
                : com.industrialage.ia2.platform.NeoForgeVersion.furnaceBurnTime(item, level);
    }

    static ItemStack fuelRemainder(ItemStack item) {
        return WorkshopNative.fuelRemainder(item);
    }

    static ItemStack smelt(ServerLevel level, ItemStack item) {
        return item.isEmpty() ? ItemStack.EMPTY : WorkshopNative.smelt(level, item);
    }

    static boolean fluidExists(String id) {
        return !id.isEmpty() && !WorkshopNative.fluid(id, 1).isEmpty();
    }

    static FluidStack fluid(String id, int amount) {
        return id.isEmpty() || amount <= 0 ? FluidStack.EMPTY : WorkshopNative.fluid(id, amount);
    }

    static String fluidId(FluidStack stack) {
        return BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString();
    }

    static String worldFluidId(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid).toString();
    }

    static boolean hasExternalFluidPort(ServerLevel level, BlockPos pos, Direction side) {
        return WorkshopNative.hasExternalFluidPort(level, pos, side);
    }

    static int pushFluid(
            ServerLevel level, BlockPos pos, Direction side, String fluid, int amount) {
        return WorkshopNative.pushFluid(level, pos, side, fluid, amount);
    }

    static Liquid pullFluid(
            ServerLevel level,
            BlockPos pos,
            Direction side,
            String fluid,
            int amount,
            boolean simulate) {
        return WorkshopNative.pullFluid(level, pos, side, fluid, amount, simulate);
    }

    static boolean canSupplyContainer(ItemStack item, boolean filling) {
        return WorkshopNative.canSupplyContainer(item, filling);
    }

    static ContainerResult fillContainer(WorkshopNode.Tank tank, ItemStack item) {
        return WorkshopNative.fillContainer(tank, item);
    }

    static ContainerResult drainWholeContainer(WorkshopNode.Tank tank, ItemStack item) {
        return WorkshopNative.drainWholeContainer(tank, item);
    }

    static boolean interactFluid(
            WorkshopNode node, Player player, InteractionHand hand, Direction side) {
        return WorkshopCells.fillNamedHand(node, side, player, hand)
                || WorkshopNative.interact(node, player, hand, side);
    }

    /** Handles the named-cell result when Minecraft dispatches directly to the held item. */
    public static boolean fillNamedHand(
            ServerLevel level, BlockPos pos, Direction side, Player player, InteractionHand hand) {
        return level.getBlockEntity(pos) instanceof WorkshopNode node
                && WorkshopCells.fillNamedHand(node, side, player, hand);
    }

    static void playPickup(ServerLevel level, BlockPos pos, Liquid moved) {
        var fluid = fluid(moved.id(), moved.amount());
        var sound = fluid.getFluidType().getSound(fluid, SoundActions.BUCKET_FILL);
        if (sound != null) level.playSound(null, pos, sound, SoundSource.BLOCKS, 1, 1);
    }

    static boolean canOpen(WorkshopNode node, Player player) {
        var pos = node.getBlockPos();
        return player.level() == node.getLevel()
                && !node.isRemoved()
                && node.getLevel().getBlockEntity(pos) == node
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    static void open(Player player, WorkshopNode node) {
        if (player instanceof ServerPlayer server) {
            server.openMenu(node, buffer -> buffer.writeEnum(node.kind));
        }
    }

    /** Returns the named original cell item for a plain, supported industrial liquid. */
    public static ItemStack namedFilledCell(FluidStack liquid) {
        if (liquid.isEmpty() || !liquid.getComponentsPatch().isEmpty()) {
            return ItemStack.EMPTY;
        }
        return WorkshopCells.namedStack(fluidId(liquid));
    }

    static void announceRoute(Player player, Direction face, int priority) {
        var message =
                Component.translatable(
                        "gui.ic2.workshop.fluid_priority", face.getSerializedName(), priority);
        WorkshopNative.announce(player, message);
    }

    static String readCrystalPattern(ItemStack memory) {
        var data = memory.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : WorkshopNative.crystalPattern(data.copyTag());
    }

    static void writeCrystalPattern(ItemStack memory, String value) {
        CustomData.update(
                DataComponents.CUSTOM_DATA, memory, data -> data.putString("ic2_pattern", value));
    }

    static void announcePattern(Player player, ItemStack pattern) {
        var text =
                Component.translatable("gui.ic2.workshop.recorded_pattern", pattern.getHoverName());
        WorkshopNative.announce(player, text);
    }

    static void announceMode(Player player, WorkshopNode node) {
        WorkshopNative.announce(player, Component.translatable(node.modeKey()));
    }
}
