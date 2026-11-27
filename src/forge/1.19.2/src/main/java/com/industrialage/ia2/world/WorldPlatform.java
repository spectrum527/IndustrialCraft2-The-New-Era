package com.industrialage.ia2.world;

import com.industrialage.ia2.forge.content.ForgeContent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class WorldPlatform {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "ic2");
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "ic2");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "ic2");
    private static final List<RegistryObject<Block>> PLOTS = new ArrayList<>();
    private static final List<RegistryObject<Block>> DEVICES = new ArrayList<>();
    private static final List<String> SUPPLIES =
            List.of(
                    "fertilizer",
                    "hydration_cell",
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
    private static final RegistryObject<BlockEntityType<CropPlot>> PLOT =
            ENTITIES.register(
                    "crop_plot",
                    () ->
                            WorldNative.type(
                                    CropPlot::new,
                                    PLOTS.stream().map(RegistryObject::get).toArray(Block[]::new)));
    private static final RegistryObject<BlockEntityType<WorldMachine>> MACHINE =
            ENTITIES.register(
                    "world_device",
                    () ->
                            WorldNative.type(
                                    WorldMachine::new,
                                    DEVICES.stream()
                                            .map(RegistryObject::get)
                                            .toArray(Block[]::new)));

    private WorldPlatform() {}

    /** Registers agriculture, terrain materials, construction and world machines. */
    public static void install(IEventBus modBus, IEventBus gameBus) {
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
        WorldSigns.install(modBus);
        WorldMenus.install(modBus);
        com.industrialage.ia2.industrial.ToolMenus.install(modBus);
        gameBus.addListener(ForgeLootAdditions::appendToChest);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        gameBus.addListener(
                (PlayerInteractEvent.RightClickBlock event) -> {
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
                (PlayerInteractEvent.RightClickItem event) -> {
                    if (WorldInteractions.item(
                            event.getLevel(), event.getEntity(), event.getHand())) {
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        event.setCanceled(true);
                    }
                });
        WorldNative.registerTicketType(modBus);
        WorldNative.registerTools(gameBus);
        modBus.addListener(
                (net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                        event.enqueueWork(WorldPlatform::addCompost));
    }

    static RegistryObject<Block> registerBlock(
            String path, Function<Block.Properties, Block> make, boolean hasItem) {
        var block = BLOCKS.register(path, () -> make.apply(WorldNative.properties(path)));
        if (hasItem) registerItem(path, properties -> new BlockItem(block.get(), properties));
        return block;
    }

    static void registerItem(String path, Function<Item.Properties, Item> make) {
        ITEMS.register(path, () -> make.apply(ForgeContent.itemProperties(path)));
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

    static java.util.function.Supplier<
                    BlockEntityType<net.minecraft.world.level.block.entity.SignBlockEntity>>
            registerSignType(
                    java.util.function.Supplier<
                                    BlockEntityType<
                                            net.minecraft.world.level.block.entity.SignBlockEntity>>
                            factory) {
        return ENTITIES.register("rubber_sign", factory);
    }

    static BlockEntityType<CropPlot> plotType() {
        return PLOT.get();
    }

    static BlockEntityType<WorldMachine> machineType() {
        return MACHINE.get();
    }
}
