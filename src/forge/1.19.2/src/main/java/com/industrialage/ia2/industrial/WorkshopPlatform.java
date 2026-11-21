package com.industrialage.ia2.industrial;

import com.industrialage.ia2.forge.content.ForgeContent;
import com.industrialage.ia2.industrial.WorkshopPlans.ContainerResult;
import com.industrialage.ia2.industrial.WorkshopPlans.Liquid;
import com.industrialage.ia2.logistics.CellItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Forge 1.19.2 registry, menu, recipe, and fluid adapter for shared industrial processors. */
public final class WorkshopPlatform {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "ic2");
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "ic2");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "ic2");
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, "ic2");
    private static final RegistryObject<MenuType<WorkshopMenu>> MENU =
            MENUS.register("workshop", () -> IForgeMenuType.create(WorkshopMenu::fromNetwork));
    private static final List<RegistryObject<WorkshopBlock>> MACHINES = new ArrayList<>();
    private static final RegistryObject<BlockEntityType<WorkshopNode>> TYPE =
            ENTITIES.register(
                    "workshop",
                    () ->
                            BlockEntityType.Builder.of(
                                            WorkshopNode::new,
                                            MACHINES.stream()
                                                    .map(RegistryObject::get)
                                                    .toArray(Block[]::new))
                                    .build(null));

    private WorkshopPlatform() {}

    private static void registerCans() {
        ITEMS.register("tin_can", () -> new Item(ForgeContent.itemProperties()));
        ITEMS.register(
                "filled_tin_can",
                () ->
                        new Item(ForgeContent.itemProperties()) {
                            @Override
                            public net.minecraft.world.InteractionResultHolder<
                                            net.minecraft.world.item.ItemStack>
                                    use(
                                            net.minecraft.world.level.Level level,
                                            net.minecraft.world.entity.player.Player player,
                                            net.minecraft.world.InteractionHand hand) {
                                var held = player.getItemInHand(hand);
                                return CannedFood.eat(level, player, held)
                                        ? net.minecraft.world.InteractionResultHolder.sidedSuccess(
                                                held, level.isClientSide)
                                        : net.minecraft.world.InteractionResultHolder.pass(held);
                            }
                        });
    }

    /** Binds all real workshop machines and their unique working components to the mod bus. */
    public static void install(IEventBus bus) {
        for (var kind : WorkshopKind.values()) {
            var block =
                    BLOCKS.register(
                            kind.path,
                            () ->
                                    WorkshopBlock.create(
                                            Block.Properties.of(Material.METAL)
                                                    .strength(2, 10)
                                                    .sound(SoundType.METAL)
                                                    .requiresCorrectToolForDrops(),
                                            kind));
            MACHINES.add(block);
            ITEMS.register(
                    kind.path, () -> new BlockItem(block.get(), ForgeContent.itemProperties()));
        }
        for (String blade : new String[] {"iron", "steel", "diamond"}) {
            ITEMS.register(
                    blade + "_cutting_blade",
                    () -> new Item(ForgeContent.itemProperties().stacksTo(1)));
        }
        WorkshopCells.FILLED.forEach(
                (path, liquid) ->
                        ITEMS.register(
                                path,
                                () ->
                                        CellItem.named(
                                                () -> fluid(liquid, WorkshopCells.VOLUME),
                                                () -> stack("ic2:empty_cell", 1))));
        registerCans();
        ITEMS.register("electrolyzed_water_cell", () -> new Item(ForgeContent.itemProperties()));
        ITEMS.register("uu_matter", () -> new Item(ForgeContent.itemProperties()));
        var scrapBox =
                ITEMS.register(
                        "scrap_box",
                        () ->
                                new Item(ForgeContent.itemProperties()) {
                                    @Override
                                    public InteractionResultHolder<ItemStack> use(
                                            Level level, Player player, InteractionHand hand) {
                                        var box = player.getItemInHand(hand);
                                        ScrapBoxContents.open(level, player, box);
                                        return InteractionResultHolder.sidedSuccess(
                                                box, level.isClientSide);
                                    }
                                });
        bus.addListener(
                (FMLCommonSetupEvent event) ->
                        event.enqueueWork(
                                () ->
                                        DispenserBlock.registerBehavior(
                                                scrapBox.get(),
                                                new DefaultDispenseItemBehavior() {
                                                    @Override
                                                    protected ItemStack execute(
                                                            BlockSource source, ItemStack box) {
                                                        var direction =
                                                                source.getBlockState()
                                                                        .getValue(
                                                                                DispenserBlock
                                                                                        .FACING);
                                                        var output =
                                                                DispenserBlock.getDispensePosition(
                                                                        source);
                                                        scrapRewards(
                                                                source.getLevel(),
                                                                reward ->
                                                                        spawnItem(
                                                                                source.getLevel(),
                                                                                reward,
                                                                                6,
                                                                                direction,
                                                                                output));
                                                        box.shrink(1);
                                                        return box;
                                                    }
                                                })));
        for (String material : WorkshopKinetic.ROTOR_MATERIALS) {
            ITEMS.register(
                    material + "_rotor", () -> new Item(ForgeContent.itemProperties().stacksTo(1)));
            ITEMS.register(
                    material + "_rotor_blade", () -> new Item(ForgeContent.itemProperties()));
        }
        ITEMS.register("steam_turbine", () -> new Item(ForgeContent.itemProperties().stacksTo(1)));
        ITEMS.register("steam_turbine_blade", () -> new Item(ForgeContent.itemProperties()));
        var casing =
                BLOCKS.register(
                        "advanced_machine",
                        () ->
                                new Block(
                                        Block.Properties.of(Material.METAL)
                                                .strength(3, 10)
                                                .sound(SoundType.METAL)
                                                .requiresCorrectToolForDrops()));
        ITEMS.register(
                "advanced_machine",
                () -> new BlockItem(casing.get(), ForgeContent.itemProperties()));
        registerFuelParts();
        ITEMS.register("crystal_memory", () -> new Item(ForgeContent.itemProperties().stacksTo(1)));
        for (var kind : WorkshopToolBox.Kind.values()) {
            ITEMS.register(kind.path, () -> new ToolBoxItem(kind));
        }
        BLOCKS.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        ENTITIES.register(bus);
    }

    static void scrapRewards(ServerLevel level, Consumer<ItemStack> reward) {
        var key = ResourceLocation.fromNamespaceAndPath("ic2", "gameplay/scrap_box");
        var context = new LootContext.Builder(level).create(LootContextParamSets.EMPTY);
        level.getServer().getLootTables().get(key).getRandomItems(context, reward);
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
            ITEMS.register(path, () -> new Item(ForgeContent.itemProperties()));
        }
    }

    static String blockId(Block block) {
        var key = ForgeRegistries.BLOCKS.getKey(block);
        return key == null ? "" : key.toString();
    }

    static ItemStack classicFuelCan(int value) {
        var result = stack("ic2:filled_fuel_can", 1);
        var data = result.getOrCreateTag();
        data.putInt("value", value);
        return result;
    }

    /** Returns the verified fuel time stored in a classic filled fuel can. */
    public static int classicFuelValue(ItemStack stack) {
        return itemId(stack).equals("ic2:filled_fuel_can") && stack.hasTag()
                ? Math.max(0, stack.getTag().getInt("value"))
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
        var tag = stack.getTagElement("ic2_tool_box");
        if (tag != null) {
            ContainerHelper.loadAllItems(tag, contents);
        }
        return contents.toArray(ItemStack[]::new);
    }

    static void saveToolBox(ItemStack stack, List<ItemStack> items) {
        var contents = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int index = 0; index < items.size(); index++) {
            contents.set(index, items.get(index));
        }
        var saved = ContainerHelper.saveAllItems(new CompoundTag(), contents, false);
        if (saved.isEmpty()) {
            stack.removeTagKey("ic2_tool_box");
        } else {
            stack.getOrCreateTag().put("ic2_tool_box", saved);
        }
    }

    private static final class ToolBoxItem extends Item {
        private final WorkshopToolBox.Kind kind;

        ToolBoxItem(WorkshopToolBox.Kind kind) {
            super(ForgeContent.itemProperties().stacksTo(1));
            this.kind = kind;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(
                Level level, Player owner, InteractionHand hand) {
            if (level instanceof ServerLevel) {
                WorkshopToolBox.open(owner, hand, kind);
            }
            return InteractionResultHolder.sidedSuccess(
                    owner.getItemInHand(hand), level.isClientSide);
        }
    }

    /** Menu type used by the registered native Workshop client. */
    public static MenuType<WorkshopMenu> menuType() {
        return MENU.get();
    }

    static int previewItemId(ItemStack stack) {
        return stack.isEmpty() ? 0 : Registry.ITEM.getId(stack.getItem());
    }

    static ItemStack previewItem(int id) {
        var item = Registry.ITEM.byId(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    static int previewFluidId(String name) {
        var liquid = fluid(name, 1);
        return liquid.isEmpty() ? 0 : Registry.FLUID.getId(liquid.getFluid());
    }

    /** Localized fluid name shared with the material tank readout. */
    public static Component previewFluid(int id) {
        if (id != 0) {
            var fluid = Registry.FLUID.byId(id);
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
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    static ItemStack stack(String identifier, int count) {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(identifier));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    static boolean sameItem(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameTags(first, second) && first.areCapsCompatible(second);
    }

    static boolean tagged(ItemStack item, String tag) {
        return item.is(TagKey.create(Registry.ITEM_REGISTRY, ResourceLocation.parse(tag)));
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
                : net.minecraftforge.common.ForgeHooks.getBurnTime(item, RecipeType.SMELTING);
    }

    static ItemStack fuelRemainder(ItemStack item) {
        return item.getCraftingRemainingItem();
    }

    static ItemStack smelt(ServerLevel level, ItemStack item) {
        if (item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var contents = new SimpleContainer(item);
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, contents, level)
                .map(recipe -> recipe.assemble(contents))
                .orElse(ItemStack.EMPTY);
    }

    static boolean fluidExists(String id) {
        var key = ResourceLocation.tryParse(id);
        return !id.isEmpty() && key != null && ForgeRegistries.FLUIDS.containsKey(key);
    }

    static FluidStack fluid(String id, int amount) {
        return amount > 0 && fluidExists(id)
                ? new FluidStack(
                        ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(id)), amount)
                : FluidStack.EMPTY;
    }

    static String fluidId(FluidStack stack) {
        var key = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        return key == null ? "" : key.toString();
    }

    static String worldFluidId(Fluid fluid) {
        var key = ForgeRegistries.FLUIDS.getKey(fluid);
        return key == null ? "" : key.toString();
    }

    private static IFluidHandler neighborFluid(ServerLevel level, BlockPos pos, Direction side) {
        return level.hasChunkAt(pos)
                ? FluidUtil.getFluidHandler(level, pos, side).orElse(null)
                : null;
    }

    static boolean hasExternalFluidPort(ServerLevel level, BlockPos pos, Direction side) {
        return neighborFluid(level, pos, side) != null;
    }

    static int pushFluid(
            ServerLevel level, BlockPos pos, Direction side, String fluid, int amount) {
        var handler = neighborFluid(level, pos, side);
        return handler == null
                ? 0
                : handler.fill(fluid(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
    }

    static Liquid pullFluid(
            ServerLevel level,
            BlockPos pos,
            Direction side,
            String fluid,
            int amount,
            boolean simulate) {
        var handler = neighborFluid(level, pos, side);
        if (handler == null || amount <= 0) {
            return Liquid.EMPTY;
        }
        var stack =
                fluid.isEmpty()
                        ? handler.drain(amount, IFluidHandler.FluidAction.SIMULATE)
                        : handler.drain(fluid(fluid, amount), IFluidHandler.FluidAction.SIMULATE);
        if (stack.isEmpty() || stack.hasTag()) {
            return Liquid.EMPTY;
        }
        if (!simulate) {
            stack = handler.drain(stack, IFluidHandler.FluidAction.EXECUTE);
        }
        return stack.isEmpty() ? Liquid.EMPTY : new Liquid(fluidId(stack), stack.getAmount());
    }

    static boolean canSupplyContainer(ItemStack item, boolean filling) {
        var single = item.copy();
        single.setCount(1);
        return FluidUtil.getFluidHandler(single)
                .map(
                        handler ->
                                handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE)
                                                .isEmpty()
                                        == filling)
                .orElse(false);
    }

    static ContainerResult fillContainer(WorkshopNode.Tank tank, ItemStack item) {
        var copy = item.copy();
        copy.setCount(1);
        var handler = FluidUtil.getFluidHandler(copy).orElse(null);
        var liquid = fluid(tank.id, tank.amount);
        if (handler == null || liquid.isEmpty() || liquid.hasTag()) return null;
        int amount = handler.fill(liquid, IFluidHandler.FluidAction.EXECUTE);
        return amount > 0
                ? new ContainerResult(handler.getContainer(), new Liquid(fluidId(liquid), amount))
                : null;
    }

    static ContainerResult drainWholeContainer(WorkshopNode.Tank tank, ItemStack item) {
        var copy = item.copy();
        copy.setCount(1);
        var handler = FluidUtil.getFluidHandler(copy).orElse(null);
        if (handler == null) return null;
        var contents =
                WorkshopCells.wholeContents(
                        tank,
                        handler.getTanks(),
                        slot -> {
                            var stored = handler.getFluidInTank(slot);
                            if (stored.isEmpty()) return Liquid.EMPTY;
                            return stored.hasTag()
                                    ? null
                                    : new Liquid(fluidId(stored), stored.getAmount());
                        });
        if (contents.amount() == 0) return null;
        var drained =
                handler.drain(
                        fluid(contents.id(), contents.amount()), IFluidHandler.FluidAction.EXECUTE);
        return drained.isEmpty()
                        || drained.hasTag()
                        || drained.getAmount() != contents.amount()
                        || !fluidId(drained).equals(contents.id())
                ? null
                : new ContainerResult(handler.getContainer(), contents);
    }

    static boolean interactFluid(
            WorkshopNode node, Player player, InteractionHand hand, Direction side) {
        var access = new WorkshopFluidPort(node, side);
        return access.size() > 0
                && FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()
                && (!(node.getLevel() instanceof ServerLevel)
                        || FluidUtil.interactWithFluidHandler(player, hand, new Port(access)));
    }

    static boolean canOpen(WorkshopNode node, Player player) {
        var pos = node.getBlockPos();
        return player.level == node.getLevel()
                && !node.isRemoved()
                && node.getLevel().getBlockEntity(pos) == node
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    static void open(Player player, WorkshopNode node) {
        if (player instanceof ServerPlayer server) {
            NetworkHooks.openScreen(server, node, buffer -> buffer.writeEnum(node.kind));
        }
    }

    /** Returns the named original cell item for a plain, supported industrial liquid. */
    public static ItemStack namedFilledCell(FluidStack liquid) {
        if (liquid.isEmpty() || liquid.hasTag()) {
            return ItemStack.EMPTY;
        }
        return WorkshopCells.namedStack(fluidId(liquid));
    }

    static void announceRoute(Player player, Direction face, int priority) {
        var message =
                Component.translatable(
                        "gui.ic2.workshop.fluid_priority", face.getSerializedName(), priority);
        player.displayClientMessage(message, true);
    }

    static String readCrystalPattern(ItemStack memory) {
        var data = memory.getTag();
        return data == null ? "" : data.getString("ic2_pattern");
    }

    static void writeCrystalPattern(ItemStack memory, String value) {
        memory.getOrCreateTag().putString("ic2_pattern", value);
    }

    static void announcePattern(Player player, ItemStack pattern) {
        var text =
                Component.translatable("gui.ic2.workshop.recorded_pattern", pattern.getHoverName());
        player.displayClientMessage(text, true);
    }

    static void announceMode(Player player, WorkshopNode node) {
        player.displayClientMessage(Component.translatable(node.modeKey()), true);
    }

    record Port(WorkshopFluidPort access) implements IFluidHandler {
        @Override
        public int getTanks() {
            return access.size();
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            var tank = access.tank(index);
            return tank == null ? FluidStack.EMPTY : fluid(tank.id, tank.amount);
        }

        @Override
        public int getTankCapacity(int index) {
            var tank = access.tank(index);
            return tank == null ? 0 : tank.capacity;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack resource) {
            return !resource.isEmpty()
                    && !resource.hasTag()
                    && access.canInsert(index, fluidId(resource));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return isFluidValid(0, resource)
                    ? access.insert(fluidId(resource), resource.getAmount(), action.simulate())
                    : 0;
        }

        @Override
        public FluidStack drain(FluidStack requested, FluidAction action) {
            return requested.isEmpty() || requested.hasTag()
                    ? FluidStack.EMPTY
                    : drain(fluidId(requested), requested.getAmount(), action);
        }

        @Override
        public FluidStack drain(int requested, FluidAction action) {
            var output = access.output();
            return output == null ? FluidStack.EMPTY : drain(output.id, requested, action);
        }

        private FluidStack drain(String id, int requested, FluidAction action) {
            return fluid(id, access.extract(id, requested, action.simulate()));
        }
    }
}
