package com.industrialage.ia2.worldtest;

import com.industrialage.ia2.power.WorldPowerPort;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Black-box player and native world checks using the installed loaders' survival entry points. */
public final class WorldChecks {
    private static final BlockPos ORIGIN = new BlockPos(70, 95, 70);
    private static final BlockPos POTATO = ORIGIN;
    private static final BlockPos EMPTY = ORIGIN.south(2);
    private static final BlockPos CROSS = ORIGIN.south(4);
    private static final BlockPos HARVESTER = ORIGIN.east(16);
    private static final BlockPos HARVEST_FIELD = HARVESTER.east();
    private static final BlockPos MATRON = ORIGIN.east(26);
    private static final BlockPos CLASSIC = MATRON.east(10);
    private static final BlockPos MINER = ORIGIN.west(9).south(12);
    private static final BlockPos WET_MINER = ORIGIN.west(15).south(12);
    private static final BlockPos MINING_PIPE_WATER = ORIGIN.west(15).south(2);
    private static final BlockPos MINING_PIPE_FLOW = MINING_PIPE_WATER.south(4);
    private static final BlockPos ADVANCED = ORIGIN.east(13).south(13);
    private static final BlockPos FOAM = ORIGIN.west(6);
    private static final BlockPos TERRAFORMER = ORIGIN.east(64).south(64);
    private static final BlockPos BATCH = ORIGIN.west(7).south(19);
    private static final BlockPos BUFFER = BATCH.east(5);
    private static final BlockPos SORTER = BUFFER.east(6);
    private static final BlockPos DISTRIBUTOR = SORTER.east(6);
    private static final BlockPos CHUNK_LOADER = new BlockPos(181, 95, 86);
    private static final BlockPos UPGRADE_MILL = ORIGIN.south(17).east(20);
    private static final BlockPos ROUTED_MILL = UPGRADE_MILL.east(7);

    private final Function<String, Block> blocks;
    private final Function<String, Item> items;
    private final Charge charge;
    private final BiConsumer<String, Boolean> check;
    private final RandomSource random = RandomSource.create(365673L);
    private ProcessorMeter processorMeter;
    private int dischargeSlot, outputSlot;
    private String legacyDurationPath;
    private boolean changedSpeed;
    private boolean checkedDrain;
    private int tickSpeedChanged;
    private int euBeforeSpeedChange;
    private int firstBatchTick;
    private boolean batchRetemplated;
    private boolean poweredChunkObserved;
    private boolean wetMinerDescended;
    private boolean wetMinerHalfWithdrawn;
    private boolean wetMinerWithdrawn;

    /** Creates a native check using version-specific registry access. */
    public WorldChecks(
            Function<String, Block> blocks,
            Function<String, Item> items,
            Charge charge,
            BiConsumer<String, Boolean> check) {
        this.blocks = blocks;
        this.items = items;
        this.charge = charge;
        this.check = check;
    }

    /** Supplies the slot indexes and synchronized native readings for this release. */
    public WorldChecks(
            Function<String, Block> blocks,
            Function<String, Item> items,
            Charge charge,
            BiConsumer<String, Boolean> check,
            ProcessorMeter processorMeter,
            int dischargeSlot,
            int outputSlot,
            String legacyDurationPath) {
        this(blocks, items, charge, check);
        this.processorMeter = processorMeter;
        this.dischargeSlot = dischargeSlot;
        this.outputSlot = outputSlot;
        this.legacyDurationPath = legacyDurationPath;
    }

    /** Reads a real processor's native menu data. */
    @FunctionalInterface
    public interface ProcessorMeter {
        /** Samples paid progress, duration and EU from the installed host menu. */
        ProcessorValues read(net.minecraft.world.inventory.AbstractContainerMenu menu);
    }

    /** Native progress and EU values used by the processor regression. */
    public record ProcessorValues(int progress, int duration, int energy, int capacity) {}

    /** Fills an actual portable item using the production charging helper. */
    @FunctionalInterface
    public interface Charge {
        /** Transfers up to the accepted amount of power into the stack. */
        double fill(ItemStack stack, double amount, int tier);
    }

    /** Loads the physical test plots, places the sticks, and starts the EU machines. */
    public void setup(ServerLevel level, ServerPlayer player) {
        for (int x = 3; x <= 8; x++) {
            for (int z = 4; z <= 5; z++) {
                level.setChunkForced(x, z, true);
                level.getChunk(x, z);
            }
        }
        for (int x = 7; x <= 9; x++) {
            for (int z = 7; z <= 9; z++) {
                level.setChunkForced(x, z, true);
                level.getChunk(x, z);
            }
        }
        floor(level, POTATO);
        floor(level, CROSS);
        floor(level, CROSS.east());
        floor(level, CROSS.west());
        set(level, EMPTY.below(), Blocks.STONE);
        var rejected = stack("crop_stick");
        click(level, player, EMPTY.below(), Direction.UP, rejected);
        verify("sticks-require-farmland", !rejected.isEmpty() && level.isEmptyBlock(EMPTY));

        placeStick(level, player, POTATO);
        var planting = new ItemStack(Items.POTATO);
        click(level, player, POTATO, Direction.UP, planting);
        verify(
                "potato-planting",
                level.getBlockState(POTATO).is(block("potato_crop")) && planting.isEmpty());
        var fertilizer = stack("fertilizer");
        click(level, player, POTATO, Direction.UP, fertilizer);
        verify("manual-fertilizer", fertilizer.isEmpty());
        click(level, player, POTATO, Direction.UP, new ItemStack(Items.WATER_BUCKET));
        verify("manual-irrigation-returns-bucket", player.getMainHandItem().is(Items.BUCKET));

        placeStick(level, player, CROSS);
        click(level, player, CROSS, Direction.UP, stack("crop_stick"));
        verify("crossed-sticks", Boolean.parseBoolean(value(level, CROSS, "crossed")));
        var weedEx = stack("weed_ex_cell");
        click(level, player, CROSS, Direction.UP, weedEx);
        verify(
                "weed-protection-returns-container",
                player.getMainHandItem().is(items.apply("fluid_cell")));
        placeCrop(level, CROSS.east(), "wheat_crop", 7);
        placeCrop(level, CROSS.west(), "reed_crop", 2);
        for (int i = 0; i < 30 && level.getBlockState(CROSS).is(block("crop_stick")); i++)
            tickPlot(level, CROSS);
        verify(
                "cross-breeding",
                !level.getBlockState(CROSS).is(block("crop_stick"))
                        && !level.getBlockState(CROSS).is(block("weed_crop")));

        foam(level, player);
        placeCrop(level, HARVEST_FIELD, "wheat_crop", 7);
        set(level, HARVESTER, block("crop_harvester"));
        set(level, HARVESTER.west(2), block("solar_generator"));
        set(level, HARVESTER.west(), block("tin_foam_cable"));
        var harvester = device(level, player, HARVESTER);
        verify(
                "harvester-output-protected",
                !harvester.slots.get(0).mayPlace(new ItemStack(Items.STONE)));

        set(level, MATRON, block("cropmatron"));
        placeCrop(level, MATRON.east(), "beetroots_crop", 0);
        var matron = device(level, player, MATRON);
        matron.slots.get(0).set(stack("fertilizer"));
        matron.slots.get(1).set(new ItemStack(Items.WATER_BUCKET));
        var waterSource = MATRON.north();
        set(level, waterSource, block("iron_tank"));
        click(level, player, waterSource, Direction.NORTH, new ItemStack(Items.WATER_BUCKET));
        verify("matron-fluid-pull-source-fills", player.getMainHandItem().is(Items.BUCKET));
        var liquidUpgrade = stack("fluid_pulling_upgrade");
        player.setShiftKeyDown(true);
        click(level, player, MATRON, Direction.NORTH, liquidUpgrade);
        player.setShiftKeyDown(false);
        verify(
                "experimental-matron-fluid-upgrade-slot",
                matron.slots.get(9).mayPlace(liquidUpgrade));
        matron.slots.get(9).set(liquidUpgrade);
        port(level, MATRON).receive(900);
        set(level, CLASSIC, block("classic_cropmatron"));
        placeCrop(level, CLASSIC.east(), "carrots_crop", 0);
        var classic = device(level, player, CLASSIC);
        classic.slots.get(0).set(stack("fertilizer"));
        classic.slots.get(1).set(stack("hydration_cell"));
        port(level, CLASSIC).receive(900);

        set(level, MINER, block("miner"));
        set(level, MINER.below(), Blocks.IRON_ORE);
        set(level, MINER.below(2), Blocks.BEDROCK);
        var miner = device(level, player, MINER);
        miner.slots.get(0).set(stack("drill"));
        miner.slots.get(2).set(stack("mining_pipe"));
        port(level, MINER).receive(1000);
        setupMiningPipeWaterlogging(level, player);

        set(level, ADVANCED, block("advanced_miner"));
        set(level, ADVANCED.below(), Blocks.IRON_ORE);
        set(level, ADVANCED.below().east(), Blocks.DIAMOND_ORE);
        set(level, ADVANCED.west(), Blocks.CHEST);
        var advanced = device(level, player, ADVANCED);
        advanced.slots.get(0).set(stack("advanced_scanner"));
        advanced.slots.get(1).set(new ItemStack(Items.RAW_IRON));
        set(level, ADVANCED.north(), Blocks.REDSTONE_BLOCK);
        var inverter = stack("redstone_inverter_upgrade");
        verify("advanced-miner-allows-redstone-inverter", advanced.slots.get(9).mayPlace(inverter));
        advanced.slots.get(9).set(inverter);
        port(level, ADVANCED).receive(4000);

        set(level, TERRAFORMER, block("terraformer"));
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (x != 0 || z != 0) set(level, TERRAFORMER.offset(x, -1, z), Blocks.GRASS_BLOCK);
            }
        }
        device(level, player, TERRAFORMER).slots.get(0).set(stack("mushroom_tfbp"));
        port(level, TERRAFORMER).receive(8000);
        previewWoodwork(level, player);
        prepareAutomation(level, player);
        prepareProcessors(level, player);
        checkAdvancedUpgradeSettings(player);
    }

    private void previewWoodwork(ServerLevel level, ServerPlayer player) {
        var sign = ORIGIN.west(7).south(7);
        set(level, sign.below(), block("rubber_planks"));
        click(level, player, sign.below(), Direction.UP, stack("rubber_sign"));
        verify(
                "native-rubber-sign",
                level.getBlockEntity(sign)
                        instanceof net.minecraft.world.level.block.entity.SignBlockEntity);
        var wall = sign.west(2);
        set(level, wall, Blocks.STONE);
        click(level, player, wall, Direction.WEST, stack("rubber_sign"));
        verify(
                "native-rubber-wall-sign",
                level.getBlockState(wall.west()).is(block("rubber_wall_sign"))
                        && level.getBlockEntity(wall.west())
                                instanceof net.minecraft.world.level.block.entity.SignBlockEntity);
    }

    private void checkMiningLoot(ServerLevel level, ServerPlayer player) {
        var ore = ORIGIN.east(3).north(3);
        var tool = new ItemStack(Items.IRON_PICKAXE);
        set(level, ore, block("lead_ore"));
        player.setPos(ore.getX() + 0.5, ore.getY(), ore.getZ() + 2.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var lead = level.getBlockState(ore);
        verify("iron-pickaxe-valid-for-lead", tool.isCorrectToolForDrops(lead));
        var leadLoot = Block.getDrops(lead, level, ore, null, player, tool);
        verify(
                "lead-ore-loot-is-raw-lead",
                leadLoot.size() == 1 && leadLoot.get(0).is(items.apply("raw_lead")));
        player.gameMode.destroyBlock(ore);
        verify("lead-ore-mines-to-raw-lead", countDrops(level, ore, items.apply("raw_lead")) == 1);
        set(level, ore, block("deepslate_uranium_ore"));
        var uranium = level.getBlockState(ore);
        var uraniumLoot = Block.getDrops(uranium, level, ore, null, player, tool);
        verify(
                "deepslate-uranium-loot-is-raw-uranium",
                uraniumLoot.size() == 1 && uraniumLoot.get(0).is(items.apply("raw_uranium")));
        player.gameMode.destroyBlock(ore);
        verify(
                "deepslate-uranium-mines-to-raw-uranium",
                countDrops(level, ore, items.apply("raw_uranium")) == 1);
    }

    /** Verifies the natural ore and basalt factories on the chosen survival seed. */
    public void natural(ServerLevel level) {
        for (String id :
                List.of(
                        "lead_ore",
                        "deepslate_lead_ore",
                        "uranium_ore",
                        "deepslate_uranium_ore",
                        "basalt")) {
            boolean found = false;
            for (int x = -2; x <= 3 && !found; x++) {
                for (int z = -2; z <= 3 && !found; z++) {
                    for (var section : level.getChunk(x, z).getSections()) {
                        if (section.maybeHas(state -> state.is(block(id)))) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            verify("naturally-generated-" + id, found);
        }
    }

    private void prepareAutomation(ServerLevel level, ServerPlayer player) {
        set(level, BATCH, block("batch_crafter"));
        var batch = device(level, player, BATCH);
        batch.slots.get(0).set(new ItemStack(Items.OAK_LOG));
        batch.slots.get(18).set(new ItemStack(Items.OAK_LOG));
        port(level, BATCH).receive(4000);
        verify(
                "batch-output-protected",
                !batch.slots.get(9).mayPlace(new ItemStack(Items.OAK_LOG)));

        set(level, BUFFER, block("item_buffer"));
        level.setBlockAndUpdate(
                BUFFER,
                level.getBlockState(BUFFER)
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        set(level, BUFFER.east(), Blocks.CHEST);
        var buffer = device(level, player, BUFFER);
        buffer.slots.get(0).set(new ItemStack(Items.COBBLESTONE, 16));
        buffer.slots.get(26).set(stack("ejector_upgrade"));

        set(level, SORTER, block("sorting_machine"));
        set(level, SORTER.east(), Blocks.CHEST);
        set(level, SORTER.below(), Blocks.CHEST);
        var sorter = device(level, player, SORTER);
        checkPageTransfer(sorter, player, SORTER, false);
        sorter.slots.get(0).set(new ItemStack(Items.DIAMOND, 4));
        sorter.slots.get(1).set(new ItemStack(Items.COBBLESTONE, 8));
        sorter.slots.get(18 + Direction.EAST.ordinal()).set(new ItemStack(Items.DIAMOND));
        port(level, SORTER).receive(4000);

        set(level, DISTRIBUTOR, block("weighted_item_distributor"));
        set(level, DISTRIBUTOR.east(), Blocks.CHEST);
        set(level, DISTRIBUTOR.below(), Blocks.CHEST);
        var chest = chest(level, DISTRIBUTOR.east());
        for (int i = 0; i < chest.getContainerSize(); i++) {
            chest.setItem(
                    i,
                    new ItemStack(Items.COBBLESTONE, i + 1 == chest.getContainerSize() ? 60 : 64));
        }
        var distributor = device(level, player, DISTRIBUTOR);
        checkPageTransfer(distributor, player, DISTRIBUTOR, true);
        distributor.slots.get(0).set(new ItemStack(Items.COBBLESTONE, 8));
        distributor.slots.get(18 + Direction.EAST.ordinal()).set(new ItemStack(Items.PAPER, 2));
        distributor.slots.get(18 + Direction.DOWN.ordinal()).set(new ItemStack(Items.PAPER));

        level.setChunkForced(CHUNK_LOADER.getX() >> 4, CHUNK_LOADER.getZ() >> 4, true);
        set(level, CHUNK_LOADER, block("chunk_loader"));
        player.setShiftKeyDown(true);
        for (int i = 0; i < 3; i++)
            click(level, player, CHUNK_LOADER, Direction.EAST, ItemStack.EMPTY);
        player.setShiftKeyDown(false);
        port(level, CHUNK_LOADER).receive(18_000);
    }

    /** Samples the live batch recipe, then verifies changing configuration without a restart. */
    public boolean automationTick(ServerLevel level, ServerPlayer player, int tick) {
        processorTick(level, player, tick);
        var batch = device(level, player, BATCH);
        if (firstBatchTick == 0 && !batch.slots.get(9).getItem().isEmpty()) {
            var produced = batch.slots.get(9).getItem();
            verify(
                    "batch-makes-four-planks",
                    produced.is(Items.OAK_PLANKS)
                            && produced.getCount() == 4
                            && batch.slots.get(0).getItem().isEmpty());
            batch.slots.get(9).set(ItemStack.EMPTY);
            batch.slots.get(18).set(ItemStack.EMPTY);
            batch.slots.get(4).set(new ItemStack(Items.OAK_PLANKS));
            batch.slots.get(7).set(new ItemStack(Items.OAK_PLANKS));
            firstBatchTick = tick;
        }
        if (!batchRetemplated && firstBatchTick > 0 && tick - firstBatchTick >= 40) {
            verify(
                    "batch-pause-after-clearing-template",
                    batch.slots.get(9).getItem().isEmpty()
                            && batch.slots.get(7).getItem().is(Items.OAK_PLANKS));
            batch.slots.get(22).set(new ItemStack(Items.OAK_PLANKS));
            batch.slots.get(25).set(new ItemStack(Items.OAK_PLANKS));
            batchRetemplated = true;
        }
        int x = (CHUNK_LOADER.getX() >> 4) + 4;
        int z = CHUNK_LOADER.getZ() >> 4;
        boolean loaded = level.getChunkSource().getChunkNow(x, z) != null;
        if (!poweredChunkObserved && loaded) {
            verify("powered-loader-holds-selected-chunk", true);
            poweredChunkObserved = true;
            set(level, CHUNK_LOADER.west(), Blocks.REDSTONE_BLOCK);
        }
        tickWetMiner(level, player);
        return batchRetemplated
                && !batch.slots.get(9).getItem().isEmpty()
                && poweredChunkObserved
                && !loaded
                && wetMinerWithdrawn
                && (processorMeter == null
                        || checkedDrain
                                && processorMeter.read(device(level, player, UPGRADE_MILL)).energy()
                                        == 2);
    }

    /** Samples the native tick outcomes after energy, crops and terrain have updated. */
    public void checkOutcomes(ServerLevel level, ServerPlayer player) {
        checkMiningLoot(level, player);
        for (int i = 0; i < 120 && age(level, POTATO) < 3; i++) tickPlot(level, POTATO);
        verify("potato-grew", age(level, POTATO) == 3);
        click(level, player, POTATO, Direction.UP, ItemStack.EMPTY);
        verify(
                "right-click-pick-potato",
                countDrops(level, POTATO, Items.POTATO) >= 1 && age(level, POTATO) < 3);
        for (int i = 0; i < 120 && age(level, POTATO) < 3; i++) tickPlot(level, POTATO);
        level.destroyBlock(POTATO, true, player);
        var bag =
                level.getEntitiesOfClass(ItemEntity.class, new AABB(POTATO).inflate(2)).stream()
                        .map(ItemEntity::getItem)
                        .filter(stack -> stack.is(items.apply("crop_seed_bag")))
                        .findFirst()
                        .orElseThrow()
                        .copy();
        var analyzer = stack("cropnalyzer");
        for (int i = 0; i < 10; i++) charge.fill(analyzer, 100, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, analyzer);
        player.setItemInHand(InteractionHand.OFF_HAND, bag);
        for (int i = 0; i < 4; i++)
            player.gameMode.useItem(player, level, analyzer, InteractionHand.MAIN_HAND);
        bag = player.getOffhandItem();
        verify(
                "seed-analysis",
                bag.getHoverName().getContents()
                                instanceof
                                net.minecraft.network.chat.contents.TranslatableContents name
                        && name.getKey().equals("item.ic2.crop_seed_bag.identified"));
        placeStick(level, player, POTATO);
        click(level, player, POTATO, Direction.UP, bag);
        verify("genetic-seed-replanted", level.getBlockState(POTATO).is(block("potato_crop")));
        placeStick(level, player, EMPTY.east());
        level.setBlockAndUpdate(EMPTY.east().below(), Blocks.STONE.defaultBlockState());
        tickPlot(level, EMPTY.east());
        verify("soil-loss-cleans-plot", level.isEmptyBlock(EMPTY.east()));

        verify(
                "foam-line-powers-harvester",
                device(level, player, HARVESTER).slots.get(0).getItem().is(Items.WHEAT)
                        && age(level, HARVEST_FIELD) < 7);
        verify(
                "matron-uses-fluid-and-fertilizer",
                device(level, player, MATRON).slots.get(0).getItem().isEmpty()
                        && device(level, player, MATRON).slots.get(3).getItem().is(Items.BUCKET));
        click(level, player, MATRON.north(), Direction.NORTH, new ItemStack(Items.BUCKET));
        verify("matron-pulls-water-through-native-port", player.getMainHandItem().is(Items.BUCKET));
        verify(
                "classic-matron-returns-cells",
                device(level, player, CLASSIC).slots.get(0).getItem().isEmpty()
                        && device(level, player, CLASSIC)
                                .slots
                                .get(3)
                                .getItem()
                                .is(items.apply("fluid_cell")));
        verify(
                "miner-drills-ore-and-halts-at-bedrock",
                level.getBlockState(MINER.below()).is(block("mining_pipe_tip"))
                        && device(level, player, MINER).slots.get(3).getItem().is(Items.RAW_IRON)
                        && level.getBlockState(MINER.below(2)).is(Blocks.BEDROCK));
        checkMiningPipeWaterloggingOutcomes(level);
        verify(
                "advanced-miner-filters-and-ejects",
                level.isEmptyBlock(ADVANCED.below())
                        && level.getBlockState(ADVANCED.below().east()).is(Blocks.DIAMOND_ORE)
                        && ((net.minecraft.world.Container) level.getBlockEntity(ADVANCED.west()))
                                .getItem(0)
                                .is(Items.RAW_IRON));
        checkAutomation(level, player);
        verify(
                "mushroom-blueprint-changes-terrain",
                BlockPos.betweenClosedStream(
                                TERRAFORMER.offset(-3, -1, -3), TERRAFORMER.offset(3, -1, 3))
                        .anyMatch(pos -> level.getBlockState(pos).is(Blocks.MYCELIUM)));
        var blueprint = device(level, player, TERRAFORMER).slots.get(0).getItem();
        verify(
                "terraformer-retains-its-blueprint-after-working",
                blueprint.is(stack("mushroom_tfbp").getItem()) && blueprint.getCount() == 1);
    }

    private void prepareProcessors(ServerLevel level, ServerPlayer player) {
        if (processorMeter == null) return;
        set(level, UPGRADE_MILL, block("macerator"));
        var menu = device(level, player, UPGRADE_MILL);
        var inventory = (net.minecraft.world.WorldlyContainer) menu.slots.get(0).container;
        verify(
                "four-native-upgrade-slots",
                menu.slots.get(3).mayPlace(stack("overclocker_upgrade"))
                        && menu.slots.get(6).mayPlace(stack("energy_storage_upgrade"))
                        && !menu.slots.get(3).mayPlace(stack("fluid_pulling_upgrade")));
        for (Direction side : Direction.values()) {
            verify(
                    "processor-exposes-input-output-and-battery-" + side.getName(),
                    java.util.Arrays.stream(inventory.getSlotsForFace(side))
                                    .boxed()
                                    .collect(java.util.stream.Collectors.toSet())
                                    .equals(java.util.Set.of(0, outputSlot, dischargeSlot))
                            && !inventory.canTakeItemThroughFace(0, new ItemStack(Items.COAL), side)
                            && !inventory.canTakeItemThroughFace(
                                    dischargeSlot, new ItemStack(Items.REDSTONE), side));
        }
        menu.slots.get(0).set(new ItemStack(Items.COAL, 2));
        menu.slots.get(dischargeSlot).set(new ItemStack(Items.REDSTONE));
        set(level, ROUTED_MILL, block("macerator"));
        set(level, ROUTED_MILL.north(), Blocks.CHEST);
        set(level, ROUTED_MILL.east(), Blocks.CHEST);
        set(level, ROUTED_MILL.south(), Blocks.CHEST);
        var supply = chest(level, ROUTED_MILL.north());
        supply.setItem(0, new ItemStack(items.apply("raw_tin")));
        supply.setItem(1, new ItemStack(Items.COAL));
        var pull = stack("advanced_pulling_upgrade");
        var eject = stack("ejector_upgrade");
        player.setShiftKeyDown(true);
        click(level, player, ROUTED_MILL, Direction.NORTH, pull);
        click(level, player, ROUTED_MILL, Direction.EAST, eject);
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, pull);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COAL));
        player.gameMode.useItem(player, level, pull, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        var route = device(level, player, ROUTED_MILL);
        route.slots.get(3).set(pull);
        route.slots.get(4).set(eject);
        route.slots.get(5).set(stack("overclocker_upgrade"));
        route.slots.get(dischargeSlot).set(new ItemStack(Items.REDSTONE));
    }

    private void processorTick(ServerLevel level, ServerPlayer player, int tick) {
        if (processorMeter == null || checkedDrain) return;
        var menu = device(level, player, UPGRADE_MILL);
        var state = processorMeter.read(menu);
        if (changedSpeed && tick == tickSpeedChanged + 1) {
            verify(
                    "upgraded-processor-pays-three-eu-per-tick",
                    state.progress() == 106 && state.energy() == euBeforeSpeedChange - 3);
            checkedDrain = true;
        }
        if (!changedSpeed && state.progress() == 150) {
            euBeforeSpeedChange = state.energy();
            var at = UPGRADE_MILL;
            var server = level.getServer();
            server.getCommands()
                    .performPrefixedCommand(
                            server.createCommandSourceStack().withSuppressedOutput(),
                            "data remove block "
                                    + at.getX()
                                    + " "
                                    + at.getY()
                                    + " "
                                    + at.getZ()
                                    + " "
                                    + legacyDurationPath);
            menu = device(level, player, UPGRADE_MILL);
            var legacy = processorMeter.read(menu);
            verify(
                    "legacy-processor-duration-field-optional",
                    legacy.duration() == 300
                            && legacy.progress() == 150
                            && legacy.energy() == euBeforeSpeedChange);
            menu.slots.get(3).set(stack("overclocker_upgrade"));
            state = processorMeter.read(menu);
            verify(
                    "upgrade-preserves-paid-progress",
                    state.progress() == 105
                            && state.duration() == 210
                            && state.capacity() == 1230
                            && state.energy() == euBeforeSpeedChange);
            tickSpeedChanged = tick;
            changedSpeed = true;
        }
    }

    /** Reads the powered upgrade and genetic crop from a newly started physical server. */
    public void cold(ServerLevel level, ServerPlayer player) {
        level.getChunk(UPGRADE_MILL.getX() >> 4, UPGRADE_MILL.getZ() >> 4);
        level.getChunk(POTATO.getX() >> 4, POTATO.getZ() >> 4);
        var menu = device(level, player, UPGRADE_MILL);
        var values = processorMeter.read(menu);
        verify(
                "upgraded-processor-retains-eu-and-progress-on-restart",
                values.duration() == 210
                        && values.capacity() == 1230
                        && values.energy() == 2
                        && values.progress() == 61
                        && menu.slots.get(3).getItem().is(items.apply("overclocker_upgrade"))
                        && menu.slots.get(outputSlot).getItem().is(items.apply("coal_dust")));
        verify(
                "potato-and-genetics-reload",
                level.getBlockState(POTATO).is(block("potato_crop"))
                        && level.getBlockEntity(POTATO) != null);
    }

    private void checkAutomation(ServerLevel level, ServerPlayer player) {
        if (processorMeter != null) {
            verify("processor-observed-upgrade", checkedDrain);
            var north = chest(level, ROUTED_MILL.north());
            var east = chest(level, ROUTED_MILL.east());
            var south = chest(level, ROUTED_MILL.south());
            verify(
                    "processor-advanced-pull-and-selected-eject",
                    north.getItem(0).is(items.apply("raw_tin"))
                            && north.getItem(1).isEmpty()
                            && east.getItem(0).is(items.apply("coal_dust"))
                            && east.getItem(0).getCount() == 1
                            && south.isEmpty());
        }
        var batch = device(level, player, BATCH);
        verify(
                "batch-accepts-live-template-change",
                batch.slots.get(9).getItem().is(Items.STICK)
                        && batch.slots.get(9).getItem().getCount() == 4
                        && batch.slots.get(4).getItem().isEmpty());
        verify(
                "buffer-ejects-with-upgrade",
                chest(level, BUFFER.east()).getItem(0).getCount() == 16
                        && device(level, player, BUFFER).slots.get(0).getItem().isEmpty());
        var diamond = chest(level, SORTER.east()).getItem(0);
        var unlabeled = chest(level, SORTER.below()).getItem(0);
        verify(
                "sorter-filters-east-and-defaults-down",
                diamond.is(Items.DIAMOND)
                        && diamond.getCount() == 4
                        && unlabeled.is(Items.COBBLESTONE)
                        && unlabeled.getCount() == 8);
        var primary = chest(level, DISTRIBUTOR.east());
        verify(
                "weighted-partial-fallback",
                primary.getItem(primary.getContainerSize() - 1).getCount() == 64
                        && chest(level, DISTRIBUTOR.below()).getItem(0).getCount() == 4
                        && device(level, player, DISTRIBUTOR).slots.get(0).getItem().isEmpty());
        int x = (CHUNK_LOADER.getX() >> 4) + 4;
        int z = CHUNK_LOADER.getZ() >> 4;
        verify("chunk-released-after-shutdown", level.getChunkSource().getChunkNow(x, z) == null);
    }

    private static net.minecraft.world.Container chest(ServerLevel level, BlockPos pos) {
        return (net.minecraft.world.Container) level.getBlockEntity(pos);
    }

    private void foam(ServerLevel level, ServerPlayer player) {
        set(level, FOAM, Blocks.STONE);
        var sprayer = stack("foam_sprayer");
        player.setItemInHand(InteractionHand.OFF_HAND, stack("construction_foam_bucket"));
        click(level, player, FOAM, Direction.UP, sprayer);
        verify("sprayer-loads-foam-bucket", player.getOffhandItem().is(Items.BUCKET));
        player.setShiftKeyDown(true);
        click(level, player, FOAM, Direction.UP, sprayer);
        verify(
                "single-block-foam-mode",
                level.getBlockState(FOAM.above()).is(block("foam"))
                        && level.isEmptyBlock(FOAM.above().north())
                        && level.isEmptyBlock(FOAM.above().east()));
        click(level, player, FOAM.above(), Direction.NORTH, new ItemStack(Items.SAND));
        verify("foam-cures-with-sand", level.getBlockState(FOAM.above()).is(block("white_wall")));
        click(level, player, FOAM.above(), Direction.NORTH, stack("blue_painter"));
        verify("wall-recolors", level.getBlockState(FOAM.above()).is(block("blue_wall")));
        var obscurator = stack("obscurator");
        for (int i = 0; i < 5; i++) charge.fill(obscurator, 250, 2);
        player.setShiftKeyDown(true);
        click(level, player, FOAM, Direction.UP, obscurator);
        player.setShiftKeyDown(false);
        click(level, player, FOAM.above(), Direction.UP, obscurator);
        verify(
                "charged-obscurator-camouflages-wall",
                level.getBlockState(FOAM.above()).is(block("obscured_wall"))
                        && "stone".equalsIgnoreCase(value(level, FOAM.above(), "material")));
        var waterSheet = stack("water_sheet");
        click(level, player, FOAM.above(), Direction.UP, waterSheet);
        verify(
                "water-sheet-covers-hardened-foam",
                waterSheet.isEmpty()
                        && "water".equalsIgnoreCase(value(level, FOAM.above(), "material")));
        var lavaSheet = stack("lava_sheet");
        click(level, player, FOAM.above(), Direction.UP, lavaSheet);
        verify(
                "lava-sheet-covers-hardened-foam",
                lavaSheet.isEmpty()
                        && "lava".equalsIgnoreCase(value(level, FOAM.above(), "material")));
        player.setShiftKeyDown(true);

        var scaffold = FOAM.east(2);
        set(level, scaffold.below(), Blocks.STONE);
        set(level, scaffold, block("wooden_scaffold"));
        click(level, player, scaffold, Direction.EAST, stack("iron_fence"));
        verify(
                "scaffold-reinforces",
                level.getBlockState(scaffold).is(block("reinforced_wooden_scaffold")));
        click(level, player, scaffold, Direction.EAST, sprayer);
        click(level, player, scaffold, Direction.EAST, new ItemStack(Items.SAND));
        verify(
                "reinforced-foam-hardens",
                level.getBlockState(scaffold).is(block("reinforced_stone")));
        var cable = FOAM.west(2);
        set(level, cable, block("copper_cable"));
        click(level, player, cable, Direction.UP, sprayer);
        verify("sprayer-encloses-cable", level.getBlockState(cable).is(block("copper_foam_cable")));
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.OFF_HAND, stack("construction_foam_bucket"));
        click(level, player, FOAM, Direction.NORTH, sprayer);
        click(level, player, FOAM, Direction.NORTH, sprayer);
        long area =
                BlockPos.betweenClosedStream(FOAM.offset(-6, -6, -1), FOAM.offset(6, 6, -1))
                        .filter(pos -> level.getBlockState(pos).is(block("foam")))
                        .count();
        verify("sprayer-flood-fills-ten", area == 10);
    }

    private void checkAdvancedUpgradeSettings(ServerPlayer player) {
        ItemStack previous = player.getMainHandItem();
        ItemStack upgrade = stack("advanced_pulling_upgrade");
        player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
        try {
            Class<?> type = Class.forName("com.industrialage.ia2.world.UpgradeMenu");
            var constructor =
                    type.getDeclaredConstructor(
                            net.minecraft.world.inventory.MenuType.class,
                            int.class,
                            net.minecraft.world.entity.player.Inventory.class,
                            net.minecraft.world.entity.player.Player.class,
                            InteractionHand.class,
                            ItemStack.class);
            constructor.setAccessible(true);
            var menu =
                    (net.minecraft.world.inventory.AbstractContainerMenu)
                            constructor.newInstance(
                                    null,
                                    91,
                                    player.getInventory(),
                                    player,
                                    InteractionHand.MAIN_HAND,
                                    upgrade);
            ItemStack coal = new ItemStack(Items.COAL, 2);
            menu.setCarried(coal);
            menu.clicked(0, 0, ClickType.PICKUP, player);
            menu.setCarried(stack("raw_tin"));
            menu.clicked(1, 0, ClickType.PICKUP, player);
            var settings =
                    com.industrialage.ia2.world.MachineAutomation.class.getDeclaredMethod(
                            "settings", ItemStack.class);
            settings.setAccessible(true);
            Object rule = settings.invoke(null, upgrade);
            var matches = rule.getClass().getDeclaredMethod("matches", ItemStack.class);
            matches.setAccessible(true);
            boolean multiple =
                    (boolean) matches.invoke(rule, new ItemStack(Items.COAL))
                            && (boolean) matches.invoke(rule, stack("raw_tin"))
                            && !(boolean) matches.invoke(rule, new ItemStack(Items.DIAMOND))
                            && coal.getCount() == 2;
            menu.clickMenuButton(player, 2);
            rule = settings.invoke(null, upgrade);
            boolean deny =
                    !(boolean) matches.invoke(rule, new ItemStack(Items.COAL))
                            && (boolean) matches.invoke(rule, new ItemStack(Items.DIAMOND));
            menu.clickMenuButton(player, 2);
            ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
            worn.setDamageValue(5);
            ItemStack other = worn.copy();
            other.setDamageValue(6);
            menu.setCarried(worn);
            menu.clicked(2, 0, ClickType.PICKUP, player);
            menu.clickMenuButton(player, 0);
            rule = settings.invoke(null, upgrade);
            boolean damage =
                    (boolean) matches.invoke(rule, worn) && !(boolean) matches.invoke(rule, other);
            var battery = stack("re_battery");
            var different = stack("re_battery");
            for (int i = 0; i < 2; i++) charge.fill(battery, 50, 2);
            for (int i = 0; i < 4; i++) charge.fill(different, 50, 2);
            menu.setCarried(battery);
            menu.clicked(3, 0, ClickType.PICKUP, player);
            menu.clickMenuButton(player, 1);
            rule = settings.invoke(null, upgrade);
            boolean energy =
                    (boolean) matches.invoke(rule, battery)
                            && !(boolean) matches.invoke(rule, different);
            Item[] extra = {
                Items.IRON_INGOT,
                Items.GOLD_INGOT,
                Items.LAPIS_LAZULI,
                Items.REDSTONE,
                Items.EMERALD
            };
            for (int i = 0; i < extra.length; i++) {
                menu.setCarried(new ItemStack(extra[i]));
                menu.clicked(4 + i, 0, ClickType.PICKUP, player);
            }
            for (int i = 0; i < 3; i++) menu.clickMenuButton(player, 3);
            var reopened =
                    (net.minecraft.world.inventory.AbstractContainerMenu)
                            constructor.newInstance(
                                    null,
                                    92,
                                    player.getInventory(),
                                    player,
                                    InteractionHand.MAIN_HAND,
                                    upgrade);
            boolean persisted =
                    reopened.slots.get(0).getItem().is(Items.COAL)
                            && reopened.slots.get(1).getItem().is(items.apply("raw_tin"))
                            && com.industrialage.ia2.world.MachineAutomation.faces(upgrade)
                                    .equals(List.of(Direction.NORTH));
            for (int i = 0; i < extra.length; i++)
                persisted &= reopened.slots.get(4 + i).getItem().is(extra[i]);
            persisted &=
                    reopened.slots.get(2).getItem().is(Items.IRON_PICKAXE)
                            && reopened.slots.get(3).getItem().is(items.apply("re_battery"));
            verify("advanced-upgrade-nine-samples-do-not-consume", multiple && persisted);
            verify("advanced-upgrade-exclusion", deny);
            verify("advanced-upgrade-damage-match", damage);
            verify("advanced-upgrade-energy-match", energy);
            verify("advanced-upgrade-reopens-with-filters-and-face", persisted);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }
    }

    private void checkPageTransfer(
            net.minecraft.world.inventory.AbstractContainerMenu menu,
            ServerPlayer player,
            BlockPos pos,
            boolean weighted) {
        var position = player.position();
        var saved = player.getInventory().getItem(9).copy();
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        menu.slots.get(0).set(new ItemStack(Items.COBBLESTONE, 3));
        menu.slots.get(9).set(new ItemStack(Items.COBBLESTONE, 7));
        player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 5));
        boolean first =
                menu.clickMenuButton(player, 0)
                        && menu.slots.get(0).isActive()
                        && !menu.slots.get(9).isActive();
        menu.quickMoveStack(player, 27);
        first &=
                menu.slots.get(0).getItem().getCount() == 8
                        && menu.slots.get(9).getItem().getCount() == 7
                        && player.getInventory().getItem(9).isEmpty();
        player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 4));
        boolean second =
                menu.clickMenuButton(player, 1)
                        && !menu.slots.get(0).isActive()
                        && menu.slots.get(9).isActive();
        menu.quickMoveStack(player, 27);
        second &=
                menu.slots.get(0).getItem().getCount() == 8
                        && menu.slots.get(9).getItem().getCount() == 11
                        && player.getInventory().getItem(9).isEmpty();
        if (weighted)
            second &=
                    menu.clickMenuButton(player, 2)
                            && menu.slots.get(18).isActive()
                            && menu.slots.get(25).isActive()
                            && !menu.slots.get(0).isActive()
                            && !menu.slots.get(9).isActive();
        verify((weighted ? "distributor" : "sorter") + "-page-shift-first", first);
        verify((weighted ? "distributor" : "sorter") + "-page-shift-second", second);
        menu.slots.get(0).set(ItemStack.EMPTY);
        menu.slots.get(9).set(ItemStack.EMPTY);
        menu.clickMenuButton(player, 0);
        player.getInventory().setItem(9, saved);
        player.setPos(position.x, position.y, position.z);
    }

    private void placeStick(ServerLevel level, ServerPlayer player, BlockPos pos) {
        floor(level, pos);
        click(level, player, pos.below(), Direction.UP, stack("crop_stick"));
        if (!level.getBlockState(pos).is(block("crop_stick")))
            throw new AssertionError("Crop stick not placed at " + pos);
    }

    private void placeCrop(ServerLevel level, BlockPos pos, String name, int age) {
        floor(level, pos);
        level.setBlockAndUpdate(
                pos, block(name).defaultBlockState().setValue(BlockStateProperties.AGE_7, age));
    }

    private void floor(ServerLevel level, BlockPos pos) {
        set(level, pos.below(2), Blocks.DIRT);
        level.setBlockAndUpdate(
                pos.below(),
                Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
        set(level, pos.below().north(), Blocks.WATER);
    }

    private void tickPlot(ServerLevel level, BlockPos pos) {
        level.getBlockState(pos).randomTick(level, pos, random);
    }

    private static net.minecraft.world.inventory.AbstractContainerMenu device(
            ServerLevel level, ServerPlayer player, BlockPos pos) {
        return ((MenuProvider) level.getBlockEntity(pos))
                .createMenu(7, player.getInventory(), player);
    }

    private static WorldPowerPort port(ServerLevel level, BlockPos pos) {
        return (WorldPowerPort) level.getBlockEntity(pos);
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlockAndUpdate(pos, block.defaultBlockState());
    }

    private static int age(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.hasProperty(BlockStateProperties.AGE_7)
                ? state.getValue(BlockStateProperties.AGE_7)
                : -1;
    }

    private static String value(ServerLevel level, BlockPos pos, String property) {
        var state = level.getBlockState(pos);
        var member = state.getBlock().getStateDefinition().getProperty(property);
        return member == null ? "" : state.getValue(member).toString();
    }

    private static int countDrops(ServerLevel level, BlockPos pos, Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static void click(
            ServerLevel level, ServerPlayer player, BlockPos pos, Direction side, ItemStack held) {
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(pos)
                                .add(
                                        side.getStepX() / 2.0,
                                        side.getStepY() / 2.0,
                                        side.getStepZ() / 2.0),
                        side,
                        pos,
                        false);
        player.gameMode.useItemOn(player, level, held, InteractionHand.MAIN_HAND, hit);
    }

    private Block block(String path) {
        return blocks.apply(path);
    }

    private ItemStack stack(String path) {
        return new ItemStack(items.apply(path));
    }

    private void verify(String name, boolean passed) {
        check.accept(name, passed);
    }

    private void setupMiningPipeWaterlogging(ServerLevel level, ServerPlayer player) {
        var pipe = block("mining_pipe");
        var tip = block("mining_pipe_tip");
        verify(
                "mining-pipes-implement-waterlogged",
                pipe instanceof SimpleWaterloggedBlock
                        && tip instanceof SimpleWaterloggedBlock
                        && !pipe.defaultBlockState().getValue(BlockStateProperties.WATERLOGGED)
                        && !tip.defaultBlockState().getValue(BlockStateProperties.WATERLOGGED)
                        && pipe.defaultBlockState().getFluidState().isEmpty()
                        && tip.defaultBlockState().getFluidState().isEmpty()
                        && pipe.defaultBlockState()
                                .setValue(BlockStateProperties.WATERLOGGED, true)
                                .getFluidState()
                                .isSource()
                        && tip.defaultBlockState()
                                .setValue(BlockStateProperties.WATERLOGGED, true)
                                .getFluidState()
                                .isSource());

        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(level, MINING_PIPE_WATER.offset(dx, -1, dz), Blocks.STONE);
                boolean wall = dx == -1 || dx == 2 || dz == -1 || dz == 1;
                set(
                        level,
                        MINING_PIPE_WATER.offset(dx, 0, dz),
                        wall || dx == 1 ? Blocks.STONE : Blocks.AIR);
            }
        }
        set(level, MINING_PIPE_WATER, Blocks.WATER);
        var held = stack("mining_pipe");
        player.setPos(
                MINING_PIPE_WATER.getX() + 0.5,
                MINING_PIPE_WATER.getY() + 1,
                MINING_PIPE_WATER.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(MINING_PIPE_WATER.below()).add(0, 0.5, 0),
                        Direction.UP,
                        MINING_PIPE_WATER.below(),
                        false);
        var placed =
                ((BlockItem) held.getItem())
                        .place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, held, hit));
        var placedState = level.getBlockState(MINING_PIPE_WATER);
        verify(
                "mining-pipe-placed-in-water",
                placed.consumesAction()
                        && placedState.is(pipe)
                        && placedState.getValue(BlockStateProperties.WATERLOGGED)
                        && level.getFluidState(MINING_PIPE_WATER).isSource()
                        && level.getFluidState(MINING_PIPE_WATER).getType() == Fluids.WATER);
        var scooped =
                ((SimpleWaterloggedBlock) pipe).pickupBlock(level, MINING_PIPE_WATER, placedState);
        verify(
                "mining-pipe-bucket-unwaterlogs",
                scooped.is(Items.WATER_BUCKET)
                        && !level.getBlockState(MINING_PIPE_WATER)
                                .getValue(BlockStateProperties.WATERLOGGED)
                        && level.getFluidState(MINING_PIPE_WATER).isEmpty());
        verify(
                "mining-pipe-refill-waterlog",
                ((SimpleWaterloggedBlock) pipe)
                                .placeLiquid(
                                        level,
                                        MINING_PIPE_WATER,
                                        level.getBlockState(MINING_PIPE_WATER),
                                        Fluids.WATER.getSource(false))
                        && level.getBlockState(MINING_PIPE_WATER)
                                .getValue(BlockStateProperties.WATERLOGGED));
        set(level, MINING_PIPE_WATER.east(), Blocks.AIR);
        verify(
                "mining-pipe-update-shape-schedules-water-tick",
                level.getFluidTicks().hasScheduledTick(MINING_PIPE_WATER, Fluids.WATER));

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 1; dz++) {
                set(level, MINING_PIPE_FLOW.offset(dx, -1, dz), Blocks.STONE);
                boolean wall = dx == -2 || dx == 2 || dz == -2 || dz == 1;
                set(level, MINING_PIPE_FLOW.offset(dx, 0, dz), wall ? Blocks.STONE : Blocks.AIR);
            }
        }
        set(level, MINING_PIPE_FLOW.east(), pipe);
        set(level, MINING_PIPE_FLOW.west(), tip);
        set(level, MINING_PIPE_FLOW, Blocks.WATER);

        for (int depth = 1; depth <= 2; depth++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0)
                        set(level, WET_MINER.offset(dx, -depth, dz), Blocks.STONE);
                }
            }
        }
        set(level, WET_MINER.below(3), Blocks.BEDROCK);
        set(level, WET_MINER.below(), Blocks.WATER);
        set(level, WET_MINER.below(2), Blocks.WATER);
        set(level, WET_MINER.north(), Blocks.AIR);
        set(level, WET_MINER, block("miner"));
        var wetMiner = device(level, player, WET_MINER);
        wetMiner.slots.get(0).set(stack("drill"));
        wetMiner.slots.get(2).set(new ItemStack(items.apply("mining_pipe"), 2));
        port(level, WET_MINER).receive(2000);
    }

    private void tickWetMiner(ServerLevel level, ServerPlayer player) {
        var pipe = block("mining_pipe");
        var tip = block("mining_pipe_tip");
        if (!wetMinerDescended) {
            var lower = level.getBlockState(WET_MINER.below(2));
            if (lower.is(tip)) {
                var upper = level.getBlockState(WET_MINER.below());
                verify(
                        "miner-preserves-waterlogging-when-descending",
                        upper.is(pipe)
                                && upper.getValue(BlockStateProperties.WATERLOGGED)
                                && level.getFluidState(WET_MINER.below()).isSource()
                                && lower.getValue(BlockStateProperties.WATERLOGGED)
                                && level.getFluidState(WET_MINER.below(2)).isSource());
                wetMinerDescended = true;
                set(level, WET_MINER.north(), Blocks.REDSTONE_BLOCK);
            }
            return;
        }
        if (!wetMinerHalfWithdrawn) {
            var lower = level.getBlockState(WET_MINER.below(2));
            if (lower.is(Blocks.WATER)) {
                var upper = level.getBlockState(WET_MINER.below());
                verify(
                        "miner-preserves-waterlogging-during-withdrawal",
                        level.getFluidState(WET_MINER.below(2)).isSource()
                                && upper.is(tip)
                                && upper.getValue(BlockStateProperties.WATERLOGGED)
                                && level.getFluidState(WET_MINER.below()).isSource());
                wetMinerHalfWithdrawn = true;
            }
            return;
        }
        if (!wetMinerWithdrawn && level.getBlockState(WET_MINER.below()).is(Blocks.WATER)) {
            var menu = device(level, player, WET_MINER);
            int recovered = 0;
            for (int i = 0; i < 8 && i < menu.slots.size(); i++) {
                var item = menu.slots.get(i).getItem();
                if (item.is(items.apply("mining_pipe"))) recovered += item.getCount();
            }
            verify(
                    "miner-restores-water-source-on-full-withdrawal",
                    level.getFluidState(WET_MINER.below()).isSource()
                            && level.getFluidState(WET_MINER.below(2)).isSource()
                            && recovered == 2);
            wetMinerWithdrawn = true;
        }
    }

    private void checkMiningPipeWaterloggingOutcomes(ServerLevel level) {
        var pipe = block("mining_pipe");
        var tip = block("mining_pipe_tip");
        verify(
                "waterlogged-mining-pipe-spreads-water",
                level.getFluidState(MINING_PIPE_WATER.east()).getType() == Fluids.FLOWING_WATER);
        var east = level.getBlockState(MINING_PIPE_FLOW.east());
        var west = level.getBlockState(MINING_PIPE_FLOW.west());
        verify(
                "flowing-water-preserves-mining-pipes",
                level.getFluidState(MINING_PIPE_FLOW.north()).getType() == Fluids.FLOWING_WATER
                        && east.is(pipe)
                        && !east.getValue(BlockStateProperties.WATERLOGGED)
                        && west.is(tip)
                        && !west.getValue(BlockStateProperties.WATERLOGGED));
        verify(
                "destroying-waterlogged-mining-pipe-leaves-water",
                level.destroyBlock(MINING_PIPE_WATER, false)
                        && level.getBlockState(MINING_PIPE_WATER).is(Blocks.WATER)
                        && level.getFluidState(MINING_PIPE_WATER).isSource());
    }
}
