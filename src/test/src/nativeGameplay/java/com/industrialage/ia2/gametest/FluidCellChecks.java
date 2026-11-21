package com.industrialage.ia2.gametest;

import com.industrialage.ia2.industrial.PowerNode;
import com.industrialage.ia2.industrial.WorkshopMenu;
import com.industrialage.ia2.industrial.WorkshopNode;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;

/** Production-jar regression for real cell slots and 10 naturally timed washer cycles. */
final class FluidCellChecks {
    private static final BlockPos SINGLE = new BlockPos(17, 140, 18);
    private static final BlockPos DOUBLE = new BlockPos(19, 140, 18);
    private static final BlockPos NO_POWER = new BlockPos(21, 140, 18);
    private static final BlockPos GEO = new BlockPos(23, 140, 18);
    private static final BlockPos GEO_FUELLED = new BlockPos(25, 140, 18);
    private static final BlockPos HEATER = new BlockPos(27, 140, 18);
    private static final BlockPos FURNACE = HEATER.east();
    private static final BlockPos SEMI = new BlockPos(30, 140, 18);
    private static final BlockPos SEMI_EMPTY = new BlockPos(30, 140, 20);
    private static final BlockPos GEO_PARTIAL = new BlockPos(30, 140, 22);
    private static final BlockPos GEO_7001 = new BlockPos(30, 140, 24);
    private WorkshopNode single;
    private WorkshopNode doubled;
    private WorkshopNode noPower;
    private PowerNode geo;
    private PowerNode geoFuelled;
    private WorkshopNode heater;
    private WorkshopNode furnace;
    private WorkshopNode semi;
    private WorkshopNode semiEmpty;
    private PowerNode geoPartial;
    private PowerNode geo7001;
    private WorkshopMenu singleMenu;
    private WorkshopMenu doubleMenu;
    private WorkshopMenu noPowerMenu;
    private AbstractContainerMenu geoMenu;
    private AbstractContainerMenu geoFuelledMenu;
    private AbstractContainerMenu heaterMenu;
    private WorkshopMenu furnaceMenu;
    private AbstractContainerMenu semiMenu;
    private AbstractContainerMenu semiEmptyMenu;
    private AbstractContainerMenu geoPartialMenu;
    private AbstractContainerMenu geo7001Menu;
    private int batches;
    private int activeTicks;
    private int dryTicks;
    private final FluidTankBoundaryChecks boundaries = new FluidTankBoundaryChecks();
    private boolean doubleCompleted;
    private boolean blastConsumedAir;
    private boolean done;

    void setup(ServerLevel world, BiConsumer<String, Boolean> check) {
        world.setChunkForced(1, 1, true);
        world.getChunk(1, 1);
        var owner = FakePlayerFactory.getMinecraft(world);
        NativeGameplayFixture.place(world, owner, "ore_washing_plant", SINGLE);
        NativeGameplayFixture.place(world, owner, "ore_washing_plant", DOUBLE);
        NativeGameplayFixture.place(world, owner, "ore_washing_plant", NO_POWER);
        NativeGameplayFixture.place(world, owner, "geo_generator", GEO);
        NativeGameplayFixture.place(world, owner, "geo_generator", GEO_FUELLED);
        NativeGameplayFixture.place(world, owner, "fluid_heat_generator", HEATER);
        NativeGameplayFixture.place(world, owner, "blast_furnace", FURNACE);
        NativeGameplayFixture.place(world, owner, "semifluid_generator", SEMI);
        NativeGameplayFixture.place(world, owner, "semifluid_generator", SEMI_EMPTY);
        NativeGameplayFixture.place(world, owner, "geo_generator", GEO_PARTIAL);
        NativeGameplayFixture.place(world, owner, "geo_generator", GEO_7001);
        world.setBlockAndUpdate(
                HEATER,
                world.getBlockState(HEATER).setValue(BlockStateProperties.FACING, Direction.EAST));
        world.setBlockAndUpdate(
                FURNACE,
                world.getBlockState(FURNACE)
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        single = (WorkshopNode) world.getBlockEntity(SINGLE);
        doubled = (WorkshopNode) world.getBlockEntity(DOUBLE);
        noPower = (WorkshopNode) world.getBlockEntity(NO_POWER);
        geo = (PowerNode) world.getBlockEntity(GEO);
        geoFuelled = (PowerNode) world.getBlockEntity(GEO_FUELLED);
        heater = (WorkshopNode) world.getBlockEntity(HEATER);
        furnace = (WorkshopNode) world.getBlockEntity(FURNACE);
        semi = (WorkshopNode) world.getBlockEntity(SEMI);
        semiEmpty = (WorkshopNode) world.getBlockEntity(SEMI_EMPTY);
        geoPartial = (PowerNode) world.getBlockEntity(GEO_PARTIAL);
        geo7001 = (PowerNode) world.getBlockEntity(GEO_7001);
        singleMenu = (WorkshopMenu) single.createMenu(1, owner.getInventory(), owner);
        doubleMenu = (WorkshopMenu) doubled.createMenu(2, owner.getInventory(), owner);
        noPowerMenu = (WorkshopMenu) noPower.createMenu(3, owner.getInventory(), owner);
        geoMenu = geo.createMenu(4, owner.getInventory(), owner);
        geoFuelledMenu = geoFuelled.createMenu(5, owner.getInventory(), owner);
        heaterMenu = heater.createMenu(6, owner.getInventory(), owner);
        furnaceMenu = (WorkshopMenu) furnace.createMenu(7, owner.getInventory(), owner);
        semiMenu = semi.createMenu(8, owner.getInventory(), owner);
        semiEmptyMenu = semiEmpty.createMenu(9, owner.getInventory(), owner);
        geoPartialMenu = geoPartial.createMenu(10, owner.getInventory(), owner);
        geo7001Menu = geo7001.createMenu(11, owner.getInventory(), owner);
        var ore = new ItemStack(NativeGameplayFixture.item("crushed_copper"), 11);
        var water = new ItemStack(NativeGameplayFixture.item("water_cell"));
        var twoWater = new ItemStack(NativeGameplayFixture.item("water_cell"), 9);
        check.accept(
                "washer-menu-accepts-water-rejects-lava",
                singleMenu.getSlot(6).mayPlace(water)
                        && !singleMenu
                                .getSlot(6)
                                .mayPlace(new ItemStack(NativeGameplayFixture.item("lava_cell"))));
        singleMenu.getSlot(0).set(ore);
        singleMenu.getSlot(6).set(water);
        single.receive(16_000);
        doubleMenu.getSlot(6).set(twoWater);
        noPowerMenu.getSlot(6).set(new ItemStack(NativeGameplayFixture.item("water_cell")));
        check.accept(
                "geothermal-menu-accepts-lava-rejects-water",
                geoMenu.getSlot(0).mayPlace(new ItemStack(NativeGameplayFixture.item("lava_cell")))
                        && !geoMenu.getSlot(0)
                                .mayPlace(new ItemStack(NativeGameplayFixture.item("water_cell"))));
        geoMenu.getSlot(0).set(new ItemStack(NativeGameplayFixture.item("water_cell")));
        check.accept(
                "geothermal-pipe-prefills-7000-mb",
                NativeTransferAccess.fill(
                                world,
                                GEO_FUELLED,
                                Direction.UP,
                                new FluidStack(Fluids.LAVA, 7000),
                                false)
                        == 7000);
        geoFuelledMenu.getSlot(0).set(new ItemStack(NativeGameplayFixture.item("lava_cell")));
        heaterMenu.getSlot(0).set(new ItemStack(NativeGameplayFixture.item("lava_cell"), 3));
        furnaceMenu.getSlot(4).set(new ItemStack(NativeGameplayFixture.item("air_cell"), 2));
        var biogas =
                BuiltInRegistries.FLUID.stream()
                        .filter(
                                fluid ->
                                        BuiltInRegistries.FLUID
                                                .getKey(fluid)
                                                .toString()
                                                .equals("ic2:biogas"))
                        .findFirst()
                        .orElseThrow();
        var biogasStack = new FluidStack(biogas, 1000);
        check.accept(
                "semifluid-pipe-prefills-one-liter",
                NativeTransferAccess.fill(world, SEMI, Direction.UP, biogasStack, false) == 1000);
        var cell = new ItemStack(NativeGameplayFixture.item("fluid_cell"));
        semiMenu.getSlot(0).set(NativeTransferAccess.filledCell(cell, biogasStack));
        semiEmptyMenu.getSlot(0).set(NativeTransferAccess.filledCell(cell, biogasStack));
        geoPartialMenu
                .getSlot(0)
                .set(NativeTransferAccess.filledCell(cell, new FluidStack(Fluids.LAVA, 250)));
        check.accept(
                "geothermal-pipe-prefills-7001-mb",
                NativeTransferAccess.fill(
                                world,
                                GEO_7001,
                                Direction.UP,
                                new FluidStack(Fluids.LAVA, 7001),
                                false)
                        == 7001);
        geo7001Menu.getSlot(0).set(new ItemStack(NativeGameplayFixture.item("lava_cell")));
        var commands = world.getServer().getCommands();
        var console = world.getServer().createCommandSourceStack().withSuppressedOutput();
        commands.performPrefixedCommand(console, "data merge block 25 140 18 {eu:2400.0d}");
        commands.performPrefixedCommand(console, "data merge block 30 140 24 {eu:2400.0d}");
        boundaries.setup(world, check);
    }

    boolean tick(ServerLevel world, BiConsumer<String, Boolean> check) {
        if (done) return true;
        if (!world.isPositionEntityTicking(DOUBLE)
                || !single.ranThisTick(world)
                || !doubled.ranThisTick(world)) return false;
        int elapsed = ++activeTicks;
        boolean boundaryDone = boundaries.tick(world, check);
        if (elapsed == 3) {
            check.accept(
                    "washer-no-ore-or-power-prefills-single-cell",
                    noPowerMenu.getSlot(6).getItem().isEmpty()
                            && noPowerMenu
                                    .getSlot(14)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("empty_cell"))
                            && noPower.fluidContents().getFirst().amount() == 1000);
            noPowerMenu.getSlot(6).set(new ItemStack(NativeGameplayFixture.item("lava_cell")));
            check.accept(
                    "geothermal-rejects-wrong-fluid-on-tick",
                    geoMenu.getSlot(0).getItem().is(NativeGameplayFixture.item("water_cell"))
                            && geo.fluidContents().getFirst().amount() == 0
                            && geo.bufferedEu().stored() == 0);
            geoMenu.getSlot(0).set(new ItemStack(NativeGameplayFixture.item("lava_cell")));
            geoMenu.getSlot(1).set(new ItemStack(Items.STONE, 64));
        }
        if (elapsed == 7) {
            check.accept(
                    "geothermal-blocked-return-preserves-cell",
                    geoMenu.getSlot(0).getItem().is(NativeGameplayFixture.item("lava_cell"))
                            && geo.fluidContents().getFirst().amount() == 0);
            check.accept(
                    "washer-wrong-liquid-does-not-consume",
                    noPowerMenu.getSlot(6).getItem().is(NativeGameplayFixture.item("lava_cell"))
                            && noPowerMenu.getSlot(14).getItem().getCount() == 1
                            && noPower.fluidContents().getFirst().amount() == 1000);
            noPowerMenu.getSlot(6).set(new ItemStack(NativeGameplayFixture.item("water_cell")));
            noPowerMenu.getSlot(14).set(new ItemStack(Items.STONE, 64));
            geoMenu.getSlot(1).set(ItemStack.EMPTY);
        }
        if (elapsed == 11) {
            check.accept(
                    "washer-blocked-shell-output-preserves-cell",
                    noPowerMenu.getSlot(6).getItem().is(NativeGameplayFixture.item("water_cell"))
                            && noPower.fluidContents().getFirst().amount() == 1000);
            noPowerMenu.getSlot(14).set(ItemStack.EMPTY);
            for (int slot = 9; slot <= 11; slot++) {
                noPowerMenu.getSlot(slot).set(new ItemStack(Items.STONE, 64));
            }
        }
        if (elapsed == 15) {
            check.accept(
                    "washer-with-no-ore-or-power-fills-eight-of-nine-cells",
                    doubleMenu.getSlot(6).getItem().is(NativeGameplayFixture.item("water_cell"))
                            && doubleMenu.getSlot(6).getItem().getCount() == 1
                            && doubleMenu
                                    .getSlot(14)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("empty_cell"))
                            && doubleMenu.getSlot(14).getItem().getCount() == 8
                            && doubled.fluidContents().getFirst().amount() == 8000);
            doubleMenu.getSlot(0).set(new ItemStack(NativeGameplayFixture.item("crushed_copper")));
            doubled.receive(16_000);
            check.accept(
                    "washer-blocked-products-still-prefill",
                    noPowerMenu.getSlot(6).getItem().isEmpty()
                            && noPowerMenu
                                    .getSlot(14)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("empty_cell"))
                            && noPower.fluidContents().getFirst().amount() == 2000);
            check.accept(
                    "geothermal-lava-cell-returns-shell-and-generates",
                    geoMenu.getSlot(0).getItem().isEmpty()
                            && geoMenu.getSlot(1)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("empty_cell"))
                            && geo.fluidContents().getFirst().amount() > 0
                            && geo.bufferedEu().stored() > 0);
            check.accept(
                    "geothermal-saturated-7000-prefills-whole-cell",
                    geoFuelledMenu.getSlot(0).getItem().isEmpty()
                            && geoFuelledMenu
                                    .getSlot(1)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("empty_cell"))
                            && geoFuelled.fluidContents().getFirst().amount() == 8000
                            && geoFuelled.bufferedEu().stored() == 2400);
            check.accept(
                    "geothermal-saturated-7001-preserves-whole-cell",
                    geo7001Menu.getSlot(0).getItem().is(NativeGameplayFixture.item("lava_cell"))
                            && geo7001Menu.getSlot(1).getItem().isEmpty()
                            && geo7001.fluidContents().getFirst().amount() == 7001
                            && geo7001.bufferedEu().stored() == 2400);
            check.accept(
                    "geothermal-pipe-may-partially-fill-999-mb-gap",
                    NativeTransferAccess.fill(
                                            world,
                                            GEO_7001,
                                            Direction.UP,
                                            new FluidStack(Fluids.LAVA, 1000),
                                            false)
                                    == 999
                            && geo7001.fluidContents().getFirst().amount() == 8000
                            && geo7001Menu
                                    .getSlot(0)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("lava_cell")));
            check.accept(
                    "geothermal-partial-universal-cell-generates-and-returns-empty",
                    geoPartialMenu.getSlot(0).getItem().isEmpty()
                            && geoPartialMenu
                                    .getSlot(1)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("fluid_cell"))
                            && NativeTransferAccess.itemFluidVolume(
                                            geoPartialMenu.getSlot(1).getItem())
                                    == 0
                            && geoPartial.fluidContents().getFirst().amount() > 0
                            && geoPartial.fluidContents().getFirst().amount() < 250
                            && geoPartial.bufferedEu().stored() > 0);
            check.accept(
                    "fluid-heater-prefills-all-three-cells",
                    heaterMenu.getSlot(0).getItem().isEmpty()
                            && heaterMenu
                                    .getSlot(9)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("empty_cell"))
                            && heaterMenu.getSlot(9).getItem().getCount() == 3
                            && heater.fluidContents().getFirst().amount() > 2000
                            && heater.fluidContents().getFirst().amount() <= 3000);
            check.accept(
                    "cold-furnace-without-iron-prefills-two-air-cells",
                    furnaceMenu.getSlot(4).getItem().isEmpty()
                            && furnaceMenu
                                    .getSlot(13)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("empty_cell"))
                            && furnaceMenu.getSlot(13).getItem().getCount() == 2
                            && furnace.fluidContents().getFirst().amount() == 2000);
            furnaceMenu.getSlot(0).set(new ItemStack(Items.IRON_INGOT));
            check.accept(
                    "semifluid-prefills-cell-while-consuming-piped-biogas",
                    semiMenu.getSlot(0).getItem().isEmpty()
                            && semiMenu.getSlot(9)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("fluid_cell"))
                            && NativeTransferAccess.itemFluidVolume(semiMenu.getSlot(9).getItem())
                                    == 0
                            && semi.fluidContents().getFirst().amount() > 1000
                            && semi.fluidContents().getFirst().amount() < 2000
                            && semi.bufferedEu().stored() > 0);
            check.accept(
                    "semifluid-empty-tank-opens-universal-cell",
                    semiEmptyMenu.getSlot(0).getItem().isEmpty()
                            && semiEmptyMenu
                                    .getSlot(9)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("fluid_cell"))
                            && NativeTransferAccess.itemFluidVolume(
                                            semiEmptyMenu.getSlot(9).getItem())
                                    == 0
                            && semiEmpty.fluidContents().getFirst().amount() > 0
                            && semiEmpty.fluidContents().getFirst().amount() < 1000
                            && semiEmpty.bufferedEu().stored() > 0);
        }
        if (elapsed % 50 == 0) single.receive(16_000);
        int produced = singleMenu.getSlot(9).getItem().getCount();
        if (produced > batches) {
            check.accept(
                    "washer-native-batch-" + produced + "-uses-100-mb",
                    produced == batches + 1
                            && singleMenu
                                    .getSlot(9)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("purified_copper"))
                            && single.fluidContents().getFirst().amount() == 1000 - produced * 100
                            && singleMenu
                                    .getSlot(14)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("empty_cell"))
                            && singleMenu.getSlot(14).getItem().getCount() == 1);
            batches = produced;
        }
        if (!doubleCompleted && !doubleMenu.getSlot(9).getItem().isEmpty()) {
            check.accept(
                    "washer-full-tank-first-batch-leaves-7900-and-ninth-cell",
                    doubleMenu
                                    .getSlot(9)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("purified_copper"))
                            && doubled.fluidContents().getFirst().amount() == 7900
                            && doubleMenu.getSlot(6).getItem().getCount() == 1
                            && doubleMenu.getSlot(14).getItem().getCount() == 8);
            doubleCompleted = true;
        }
        if (!blastConsumedAir && furnaceMenu.progress() > 0) {
            check.accept(
                    "blast-furnace-consumes-prefilled-air",
                    furnace.fluidContents().getFirst().amount() > 1000
                            && furnace.fluidContents().getFirst().amount() < 2000
                            && furnaceMenu.getSlot(4).getItem().isEmpty()
                            && furnaceMenu.getSlot(13).getItem().getCount() == 2);
            blastConsumedAir = true;
        }
        if (batches == 10) {
            if (dryTicks < 10) {
                check.accept(
                        "washer-eleventh-input-waits-dry-" + dryTicks,
                        singleMenu.getSlot(0).getItem().getCount() == 1
                                && singleMenu.progress() == 0
                                && single.fluidContents().getFirst().amount() == 0);
                ++dryTicks;
            }
            if (dryTicks == 10) done = doubleCompleted && blastConsumedAir && boundaryDone;
        }
        return done;
    }
}
