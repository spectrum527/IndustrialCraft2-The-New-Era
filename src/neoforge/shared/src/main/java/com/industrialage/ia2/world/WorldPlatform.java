package com.industrialage.ia2.world;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WorldPlatform {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("ic2");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ic2");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "ic2");
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "ic2");
    private static final List<DeferredBlock<Block>> PLOTS = new ArrayList<>();
    private static final List<DeferredBlock<Block>> DEVICES = new ArrayList<>();
    private static final List<String> SUPPLIES =
            List.of(
                    "fertilizer",
                    "hydration_cell",
                    "weed_ex_cell",
                    "hops",
                    "bobs_yer_uncle_ranks_berry",
                    "coffee_beans",
                    "coffee_powder",
                    "grin_powder",
                    "milk_wart",
                    "oil_berry",
                    "terra_wart",
                    "weed",
                    "empty_mug",
                    "cold_coffee_mug",
                    "dark_coffee_mug",
                    "coffee_mug");
    private static final java.util.function.Supplier<DataComponentType<String>> GENOME =
            COMPONENTS.register(
                    "crop_genome",
                    () ->
                            DataComponentType.<String>builder()
                                    .persistent(Codec.STRING)
                                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                                    .build());
    private static final java.util.function.Supplier<DataComponentType<String>> UPGRADE_CONFIG =
            COMPONENTS.register(
                    "upgrade_settings",
                    () ->
                            DataComponentType.<String>builder()
                                    .persistent(Codec.STRING)
                                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                                    .build());
    private static final java.util.function.Supplier<DataComponentType<String>> SAMPLE =
            COMPONENTS.register(
                    "wall_sample",
                    () ->
                            DataComponentType.<String>builder()
                                    .persistent(Codec.STRING)
                                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                                    .build());
    private static final java.util.function.Supplier<DataComponentType<Integer>> FOAM =
            COMPONENTS.register(
                    "foam_mb",
                    () ->
                            DataComponentType.<Integer>builder()
                                    .persistent(Codec.intRange(0, 80_000))
                                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                                    .build());
    private static final java.util.function.Supplier<BlockEntityType<CropPlot>> PLOT =
            ENTITIES.register(
                    "crop_plot",
                    () ->
                            WorldNative.type(
                                    CropPlot::new,
                                    PLOTS.stream().map(DeferredBlock::get).toArray(Block[]::new)));
    private static final java.util.function.Supplier<BlockEntityType<WorldMachine>> MACHINE =
            ENTITIES.register(
                    "world_device",
                    () ->
                            WorldNative.type(
                                    WorldMachine::new,
                                    DEVICES.stream()
                                            .map(DeferredBlock::get)
                                            .toArray(Block[]::new)));

    private WorldPlatform() {}

    /** Registers agriculture, terrain materials, construction and world machines. */
    public static void install(IEventBus modBus, IEventBus gameBus, boolean client) {
        PLOTS.add(registerBlock("crop_stick", props -> plot(props, null), true));
        for (var crop : CropSpecies.values())
            PLOTS.add(registerBlock(crop.path(), props -> plot(props, crop), false));
        DEVICES.addAll(
                java.util.Arrays.stream(WorldMachineBlock.Kind.values())
                        .map(
                                kind ->
                                        registerBlock(
                                                kind.path,
                                                props ->
                                                        new WorldMachineBlock(
                                                                WorldBlocks.metal(props), kind),
                                                true))
                        .toList());
        registerItem("crop_seed_bag", CropSeed::new);
        for (String name : SUPPLIES) registerItem(name, props -> WorldNative.supply(name, props));
        WorldBlocks.install();
        WorldSigns.install(modBus, client);
        WorldMenus.install(modBus, client);
        com.industrialage.ia2.industrial.ToolMenus.install(modBus, client);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        WorldFluidAccess.install(modBus);
        gameBus.addListener(
                PlayerInteractEvent.RightClickBlock.class,
                event -> {
                    if (WorldInteractions.block(
                            event.getLevel(),
                            event.getEntity(),
                            event.getHand(),
                            event.getPos(),
                            event.getHitVec().getDirection())) {
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        event.setCanceled(true);
                    }
                });
        gameBus.addListener(
                PlayerInteractEvent.RightClickItem.class,
                event -> {
                    if (WorldInteractions.item(
                            event.getLevel(), event.getEntity(), event.getHand())) {
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        event.setCanceled(true);
                    }
                });
        WorldNative.registerTicketType(modBus);
        WorldNative.registerTools(gameBus);
        modBus.addListener(
                net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class,
                event -> event.enqueueWork(WorldPlatform::addCompost));
    }

    /** Adds the world item registrations to IC2's survival and construction tab. */
    public static void forEachItem(Consumer<Item> save) {
        ITEMS.getEntries()
                .forEach(
                        entry -> {
                            if (!entry.getId().getPath().equals("crop_seed_bag"))
                                save.accept(entry.get());
                        });
    }

    static DeferredBlock<Block> registerBlock(
            String path, Function<Block.Properties, Block> make, boolean hasItem) {
        var block = BLOCKS.registerBlock(path, make);
        if (hasItem) ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    static void registerItem(String path, Function<Item.Properties, Item> make) {
        ITEMS.registerItem(path, make);
    }

    private static void addCompost() {
        for (String product :
                List.of("weed", "hops", "coffee_beans", "oil_berry", "terra_wart", "milk_wart")) {
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(
                    WorldNative.item("ic2:" + product), 0.65F);
        }
    }

    private static Block plot(Block.Properties props, CropSpecies species) {
        return new CropBlock(
                WorldNative.noCollision(props)
                        .noOcclusion()
                        .strength(0.1F)
                        .sound(SoundType.CROP)
                        .randomTicks(),
                species);
    }

    static BlockEntityType<CropPlot> plotType() {
        return PLOT.get();
    }

    static BlockEntityType<WorldMachine> machineType() {
        return MACHINE.get();
    }

    static String genome(ItemStack stack) {
        return stack.getOrDefault(GENOME.get(), "");
    }

    static void setGenome(ItemStack stack, String genome) {
        stack.set(GENOME.get(), genome);
    }

    static String upgradeConfig(ItemStack stack) {
        return stack.getOrDefault(UPGRADE_CONFIG.get(), "");
    }

    static void setUpgradeConfig(ItemStack stack, String config) {
        stack.set(UPGRADE_CONFIG.get(), config);
    }

    static String sample(ItemStack stack) {
        return stack.getOrDefault(SAMPLE.get(), "");
    }

    static void setSample(ItemStack stack, String sample) {
        stack.set(SAMPLE.get(), sample);
    }

    static int foam(ItemStack stack) {
        return stack.getOrDefault(FOAM.get(), 0);
    }

    static void setFoam(ItemStack stack, int amount) {
        stack.set(FOAM.get(), amount);
    }
}
