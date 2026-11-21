package com.industrialage.ia2.gametest;

import com.industrialage.ia2.gameplay.IndustrialMachine;
import com.industrialage.ia2.industrial.WorkshopNode;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;

/** Extra workshop cases using placed devices, the shipped JAR, and the native transfer API. */
final class WorkshopChecks {
    private static final BlockPos FIRE = new BlockPos(19, 113, 20);
    private static final BlockPos FORMER = FIRE.east(2);
    private static final BlockPos WASHER = FIRE.south(2);
    private static final BlockPos POWER = FIRE.south(5);
    private static final BlockPos HEATER = POWER.east(2);
    private static final BlockPos FURNACE = HEATER.east();
    private static final BlockPos BOTTLER = new BlockPos(29, 113, 20);
    private static final BlockPos ELECTROLYZER = new BlockPos(29, 114, 27);
    private static final BlockPos FERMENTER = new BlockPos(23, 114, 29);
    private static final BlockPos SCRAP = new BlockPos(28, 121, 27);
    private static final BlockPos SCRAP_HAND = new BlockPos(20, 121, 28);
    private static final BlockPos BIO_HEATER = FERMENTER.west();
    private final FluidCellChecks fluidCells = new FluidCellChecks();
    private final CellMachineChecks cellMachines = new CellMachineChecks();
    private int elapsed;
    private boolean advancedCold;
    private boolean fermentationVerified;
    private boolean scrapVerified;
    private boolean handOpened;
    private boolean handVerified;
    private boolean complete;

    void setup(ServerLevel world, BiConsumer<String, Boolean> check) {
        int chunkX = FIRE.getX() >> 4;
        int chunkZ = FIRE.getZ() >> 4;
        world.setChunkForced(chunkX, chunkZ, true);
        world.getChunk(chunkX, chunkZ);
        var player = FakePlayerFactory.getMinecraft(world);
        fluidCells.setup(world, check);
        cellMachines.setup(world, check);
        var scrapBox = NativeGameplayFixture.item("scrap_box");
        world.setBlockAndUpdate(SCRAP_HAND.below(), Blocks.STONE.defaultBlockState());
        world.setBlockAndUpdate(
                SCRAP,
                Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.UP));
        if (world.getBlockEntity(SCRAP) instanceof DispenserBlockEntity dispenser) {
            dispenser.setItem(0, new ItemStack(scrapBox, 2));
            world.setBlockAndUpdate(SCRAP.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        }
        NativeGameplayFixture.place(world, player, "generator", FIRE);
        NativeGameplayFixture.place(world, player, "insulated_copper_cable", FIRE.east());
        NativeGameplayFixture.place(world, player, "metal_former", FORMER);
        NativeGameplayFixture.place(world, player, "ore_washing_plant", WASHER);
        var source = (IndustrialMachine) world.getBlockEntity(FIRE);
        source.setItem(0, new ItemStack(Items.COAL));
        check.accept(
                "native-metal-former-accepts-ingot",
                NativeTransferAccess.insert(
                                world,
                                FORMER,
                                Direction.UP,
                                new ItemStack(Items.COPPER_INGOT),
                                false)
                        == 1);
        var washer = (WorkshopNode) world.getBlockEntity(WASHER);
        check.accept("native-ore-washer-receives-eu", washer.receive(16_000) >= 8_000);
        check.accept(
                "native-ore-washer-accepts-crushed-copper",
                NativeTransferAccess.insert(
                                world,
                                WASHER,
                                Direction.UP,
                                new ItemStack(NativeGameplayFixture.item("crushed_copper")),
                                false)
                        == 1);

        var charged = new ItemStack(NativeGameplayFixture.item("batbox"));
        charged.set(energyComponent(), 36_000.0);
        NativeGameplayFixture.place(world, player, "batbox", POWER, charged);
        NativeGameplayFixture.place(world, player, "insulated_copper_cable", POWER.east());
        NativeGameplayFixture.place(world, player, "electric_heat_generator", HEATER);
        NativeGameplayFixture.place(world, player, "blast_furnace", FURNACE);
        world.setBlockAndUpdate(
                HEATER,
                world.getBlockState(HEATER).setValue(BlockStateProperties.FACING, Direction.EAST));
        world.setBlockAndUpdate(
                FURNACE,
                world.getBlockState(FURNACE)
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        var heater = (WorkshopNode) world.getBlockEntity(HEATER);
        var menu = heater.createMenu(1, player.getInventory(), player);
        var coils = new ItemStack(NativeGameplayFixture.item("coil"), 10);
        check.accept("native-heat-generator-accepts-coils", menu.getSlot(4).mayPlace(coils));
        menu.getSlot(4).set(coils);
        check.accept(
                "native-blast-furnace-accepts-iron",
                NativeTransferAccess.insert(
                                world,
                                FURNACE,
                                Direction.UP,
                                new ItemStack(Items.IRON_INGOT),
                                false)
                        == 1);
        var air =
                BuiltInRegistries.FLUID.stream()
                        .filter(
                                fluid ->
                                        BuiltInRegistries.FLUID
                                                .getKey(fluid)
                                                .toString()
                                                .equals("ic2:air"))
                        .findFirst()
                        .orElseThrow();
        check.accept(
                "native-blast-furnace-filled-with-6000-mb-air",
                NativeTransferAccess.fill(
                                world, FURNACE, Direction.NORTH, new FluidStack(air, 6000), false)
                        == 6000);
        NativeGameplayFixture.place(world, player, "fluid_bottler", BOTTLER);
        var bottler = (WorkshopNode) world.getBlockEntity(BOTTLER);
        check.accept("native-bottler-receives-400-eu", bottler.receive(400) == 400);
        check.accept(
                "native-bottler-accepts-two-empty-cells",
                NativeTransferAccess.insert(
                                world,
                                BOTTLER,
                                Direction.UP,
                                new ItemStack(NativeGameplayFixture.item("empty_cell"), 2),
                                false)
                        == 2);
        check.accept(
                "native-bottler-accepts-2000-mb-water",
                NativeTransferAccess.fill(
                                world,
                                BOTTLER,
                                Direction.NORTH,
                                new FluidStack(Fluids.WATER, 2000),
                                false)
                        == 2000);

        NativeGameplayFixture.place(world, player, "electrolyzer", ELECTROLYZER);
        var tank = NativeGameplayFixture.block("bronze_tank").defaultBlockState();
        world.setBlockAndUpdate(ELECTROLYZER.above(), tank);
        world.setBlockAndUpdate(ELECTROLYZER.below(), tank);
        var electrolyzer = (WorkshopNode) world.getBlockEntity(ELECTROLYZER);
        check.accept("native-electrolyzer-receives-2560-eu", electrolyzer.receive(2560) == 2560);
        check.accept(
                "native-electrolyzer-accepts-water",
                NativeTransferAccess.fill(
                                world,
                                ELECTROLYZER,
                                Direction.NORTH,
                                new FluidStack(Fluids.WATER, 40),
                                false)
                        == 40);
        check.accept(
                "native-electrolyzer-rejects-lava",
                NativeTransferAccess.fill(
                                world,
                                ELECTROLYZER,
                                Direction.NORTH,
                                new FluidStack(Fluids.LAVA, 40),
                                true)
                        == 0);

        NativeGameplayFixture.place(world, player, "solid_heat_generator", BIO_HEATER);
        NativeGameplayFixture.place(world, player, "fermenter", FERMENTER);
        NativeGameplayFixture.place(world, player, "bronze_tank", FERMENTER.east());
        world.setBlockAndUpdate(
                BIO_HEATER,
                world.getBlockState(BIO_HEATER)
                        .setValue(BlockStateProperties.FACING, Direction.EAST));
        world.setBlockAndUpdate(
                FERMENTER,
                world.getBlockState(FERMENTER)
                        .setValue(BlockStateProperties.FACING, Direction.WEST));
        check.accept(
                "native-fermentation-heater-accepts-coal",
                NativeTransferAccess.insert(
                                world,
                                BIO_HEATER,
                                Direction.UP,
                                new ItemStack(Items.COAL, 4),
                                false)
                        == 4);
        var biomass =
                BuiltInRegistries.FLUID.stream()
                        .filter(
                                fluid ->
                                        BuiltInRegistries.FLUID
                                                .getKey(fluid)
                                                .toString()
                                                .equals("ic2:biomass"))
                        .findFirst()
                        .orElseThrow();
        check.accept(
                "native-fermenter-accepts-500-mb-biomass",
                NativeTransferAccess.fill(
                                world,
                                FERMENTER,
                                Direction.NORTH,
                                new FluidStack(biomass, 500),
                                false)
                        == 500);
        universalCell(world, player, check);
    }

    private void universalCell(
            ServerLevel world,
            net.minecraft.server.level.ServerPlayer player,
            BiConsumer<String, Boolean> check) {
        var tank = new BlockPos(27, 118, 27);
        world.setBlockAndUpdate(
                tank, NativeGameplayFixture.block("bronze_tank").defaultBlockState());
        var water = new FluidStack(Fluids.WATER, 257);
        check.accept(
                "tank-accepted-irregular-fluid-amount",
                NativeTransferAccess.fill(world, tank, Direction.WEST, water, false) == 257);
        var fluidCell = new ItemStack(NativeGameplayFixture.item("fluid_cell"));
        player.setPos(tank.getX() - 2, tank.getY(), tank.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, fluidCell);
        var hit = new BlockHitResult(Vec3.atCenterOf(tank), Direction.WEST, tank, false);
        player.gameMode.useItemOn(player, world, fluidCell, InteractionHand.MAIN_HAND, hit);
        var held = player.getItemInHand(InteractionHand.MAIN_HAND);
        check.accept(
                "universal-cell-actual-partial-fill",
                held.is(fluidCell.getItem())
                        && NativeTransferAccess.cellVolume(held) == 257
                        && NativeTransferAccess.volume(world, tank, Direction.WEST) == 0);
        player.gameMode.useItemOn(player, world, held, InteractionHand.MAIN_HAND, hit);
        check.accept(
                "universal-cell-returned-exact-partial-fill",
                NativeTransferAccess.cellVolume(player.getItemInHand(InteractionHand.MAIN_HAND))
                                == 0
                        && NativeTransferAccess.volume(world, tank, Direction.WEST) == 257);
        var source = new BlockPos(29, 119, 29);
        world.setBlockAndUpdate(source.below(), Blocks.STONE.defaultBlockState());
        world.setBlockAndUpdate(source, Blocks.WATER.defaultBlockState());
        player.setPos(source.getX() + 0.5, source.getY() + 1, source.getZ() + 0.5);
        player.setXRot(90);
        player.gameMode.useItem(
                player,
                world,
                player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND);
        check.accept(
                "universal-cell-actual-source-pickup",
                player.getItemInHand(InteractionHand.MAIN_HAND)
                                .is(NativeGameplayFixture.item("water_cell"))
                        && NativeTransferAccess.cellVolume(
                                        player.getItemInHand(InteractionHand.MAIN_HAND))
                                == 1000
                        && world.getFluidState(source).isEmpty());
        player.gameMode.useItem(
                player,
                world,
                player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND);
        check.accept(
                "universal-cell-does-not-place-fluid-into-world",
                NativeTransferAccess.cellVolume(player.getItemInHand(InteractionHand.MAIN_HAND))
                                == 1000
                        && world.getFluidState(source).isEmpty());
    }

    boolean tick(ServerLevel world, BiConsumer<String, Boolean> check) {
        if (complete) {
            return true;
        }
        ++elapsed;
        var cellsReady = fluidCells.tick(world, check);
        var machinesReady = cellMachines.tick(world, check);
        if (elapsed == 3) {
            check.accept(
                    "native-ore-washer-dry-break",
                    NativeTransferAccess.cargo(world, WASHER, Direction.DOWN).isEmpty());
            var water = new FluidStack(Fluids.WATER, 1000);
            check.accept(
                    "native-ore-washer-mb-supplied",
                    NativeTransferAccess.fill(world, WASHER, Direction.NORTH, water, false)
                            == 1000);
        }
        var extruded = NativeTransferAccess.cargo(world, FORMER, Direction.DOWN);
        var washed = NativeTransferAccess.cargo(world, WASHER, Direction.DOWN);
        if (!advancedCold && !extruded.isEmpty() && !washed.isEmpty()) {
            check.accept(
                    "native-generator-extrudes-three-copper-cables",
                    extruded.is(NativeGameplayFixture.item("copper_cable"))
                            && extruded.getCount() == 3);
            check.accept(
                    "native-washer-used-100-mb",
                    washed.is(NativeGameplayFixture.item("purified_copper"))
                            && washed.getCount() == 1
                            && NativeTransferAccess.volume(world, WASHER, Direction.UP) == 900);
            advancedCold = true;
        }
        if (!scrapVerified && elapsed >= 5) {
            var rewards =
                    world.getEntitiesOfClass(
                            ItemEntity.class, AABB.ofSize(Vec3.atCenterOf(SCRAP.above()), 6, 8, 6));
            boolean dispensed =
                    world.getBlockEntity(SCRAP) instanceof DispenserBlockEntity dispenser
                            && dispenser.getItem(0).getCount() == 1
                            && rewards.size() == 1
                            && rewards.getFirst().getItem().getCount() == 1
                            && !rewards.getFirst()
                                    .getItem()
                                    .is(NativeGameplayFixture.item("scrap_box"));
            if (dispensed || elapsed >= 2500) {
                check.accept("native-scrap-box-dispenser-ejects-reward", dispensed);
                scrapVerified = true;
            }
        }
        if (!handOpened && world.isPositionEntityTicking(SCRAP_HAND)) {
            var player = FakePlayerFactory.getMinecraft(world);
            player.setPos(SCRAP_HAND.getX() + 0.5, SCRAP_HAND.getY(), SCRAP_HAND.getZ() + 0.5);
            var box = new ItemStack(NativeGameplayFixture.item("scrap_box"), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, box);
            box.getItem().use(world, player, InteractionHand.MAIN_HAND);
            check.accept(
                    "native-scrap-box-right-click-consumes-one",
                    player.getMainHandItem().getCount() == 1);
            handOpened = true;
        } else if (!handVerified) {
            var reward =
                    world.getEntitiesOfClass(
                            ItemEntity.class, AABB.ofSize(Vec3.atCenterOf(SCRAP_HAND), 3, 5, 3));
            boolean spawned =
                    handOpened
                            && reward.size() == 1
                            && reward.getFirst().getItem().getCount() == 1
                            && !reward.getFirst()
                                    .getItem()
                                    .is(NativeGameplayFixture.item("scrap_box"));
            if (spawned || elapsed >= 2500) {
                check.accept("native-scrap-box-right-click-drops-reward", spawned);
                handVerified = true;
            }
        }
        if (elapsed == 1200) {
            var bottled = NativeTransferAccess.cargo(world, BOTTLER, Direction.EAST);
            check.accept(
                    "native-bottler-yields-two-named-water-cells",
                    bottled.is(NativeGameplayFixture.item("basic_water_cell"))
                            && bottled.getCount() == 2
                            && NativeTransferAccess.volume(world, BOTTLER, Direction.NORTH) == 0);
            var electrolyzer = (WorkshopNode) world.getBlockEntity(ELECTROLYZER);
            check.accept(
                    "electrolysis-2560-eu-does-not-finish-a-6400-eu-batch",
                    electrolyzer.fluidContents().getFirst().amount() == 40
                            && electrolyzer.bufferedEu().stored() == 0
                            && NativeTransferAccess.volume(
                                            world, ELECTROLYZER.below(), Direction.UP)
                                    == 0
                            && NativeTransferAccess.volume(
                                            world, ELECTROLYZER.above(), Direction.DOWN)
                                    == 0);
            check.accept("electrolysis-pays-remaining-3840-eu", electrolyzer.receive(3840) == 3840);
        }
        if (elapsed == 1350) {
            check.accept(
                    "native-electrolysis-outputs-26-mb-below-and-13-mb-above",
                    NativeTransferAccess.volume(world, ELECTROLYZER.below(), Direction.UP) == 26
                            && NativeTransferAccess.volume(
                                            world, ELECTROLYZER.above(), Direction.DOWN)
                                    == 13);
            var electrolyzer = (WorkshopNode) world.getBlockEntity(ELECTROLYZER);
            check.accept(
                    "native-electrolysis-used-exactly-6400-eu-and-40-mb",
                    electrolyzer.fluidContents().getFirst().amount() == 0
                            && electrolyzer.bufferedEu().stored() == 0);
        }
        if (!fermentationVerified && elapsed >= 1000 && elapsed % 20 == 0) {
            var fertilizer = NativeTransferAccess.cargo(world, FERMENTER, Direction.DOWN);
            boolean exact =
                    fertilizer.is(NativeGameplayFixture.item("fertilizer"))
                            && fertilizer.getCount() == 1
                            && NativeTransferAccess.volume(world, FERMENTER.east(), Direction.WEST)
                                    == 10000
                            && NativeTransferAccess.volume(world, FERMENTER, Direction.UP) == 0;
            if (exact || elapsed >= 2500) {
                check.accept("native-fermentation-conserves-biomass-and-byproducts", exact);
                fermentationVerified = true;
            }
        }
        var steel = NativeTransferAccess.cargo(world, FURNACE, Direction.DOWN);
        if (!advancedCold || !fermentationVerified || !cellsReady || !machinesReady || steel.isEmpty()) {
            return false;
        }
        check.accept(
                "native-heat-and-air-line-made-steel",
                steel.is(NativeGameplayFixture.item("steel_ingot"))
                        && steel.getCount() == 1
                        && NativeTransferAccess.volume(world, FURNACE, Direction.NORTH) == 0);
        check.accept("native-steel-no-shortcut-before-6000-ticks", elapsed >= 6000);
        complete = true;
        return true;
    }

    @SuppressWarnings("unchecked")
    private static DataComponentType<Double> energyComponent() {
        return (DataComponentType<Double>)
                BuiltInRegistries.DATA_COMPONENT_TYPE.stream()
                        .filter(
                                type ->
                                        BuiltInRegistries.DATA_COMPONENT_TYPE
                                                .getKey(type)
                                                .toString()
                                                .equals("ic2:eu"))
                        .findFirst()
                        .orElseThrow();
    }
}
