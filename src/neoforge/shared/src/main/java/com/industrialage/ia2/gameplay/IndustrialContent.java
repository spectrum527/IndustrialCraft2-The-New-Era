package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.content.IndustrialMaterials;
import com.industrialage.ia2.content.PortableEquipment;
import com.industrialage.ia2.equipment.ArmorPlatform;
import com.industrialage.ia2.equipment.MeterPlatform;
import com.industrialage.ia2.equipment.MountPlatform;
import com.industrialage.ia2.platform.ConfigFacade;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers playable IC2 items, blocks, block entities, and their native menu. */
public final class IndustrialContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("ic2");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ic2");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "ic2");
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, "ic2");
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "ic2");
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "ic2");
    private static final ConfigFacade.Key<Double> RETAINED_EU =
            ActiveConfig.scalarKey("balance/energyRetainedInStorageBlockDrops", Double.class);
    private static ConfigFacade settings;

    static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> EU =
            COMPONENTS.register(
                    "eu",
                    () ->
                            DataComponentType.<Double>builder()
                                    .persistent(Codec.doubleRange(0, Double.MAX_VALUE))
                                    .networkSynchronized(ByteBufCodecs.DOUBLE)
                                    .build());

    static final List<MachineKind> MACHINE_KINDS = List.of(MachineKind.values());

    private static final List<? extends DeferredHolder<Block, ? extends Block>> MACHINE_BLOCKS =
            MACHINE_KINDS.stream()
                    .map(
                            kind ->
                                    block(
                                            kind.path,
                                            props -> MachineBlock.create(props, kind),
                                            IndustrialContent::metal))
                    .toList();

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialMachine>> MACHINE =
            ENTITIES.register(
                    "machine",
                    () ->
                            NativeGameplay.machineType(
                                    IndustrialMachine::new,
                                    MACHINE_BLOCKS.stream()
                                            .map(DeferredHolder::get)
                                            .toArray(Block[]::new)));

    /** Menu type shared by the machine screen and native server. */
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE_MENU =
            MENUS.register(
                    "machine",
                    () ->
                            new MenuType<>(
                                    (IContainerFactory<MachineMenu>) MachineMenu::new,
                                    FeatureFlags.DEFAULT_FLAGS));

    static final DeferredHolder<Item, Item> RESIN = ITEMS.registerSimpleItem("resin");

    static final DeferredHolder<Item, Item> TREETAP =
            NativeGameplay.item(
                    ITEMS,
                    "treetap",
                    RubberLogBlock.Treetap::new,
                    () -> new Item.Properties().durability(16));

    private IndustrialContent() {}

    /** Adds registrations and world listeners to their owning NeoForge buses. */
    public static void install(
            IEventBus modBus, IEventBus gameBus, ConfigFacade config, Dist dist) {
        settings = config;
        for (String path :
                List.of(
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
                        "splitter_cable")) {
            block(
                    path,
                    props -> CableBlock.create(props, path),
                    () -> BlockBehaviour.Properties.of().strength(0.2F).noOcclusion());
            String foam = path.substring(0, path.length() - "_cable".length()) + "_foam_cable";
            NativeGameplay.block(
                    BLOCKS,
                    foam,
                    props -> CableBlock.foamed(props, path),
                    BlockBehaviour.Properties::of);
        }
        block("machine", Block::new, IndustrialContent::metal);
        block("tin_ore", Block::new, () -> metal().strength(3, 3).sound(SoundType.STONE));
        block(
                "deepslate_tin_ore",
                Block::new,
                () -> metal().strength(4.5F, 3).sound(SoundType.DEEPSLATE));
        RubberLogBlock.register(BLOCKS, ITEMS);
        for (String path :
                List.of(
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
                        "coal_dust",
                        "iron_dust",
                        "bronze_dust",
                        "bronze_ingot")) {
            ITEMS.registerSimpleItem(path);
        }
        IndustrialMaterials.ADDITIONAL.forEach(ITEMS::registerSimpleItem);
        PortableGear.install(ITEMS, gameBus);
        ArmorPlatform.install(ITEMS, modBus, gameBus);
        MeterPlatform.install(ITEMS, MENUS, modBus, dist);
        MountPlatform.install(ITEMS, COMPONENTS, modBus, gameBus);
        FuelJetpack.install(modBus, ITEMS, COMPONENTS);
        JetpackControls.install(modBus, gameBus, dist);
        NeoLaser.install(modBus, COMPONENTS, dist);
        NeoSaber.install(modBus, gameBus, COMPONENTS, dist);
        BoatFleet.install(modBus, ITEMS);
        if (dist == Dist.CLIENT) NativeBoats.Client.install(modBus);
        NativeGameplay.item(
                ITEMS,
                "forge_hammer",
                NativeGameplay.CraftingTool::new,
                () -> new Item.Properties().durability(80));
        NativeGameplay.item(
                ITEMS,
                "cutter",
                NativeGameplay.CraftingTool::new,
                () -> new Item.Properties().durability(60));
        NativeGameplay.bronzeGear(modBus, ITEMS);
        TABS.register(
                "industrialcraft",
                () ->
                        CreativeModeTab.builder()
                                .title(Component.translatable("itemGroup.ic2"))
                                .icon(() -> new ItemStack(MACHINE_BLOCKS.getFirst().get()))
                                .displayItems(
                                        (parameters, output) -> {
                                            var entries = new java.util.LinkedHashSet<Item>();
                                            ITEMS.getEntries()
                                                    .forEach(holder -> entries.add(holder.get()));
                                            com.industrialage.ia2.world.WorldPlatform.forEachItem(
                                                    entries::add);
                                            var all =
                                                    net.minecraft.core.registries.BuiltInRegistries
                                                            .ITEM;
                                            all.stream()
                                                    .filter(
                                                            item ->
                                                                    all.getKey(item)
                                                                            .getNamespace()
                                                                            .equals("ic2"))
                                                    .sorted(
                                                            java.util.Comparator.comparing(
                                                                    all::getKey))
                                                    .forEach(entries::add);
                                            entries.forEach(output::accept);
                                        })
                                .build());
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
        NativeGameplay.registerAutomation(modBus, MACHINE);
        EnergyNetwork.install(gameBus, config);
    }

    /** Rotates a machine toward the selected face, or dismantles it while crouching. */
    public static InteractionResult wrenchBlock(UseOnContext context) {
        return wrenchBlock(context, false);
    }

    static InteractionResult wrenchBlock(UseOnContext context, boolean electric) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var player = context.getPlayer();
        if (!(state.getBlock() instanceof MachineBlock block) || player == null) {
            return InteractionResult.PASS;
        }
        if (!player.mayBuild() || !level.mayInteract(player, pos)) {
            return InteractionResult.FAIL;
        }
        Direction from = MachineBlock.facing(state);
        Direction face = context.getClickedFace();
        if (electric && player.isShiftKeyDown()) face = face.getOpposite();
        if (!block.kind.storage() && face.getAxis().isVertical()) face = from.getClockWise();
        boolean dismantle = electric ? face == from : player.isShiftKeyDown();
        int cost = dismantle ? 1_000 : PortableEquipment.ELECTRIC_WRENCH.cost;
        var tool = context.getItemInHand();
        if (electric && !player.isCreative() && PortableGear.stored(tool) < cost) {
            return InteractionResult.FAIL;
        }
        if (dismantle) {
            if (level instanceof ServerLevel server) {
                var drop = new ItemStack(block);
                if (server.getBlockEntity(pos) instanceof IndustrialMachine machine) {
                    retainOnDrop(machine, drop);
                }
                if (!server.destroyBlock(pos, false, player, Block.UPDATE_LIMIT)) {
                    return InteractionResult.FAIL;
                }
                if (!player.isCreative()) {
                    Block.popResource(server, pos, drop);
                    if (electric) PortableGear.spend(tool, cost);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (face == from) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            if (!server.setBlockAndUpdate(pos, state.setValue(block.directionProperty(), face))) {
                return InteractionResult.FAIL;
            }
            EnergyNetwork.invalidate(server);
            if (electric && !player.isCreative()) PortableGear.spend(tool, cost);
        }
        return InteractionResult.SUCCESS;
    }

    static void retainOnDrop(IndustrialMachine machine, ItemStack stack) {
        if (machine.kind().storage() && machine.energy() > 0) {
            double fraction = settings.current().map(values -> values.get(RETAINED_EU)).orElse(0.8);
            double retained = fraction * machine.energy();
            if (retained > 0) {
                stack.set(EU.get(), retained);
            }
        }
    }

    private static <T extends Block> DeferredHolder<Block, T> block(
            String path,
            java.util.function.Function<BlockBehaviour.Properties, T> factory,
            Supplier<BlockBehaviour.Properties> properties) {
        var holder = NativeGameplay.block(BLOCKS, path, factory, properties);
        ITEMS.registerSimpleBlockItem(holder);
        return holder;
    }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of()
                .strength(2, 10)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }
}
