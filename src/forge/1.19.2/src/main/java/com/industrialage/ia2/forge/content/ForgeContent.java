package com.industrialage.ia2.forge.content;

import com.industrialage.ia2.content.IndustrialMaterials;
import com.industrialage.ia2.forge.client.ForgeClient;
import com.industrialage.ia2.platform.ConfigFacade;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registers the native blocks, items, block entities and menu for the early industrial route. */
public final class ForgeContent {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "ic2");
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "ic2");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "ic2");
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, "ic2");
    private static final Map<MachineKind, RegistryObject<Block>> MACHINES =
            new EnumMap<>(MachineKind.class);

    private static final Map<MachineKind, RegistryObject<BlockEntityType<StorageDevice>>> STORAGES =
            new EnumMap<>(MachineKind.class);
    private static final List<RegistryObject<Block>> CABLES = new ArrayList<>();

    static final CreativeModeTab TAB =
            new CreativeModeTab("ic2") {
                @Override
                public ItemStack makeIcon() {
                    return new ItemStack(MACHINES.get(MachineKind.MACERATOR).get());
                }
            };

    static final RegistryObject<Item> RESIN = item("resin", () -> new Item(itemProperties()));
    static final RegistryObject<Item> BATTERY = item("re_battery", BatteryItem::new);

    static final RegistryObject<BlockEntityType<GeneratorDevice>> GENERATOR =
            ENTITIES.register(
                    "generator",
                    () ->
                            BlockEntityType.Builder.of(
                                            GeneratorDevice::new,
                                            MACHINES.get(MachineKind.GENERATOR).get())
                                    .build(null));
    static final RegistryObject<BlockEntityType<ProcessingDevice>> PROCESSOR =
            ENTITIES.register(
                    "processing_machine",
                    () ->
                            BlockEntityType.Builder.of(
                                            ProcessingDevice::new,
                                            MACHINES.entrySet().stream()
                                                    .filter(entry -> entry.getKey().processes())
                                                    .map(entry -> entry.getValue().get())
                                                    .toArray(Block[]::new))
                                    .build(null));
    static final RegistryObject<BlockEntityType<CableBlock.Conductor>> CONDUCTOR =
            ENTITIES.register(
                    "cable",
                    () ->
                            BlockEntityType.Builder.of(
                                            CableBlock.Conductor::new,
                                            CABLES.stream()
                                                    .map(RegistryObject::get)
                                                    .toArray(Block[]::new))
                                    .build(null));
    static final RegistryObject<MenuType<MachineMenu>> MACHINE_MENU =
            MENUS.register("machine", () -> IForgeMenuType.create(MachineMenu::fromNetwork));

    private ForgeContent() {}

    /** Wires the production registrations and server tick handlers into this mod container. */
    public static void register(IEventBus modBus, IEventBus gameBus, ConfigFacade config) {
        for (var kind : MachineKind.values()) {
            var registered =
                    block(
                            kind.path,
                            () ->
                                    kind.storesEnergy()
                                            ? (kind.chargingPad()
                                                    ? new MachineBlock.ChargePad(kind)
                                                    : new MachineBlock.Storage(kind))
                                            : new MachineBlock.Working(kind));
            MACHINES.put(kind, registered);
            if (kind.storesEnergy()) {
                STORAGES.put(
                        kind,
                        ENTITIES.register(
                                kind.path,
                                () ->
                                        BlockEntityType.Builder.of(
                                                        StorageDevice::new, registered.get())
                                                .build(null)));
            }
        }
        for (String path :
                new String[] {
                    "copper_cable",
                    "insulated_copper_cable",
                    "tin_cable",
                    "insulated_tin_cable",
                    "gold_cable",
                    "insulated_gold_cable",
                    "double_insulated_gold_cable",
                    "iron_cable",
                    "insulated_iron_cable",
                    "double_insulated_iron_cable",
                    "triple_insulated_iron_cable",
                    "glass_fibre_cable",
                    "detector_cable",
                    "splitter_cable"
                }) {
            boolean specialized = path.equals("detector_cable") || path.equals("splitter_cable");
            CABLES.add(
                    block(
                            path,
                            () ->
                                    specialized
                                            ? new CableBlock.Special(path)
                                            : CableBlock.plain(path)));
            String foam = path.substring(0, path.length() - "_cable".length()) + "_foam_cable";
            CABLES.add(BLOCKS.register(foam, () -> CableBlock.foamed(path)));
        }
        for (String path :
                new String[] {
                    "raw_tin",
                    "tin_ingot",
                    "rubber",
                    "iron_plate",
                    "copper_plate",
                    "tin_plate",
                    "tin_casing",
                    "circuit",
                    "crushed_tin",
                    "crushed_copper",
                    "bronze_dust",
                    "bronze_ingot",
                    "coal_dust",
                    "iron_dust"
                }) {
            item(path, () -> new Item(itemProperties()));
        }
        for (String path : IndustrialMaterials.ADDITIONAL) {
            item(path, () -> new Item(itemProperties()));
        }
        block("machine", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
        block(
                "tin_ore",
                () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_ORE)));
        block(
                "deepslate_tin_ore",
                () ->
                        new DropExperienceBlock(
                                BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_IRON_ORE)));
        block("rubber_log", RubberTree.Log::new);
        block(
                "rubber_leaves",
                () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));
        block("rubber_sapling", RubberTree::sapling);
        item("treetap", RubberTree.Tap::new);
        item("forge_hammer", () -> new CraftingTool(80));
        item("cutter", () -> new CraftingTool(60));
        armor("helmet", EquipmentSlot.HEAD);
        armor("chestplate", EquipmentSlot.CHEST);
        armor("leggings", EquipmentSlot.LEGS);
        armor("boots", EquipmentSlot.FEET);
        BLOCKS.register(modBus);
        com.industrialage.ia2.equipment.MeterPlatform.install(ITEMS, MENUS, modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        MENUS.register(modBus);
        ForgePower.register(gameBus, config);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClient.registerContent(modBus);
        }
    }

    /** Uses a wrench to rotate a machine, or remove it while sneaking. */
    public static InteractionResult wrenchBlock(UseOnContext context) {
        return wrenchBlock(context, true);
    }

    /** Applies a wrench interaction with optional mechanical durability wear. */
    public static InteractionResult wrenchBlock(UseOnContext context, boolean mechanical) {
        var state = context.getLevel().getBlockState(context.getClickedPos());
        return state.getBlock() instanceof MachineBlock block
                ? block.wrench(state, context, mechanical)
                : InteractionResult.PASS;
    }

    /** Reuses the mechanical tap's resin and hit-face rules with an EU cost. */
    public static InteractionResult electricTap(UseOnContext context, int cost) {
        return RubberTree.Tap.tap(context, cost);
    }

    /** Returns the production machine menu type. */
    public static MenuType<MachineMenu> machineMenu() {
        return MACHINE_MENU.get();
    }

    /** The rechargeable battery item used for the client charge predicate. */
    public static Item battery() {
        return BATTERY.get();
    }

    /** Returns the rechargeable battery's stored fraction for its charge model. */
    public static float batteryCharge(ItemStack stack) {
        return (float) (BatteryItem.energy(stack) / BatteryItem.CAPACITY);
    }

    static BlockEntityType<StorageDevice> storageType(MachineKind kind) {
        return STORAGES.get(kind).get();
    }

    /** Returns the item properties used by the mod's creative-tab inventory. */
    public static Item.Properties itemProperties() {
        return new Item.Properties().tab(TAB);
    }

    private static RegistryObject<Block> block(String path, Supplier<? extends Block> factory) {
        RegistryObject<Block> block = BLOCKS.register(path, factory::get);
        item(path, () -> new BlockItem(block.get(), itemProperties()));
        return block;
    }

    private static RegistryObject<Item> item(String path, Supplier<? extends Item> factory) {
        return ITEMS.register(path, () -> factory.get());
    }

    private static void armor(String piece, EquipmentSlot slot) {
        item("bronze_" + piece, () -> new ArmorItem(CraftingTool.BRONZE, slot, itemProperties()));
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("ic2", path);
    }
}
