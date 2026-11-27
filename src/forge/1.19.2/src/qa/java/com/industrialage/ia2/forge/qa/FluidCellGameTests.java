package com.industrialage.ia2.forge.qa;

import static com.industrialage.ia2.forge.qa.PlayableGameTests.block;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.check;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.machine;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.place;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.player;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.stack;

import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.logistics.TransportPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Verifies native cell slots and exact water use in real machine ticks. */
@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class FluidCellGameTests {
    private FluidCellGameTests() {}

    /** The native compressor and extractor keep the item identity and data of one paid job. */
    @GameTest(template = "smoke", timeoutTicks = 700)
    public static void cellProcessorRoundTrip(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var operator = player(helper, pos);
        place(helper, operator, pos, stack("compressor"));
        place(helper, operator, pos.east(2), stack("extractor"));
        var compressor = machine(helper, pos);
        var extractor = machine(helper, pos.east(2));
        var source =
                compressor
                        .getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                        .orElseThrow(AssertionError::new);
        var sink =
                extractor
                        .getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                        .orElseThrow(AssertionError::new);
        var dirty = filledCell(new FluidStack(Fluids.WATER, 650));
        check(
                helper,
                source.insertItem(0, dirty, false).getCount() == 1
                        && TransportPlatform.cellContents(dirty).amount() == 650,
                "The compressor refuses a liquid-bearing universal shell");
        var mark = Component.literal("Air cell circuit");
        check(
                helper,
                source.insertItem(0, stack("fluid_cell").setHoverName(mark), false).isEmpty(),
                "A genuine empty universal shell entered the compressor");
        check(
                helper,
                sink.insertItem(0, stack("basic_water_cell").setHoverName(mark), false).isEmpty(),
                "A basic water cell entered the extractor");
        charge(compressor);
        charge(extractor);
        helper.runAfterDelay(
                315,
                () -> {
                    var air =
                            compressor
                                    .getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN)
                                    .orElseThrow(AssertionError::new)
                                    .extractItem(0, 1, false);
                    var worked =
                            extractor
                                    .getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN)
                                    .orElseThrow(AssertionError::new);
                    var basic = worked.extractItem(0, 1, false);
                    check(
                            helper,
                            air.is(stack("air_cell").getItem())
                                    && TransportPlatform.cellContents(air).amount() == 1000
                                    && air.getHoverName().equals(mark),
                            "Compressor outputs one marked air cell");
                    check(
                            helper,
                            basic.is(stack("empty_cell").getItem())
                                    && basic.getHoverName().equals(mark),
                            "Extractor returns a marked basic shell");
                    check(
                            helper,
                            sink.insertItem(0, air, false).isEmpty(),
                            "Extract the compressed air cell");
                    charge(extractor);
                    helper.runAfterDelay(
                            315,
                            () -> {
                                var universal = worked.extractItem(0, 1, false);
                                check(
                                        helper,
                                        universal.is(stack("fluid_cell").getItem())
                                                && universal.getHoverName().equals(mark)
                                                && TransportPlatform.cellContents(universal)
                                                                .amount()
                                                        == 0,
                                        "Extractor returns exactly one marked universal shell");
                                helper.succeed();
                            });
                });
    }

    /** The combined canner applies separate native fill and drain modes without losing a cell. */
    @GameTest(template = "smoke", timeoutTicks = 145)
    public static void cannerUsesBothCellFamilies(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var operator = player(helper, pos);
        place(helper, operator, pos, stack("canner"));
        place(helper, operator, pos.east(2), stack("canner"));
        var filler = (WorkshopNode) machine(helper, pos);
        var drainer = (WorkshopNode) machine(helper, pos.east(2));
        configureCanner(filler, 1);
        configureCanner(drainer, 2);
        var fill = filler.createMenu(30, operator.getInventory(), operator);
        var drain = drainer.createMenu(31, operator.getInventory(), operator);
        var water =
                filler.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                        .orElseThrow(AssertionError::new);
        check(
                helper,
                water.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 1000,
                "Canner inlet metered one full source");
        fill.getSlot(0).set(stack("fluid_cell"));
        fill.getSlot(9).set(new ItemStack(Items.COBBLESTONE, 64));
        drain.getSlot(0).set(stack("basic_lava_cell"));
        helper.runAfterDelay(
                13,
                () -> {
                    check(
                            helper,
                            fill.getSlot(0).getItem().is(stack("fluid_cell").getItem())
                                    && filler.fluidContents().get(0).amount() == 1000,
                            "Blocked canner output preserves both the shell and its fluid");
                    fill.getSlot(9).set(ItemStack.EMPTY);
                });
        helper.runAfterDelay(
                128,
                () -> {
                    check(
                            helper,
                            fill.getSlot(0).getItem().isEmpty()
                                    && fill.getSlot(9).getItem().is(stack("water_cell").getItem())
                                    && filler.fluidContents().get(0).amount() == 0,
                            "Canner filled a universal shell with a named liter of water");
                    check(
                            helper,
                            drain.getSlot(0).getItem().isEmpty()
                                    && drain.getSlot(9).getItem().is(stack("empty_cell").getItem())
                                    && drainer.fluidContents().get(1).amount() == 1000,
                            "Canner drained basic lava and returned the basic shell");
                    helper.succeed();
                });
    }

    private static void configureCanner(WorkshopNode node, int mode) {
        var state = node.saveWithoutMetadata();
        state.putInt("ic2_mode", mode);
        node.load(state);
        node.receive(400);
    }

    private static void charge(net.minecraft.world.level.block.entity.BlockEntity machine) {
        var tag = machine.saveWithoutMetadata();
        tag.putDouble("ic2_energy", 600);
        machine.load(tag);
    }

    /** Nine cells prefill eight liters without ore or power; the ninth remains until it fits. */
    @GameTest(template = "smoke", timeoutTicks = 100)
    public static void washerPrefillsBeforeWork(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("ore_washing_plant"));
        var washer = machine(helper, pos);
        var menu = ((MenuProvider) washer).createMenu(1, owner.getInventory(), owner);
        var water = stack("water_cell");
        water.setCount(9);
        check(helper, menu.getSlot(6).mayPlace(water), "Washer must accept water cells");
        check(
                helper,
                !menu.getSlot(6).mayPlace(stack("lava_cell")),
                "Washer must reject lava cells");
        menu.getSlot(6).set(water);
        helper.runAfterDelay(
                12,
                () -> {
                    var state = washer.saveWithoutMetadata();
                    check(
                            helper,
                            menu.getSlot(6).getItem().is(stack("water_cell").getItem())
                                    && menu.getSlot(6).getItem().getCount() == 1
                                    && menu.getSlot(14).getItem().is(stack("fluid_cell").getItem())
                                    && menu.getSlot(14).getItem().getCount() == 8
                                    && state.getCompound("ic2_input_fluid").getInt("Amount")
                                            == 8000,
                            "Idle unpowered washer must fill eight whole cells and retain ninth");
                    menu.getSlot(0).set(stack("crushed_copper"));
                    state = washer.saveWithoutMetadata();
                    state.putDouble("ic2_energy", 16000);
                    washer.load(state);
                });
        helper.runAfterDelay(
                19,
                () -> {
                    var state = washer.saveWithoutMetadata();
                    check(helper, state.getInt("ic2_progress") > 0, "Washer must actually start");
                    state.putInt("ic2_progress", 499);
                    washer.load(state);
                });
        helper.runAfterDelay(
                23,
                () -> {
                    var state = washer.saveWithoutMetadata();
                    check(
                            helper,
                            menu.getSlot(9).getItem().is(stack("purified_copper").getItem()),
                            "One real job completed");
                    check(
                            helper,
                            state.getCompound("ic2_input_fluid").getInt("Amount") == 7900,
                            "One job leaves 7900 mB");
                    check(
                            helper,
                            menu.getSlot(6).getItem().getCount() == 1
                                    && menu.getSlot(14).getItem().getCount() == 8,
                            "Ninth cell remains closed with only 100 mB free");
                    helper.succeed();
                });
    }

    /** Ten actual completed operations consume one liter; an eleventh waits without water. */
    @GameTest(template = "smoke", timeoutTicks = 140)
    public static void washerTenBatchesThenWaitsForWater(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("ore_washing_plant"));
        var washer = machine(helper, pos);
        var menu = ((MenuProvider) washer).createMenu(1, owner.getInventory(), owner);
        var crushed = stack("crushed_copper");
        crushed.setCount(11);
        menu.getSlot(0).set(crushed);
        menu.getSlot(6).set(stack("water_cell"));
        var initial = washer.saveWithoutMetadata();
        initial.putDouble("ic2_energy", 16000);
        washer.load(initial);
        int[] batches = {0};
        int[] dryTicks = {0};
        helper.onEachTick(
                () -> {
                    var state = washer.saveWithoutMetadata();
                    int done = menu.getSlot(9).getItem().getCount();
                    if (done > batches[0]) {
                        check(helper, done == batches[0] + 1, "One product per completed batch");
                        check(
                                helper,
                                menu.getSlot(9).getItem().is(stack("purified_copper").getItem()),
                                "Correct product");
                        check(
                                helper,
                                state.getCompound("ic2_input_fluid").getInt("Amount")
                                        == 1000 - done * 100,
                                "Each batch consumes exactly 100 mB");
                        check(
                                helper,
                                menu.getSlot(14).getItem().is(stack("fluid_cell").getItem())
                                        && menu.getSlot(14).getItem().getCount() == 1,
                                "Only one shell returned");
                        batches[0] = done;
                    }
                    if (batches[0] == 10) {
                        check(
                                helper,
                                menu.getSlot(0).getItem().getCount() == 1,
                                "Eleventh input must remain");
                        check(
                                helper,
                                state.getInt("ic2_progress") == 0,
                                "Eleventh batch must wait dry");
                        if (++dryTicks[0] == 8) helper.succeed();
                    } else if (state.getInt("ic2_progress") > 0) {
                        // The separate native water-gate test observes an unshortened 500-tick job.
                        // Here accelerate progress only; every completion is still a native machine
                        // tick.
                        state.putInt("ic2_progress", 499);
                        state.putDouble("ic2_energy", 16000);
                        washer.load(state);
                    }
                });
    }

    /** Wrong liquids and a blocked output preserve the filled cell and stored fluid. */
    @GameTest(template = "smoke", timeoutTicks = 40)
    public static void washerPreservesBlockedOrWrongCell(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("ore_washing_plant"));
        var washer = machine(helper, pos);
        var menu = ((MenuProvider) washer).createMenu(1, owner.getInventory(), owner);
        menu.getSlot(0).set(stack("crushed_copper"));
        menu.getSlot(6).set(stack("lava_cell"));
        var state = washer.saveWithoutMetadata();
        state.putDouble("ic2_energy", 16000);
        washer.load(state);
        helper.runAfterDelay(
                5,
                () -> {
                    check(
                            helper,
                            menu.getSlot(6).getItem().is(stack("lava_cell").getItem()),
                            "Wrong liquid was consumed");
                    check(
                            helper,
                            menu.getSlot(14).getItem().isEmpty(),
                            "Wrong liquid returned a shell");
                    check(
                            helper,
                            !washer.saveWithoutMetadata().contains("ic2_input_fluid"),
                            "Wrong liquid filled washer");
                    menu.getSlot(6).set(stack("water_cell"));
                    menu.getSlot(14).set(new ItemStack(Items.STONE, 64));
                });
        helper.runAfterDelay(
                10,
                () -> {
                    check(
                            helper,
                            menu.getSlot(6).getItem().is(stack("water_cell").getItem()),
                            "Blocked return consumed a cell");
                    check(
                            helper,
                            !washer.saveWithoutMetadata().contains("ic2_input_fluid"),
                            "Blocked return filled water");
                    menu.getSlot(14).set(ItemStack.EMPTY);
                    for (int i = 9; i <= 11; i++)
                        menu.getSlot(i).set(new ItemStack(Items.STONE, 64));
                });
        helper.runAfterDelay(
                15,
                () -> {
                    check(
                            helper,
                            menu.getSlot(6).getItem().isEmpty(),
                            "Blocked products should not prevent prefill");
                    check(
                            helper,
                            menu.getSlot(14).getItem().is(stack("fluid_cell").getItem()),
                            "Prefilled cell returned a shell");
                    check(
                            helper,
                            washer.saveWithoutMetadata()
                                            .getCompound("ic2_input_fluid")
                                            .getInt("Amount")
                                    == 1000,
                            "Blocked products still permit prefill");
                    helper.succeed();
                });
    }

    /** Geothermal consumes lava cells, returns shells, and refuses blocked/wrong inputs. */
    @GameTest(template = "smoke", timeoutTicks = 50)
    public static void geothermalLavaCellsAndBlockedReturn(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("geo_generator"));
        var generator = machine(helper, pos);
        var menu = ((MenuProvider) generator).createMenu(1, owner.getInventory(), owner);
        check(
                helper,
                menu.getSlot(0).mayPlace(stack("lava_cell")),
                "Geothermal must accept lava cells");
        check(
                helper,
                !menu.getSlot(0).mayPlace(stack("water_cell")),
                "Geothermal must reject water cells");
        menu.getSlot(0).set(stack("water_cell"));
        helper.runAfterDelay(
                5,
                () -> {
                    check(
                            helper,
                            menu.getSlot(0).getItem().is(stack("water_cell").getItem()),
                            "Geothermal consumed wrong liquid");
                    check(
                            helper,
                            generator.saveWithoutMetadata().getDouble("eu") == 0,
                            "Wrong liquid generated energy");
                    menu.getSlot(0).set(stack("lava_cell"));
                    menu.getSlot(1).set(new ItemStack(Items.STONE, 64));
                });
        helper.runAfterDelay(
                10,
                () -> {
                    check(
                            helper,
                            menu.getSlot(0).getItem().is(stack("lava_cell").getItem()),
                            "Blocked geothermal consumed lava");
                    check(
                            helper,
                            generator.saveWithoutMetadata().getInt("millibuckets") == 0,
                            "Blocked geothermal filled lava");
                    menu.getSlot(1).set(ItemStack.EMPTY);
                });
        helper.runAfterDelay(
                15,
                () -> {
                    var state = generator.saveWithoutMetadata();
                    check(
                            helper,
                            menu.getSlot(0).getItem().isEmpty(),
                            "Lava cell was not consumed");
                    check(
                            helper,
                            menu.getSlot(1).getItem().is(stack("fluid_cell").getItem()),
                            "Lava cell shell was not returned");
                    check(
                            helper,
                            state.getDouble("eu") > 0 && state.getInt("millibuckets") > 0,
                            "Lava cell did not generate electricity");
                    helper.succeed();
                });
    }

    /** Whole 1000 mB geothermal cells respect the 999/1000 gap boundary. */
    @GameTest(template = "smoke", timeoutTicks = 35)
    public static void geothermalQueuedCellAndNativePipe(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("geo_generator"));
        var generator = machine(helper, pos);
        var menu = ((MenuProvider) generator).createMenu(1, owner.getInventory(), owner);
        var port =
                generator
                        .getCapability(
                                ForgeCapabilities.FLUID_HANDLER, net.minecraft.core.Direction.UP)
                        .orElseThrow(AssertionError::new);
        check(
                helper,
                port.fill(new FluidStack(Fluids.LAVA, 7001), FluidAction.EXECUTE) == 7001,
                "Pipe filled 7001 mB");
        menu.getSlot(0).set(stack("lava_cell"));
        var saved = generator.saveWithoutMetadata();
        saved.putDouble("eu", 2400);
        generator.load(saved);
        helper.runAfterDelay(
                5,
                () -> {
                    var state = generator.saveWithoutMetadata();
                    check(
                            helper,
                            state.getDouble("eu") == 2400
                                    && state.getInt("millibuckets") == 7001
                                    && menu.getSlot(0).getItem().is(stack("lava_cell").getItem())
                                    && menu.getSlot(1).getItem().isEmpty(),
                            "Saturated geothermal preserves cell when only 999 mB fits");
                    check(
                            helper,
                            port.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE)
                                    == 999,
                            "A pipe may partially fill the remaining 999 mB");
                    state = generator.saveWithoutMetadata();
                    state.putInt("millibuckets", 7000);
                    generator.load(state);
                });
        helper.runAfterDelay(
                10,
                () -> {
                    var state = generator.saveWithoutMetadata();
                    check(
                            helper,
                            state.getDouble("eu") == 2400
                                    && state.getInt("millibuckets") == 8000
                                    && menu.getSlot(0).getItem().isEmpty()
                                    && menu.getSlot(1).getItem().is(stack("fluid_cell").getItem()),
                            "Saturated geothermal prefills whole cell when exactly 1000 mB fits");
                    helper.succeed();
                });
    }

    /** A partially filled universal cell provides its actual volume and returns empty. */
    @GameTest(template = "smoke", timeoutTicks = 25)
    public static void geothermalAcceptsPartialUniversalLavaCell(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("geo_generator"));
        var generator = machine(helper, pos);
        var menu = ((MenuProvider) generator).createMenu(1, owner.getInventory(), owner);
        menu.getSlot(0).set(filledCell(new FluidStack(Fluids.LAVA, 250)));
        helper.runAfterDelay(
                8,
                () -> {
                    var returned = menu.getSlot(1).getItem();
                    var state = generator.saveWithoutMetadata();
                    check(
                            helper,
                            menu.getSlot(0).getItem().isEmpty()
                                    && returned.is(stack("fluid_cell").getItem())
                                    && returned.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                                            .orElseThrow(AssertionError::new)
                                            .getFluidInTank(0)
                                            .isEmpty()
                                    && state.getInt("millibuckets") > 0
                                    && state.getInt("millibuckets") < 250
                                    && state.getDouble("eu") > 0,
                            "250 mB cell generated and returned the empty universal cell");
                    helper.succeed();
                });
    }

    /** Existing biogas and the next whole cell both fill the tank while generating. */
    @GameTest(template = "smoke", timeoutTicks = 25)
    public static void semifluidPrefillsWhileGenerating(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("semifluid_generator"));
        var generator = machine(helper, pos);
        var menu = ((MenuProvider) generator).createMenu(1, owner.getInventory(), owner);
        var biogas = ForgeRegistries.FLUIDS.getValue(new ResourceLocation("ic2", "biogas"));
        var fluid = new FluidStack(biogas, 1000);
        var port =
                generator
                        .getCapability(
                                ForgeCapabilities.FLUID_HANDLER, net.minecraft.core.Direction.UP)
                        .orElseThrow(AssertionError::new);
        check(helper, port.fill(fluid, FluidAction.EXECUTE) == 1000, "Pipe filled biogas");
        menu.getSlot(0).set(filledCell(fluid));
        helper.runAfterDelay(
                8,
                () -> {
                    var state = generator.saveWithoutMetadata();
                    var returned = menu.getSlot(9).getItem();
                    check(
                            helper,
                            menu.getSlot(0).getItem().isEmpty()
                                    && returned.is(stack("fluid_cell").getItem())
                                    && returned.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                                            .orElseThrow(AssertionError::new)
                                            .getFluidInTank(0)
                                            .isEmpty()
                                    && state.getCompound("ic2_input_fluid").getInt("Amount") > 1000
                                    && state.getCompound("ic2_input_fluid").getInt("Amount") < 2000
                                    && state.getDouble("ic2_energy") > 0,
                            "Both liters were filled and biogas was consumed");
                    helper.succeed();
                });
    }

    /** A cold furnace prefills eight liters without iron, then consumes air to work. */
    @GameTest(template = "smoke", timeoutTicks = 25)
    public static void blastFurnacePrefillsAndConsumesAir(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("blast_furnace"));
        var furnace = machine(helper, pos);
        var menu = ((MenuProvider) furnace).createMenu(1, owner.getInventory(), owner);
        var air = stack("air_cell");
        air.setCount(9);
        menu.getSlot(4).set(air);
        helper.runAfterDelay(
                10,
                () -> {
                    var state = furnace.saveWithoutMetadata();
                    check(
                            helper,
                            state.getCompound("ic2_input_fluid").getInt("Amount") == 8000
                                    && menu.getSlot(4).getItem().getCount() == 1
                                    && menu.getSlot(13).getItem().getCount() == 8,
                            "Cold furnace without iron must prefill eight cells");
                    menu.getSlot(0).set(new ItemStack(Items.IRON_INGOT));
                    state = furnace.saveWithoutMetadata();
                    state.putInt("ic2_heat", 60000);
                    furnace.load(state);
                });
        helper.runAfterDelay(
                17,
                () -> {
                    var current = furnace.saveWithoutMetadata();
                    int remaining = current.getCompound("ic2_input_fluid").getInt("Amount");
                    check(
                            helper,
                            menu.getSlot(4).getItem().getCount() == 1
                                    && menu.getSlot(13).getItem().is(stack("fluid_cell").getItem())
                                    && menu.getSlot(13).getItem().getCount() == 8
                                    && remaining > 7000
                                    && remaining < 8000
                                    && current.getInt("ic2_progress") > 0,
                            "Furnace consumed prefilled air");
                    helper.succeed();
                });
    }

    /** A 250 mB universal cell must never be partially drained into a nearly full washer. */
    @GameTest(template = "smoke", timeoutTicks = 35)
    public static void washerPartialCellExactBoundary(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("ore_washing_plant"));
        var washer = machine(helper, pos);
        var menu = ((MenuProvider) washer).createMenu(1, owner.getInventory(), owner);
        var port =
                washer.getCapability(
                                ForgeCapabilities.FLUID_HANDLER, net.minecraft.core.Direction.UP)
                        .orElseThrow(AssertionError::new);
        check(
                helper,
                port.fill(new FluidStack(Fluids.WATER, 7751), FluidAction.EXECUTE) == 7751,
                "Washer starts at 7751 mB");
        menu.getSlot(6).set(filledCell(new FluidStack(Fluids.WATER, 250)));
        helper.runAfterDelay(
                5,
                () -> {
                    var held = menu.getSlot(6).getItem();
                    check(
                            helper,
                            washer.saveWithoutMetadata()
                                                    .getCompound("ic2_input_fluid")
                                                    .getInt("Amount")
                                            == 7751
                                    && held.is(stack("fluid_cell").getItem())
                                    && held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                                                    .orElseThrow(AssertionError::new)
                                                    .getFluidInTank(0)
                                                    .getAmount()
                                            == 250
                                    && menu.getSlot(14).getItem().isEmpty(),
                            "249 mB free preserves the entire 250 mB cell");
                    var state = washer.saveWithoutMetadata();
                    state.getCompound("ic2_input_fluid").putInt("Amount", 7750);
                    washer.load(state);
                });
        helper.runAfterDelay(
                10,
                () -> {
                    var returned = menu.getSlot(14).getItem();
                    check(
                            helper,
                            washer.saveWithoutMetadata()
                                                    .getCompound("ic2_input_fluid")
                                                    .getInt("Amount")
                                            == 8000
                                    && menu.getSlot(6).getItem().isEmpty()
                                    && returned.is(stack("fluid_cell").getItem())
                                    && returned.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                                            .orElseThrow(AssertionError::new)
                                            .getFluidInTank(0)
                                            .isEmpty(),
                            "250 mB free accepts and returns the complete 250 mB cell");
                    helper.succeed();
                });
    }

    /** A job crossing 7100 to 7000 mB permits the queued whole liter to fill. */
    @GameTest(template = "smoke", timeoutTicks = 35)
    public static void washerRefillsAfterCrossingThreshold(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("ore_washing_plant"));
        var washer = machine(helper, pos);
        var menu = ((MenuProvider) washer).createMenu(1, owner.getInventory(), owner);
        var port =
                washer.getCapability(
                                ForgeCapabilities.FLUID_HANDLER, net.minecraft.core.Direction.UP)
                        .orElseThrow(AssertionError::new);
        check(
                helper,
                port.fill(new FluidStack(Fluids.WATER, 7100), FluidAction.EXECUTE) == 7100,
                "Washer starts at 7100 mB");
        menu.getSlot(6).set(stack("water_cell"));
        menu.getSlot(0).set(stack("crushed_copper"));
        var initial = washer.saveWithoutMetadata();
        initial.putDouble("ic2_energy", 16000);
        washer.load(initial);
        helper.runAfterDelay(
                5,
                () -> {
                    var state = washer.saveWithoutMetadata();
                    check(
                            helper,
                            state.getCompound("ic2_input_fluid").getInt("Amount") == 7100
                                    && menu.getSlot(6).getItem().is(stack("water_cell").getItem())
                                    && state.getInt("ic2_progress") > 0,
                            "Cell waits while 900 mB is free and work is running");
                    state.putInt("ic2_progress", 499);
                    washer.load(state);
                });
        helper.runAfterDelay(
                10,
                () -> {
                    check(
                            helper,
                            washer.saveWithoutMetadata()
                                                    .getCompound("ic2_input_fluid")
                                                    .getInt("Amount")
                                            == 8000
                                    && menu.getSlot(9)
                                            .getItem()
                                            .is(stack("purified_copper").getItem())
                                    && menu.getSlot(6).getItem().isEmpty()
                                    && menu.getSlot(14).getItem().is(stack("fluid_cell").getItem()),
                            "Completed job makes exactly enough room for one full liter");
                    helper.succeed();
                });
    }

    /** The bottler's drain slot only starts when the entire container fits its tank. */
    @GameTest(template = "smoke", timeoutTicks = 160)
    public static void bottlerDrainUsesWholeContainer(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("fluid_bottler"));
        var bottler = machine(helper, pos);
        var menu = ((MenuProvider) bottler).createMenu(1, owner.getInventory(), owner);
        var port =
                bottler.getCapability(
                                ForgeCapabilities.FLUID_HANDLER, net.minecraft.core.Direction.UP)
                        .orElseThrow(AssertionError::new);
        check(
                helper,
                port.fill(new FluidStack(Fluids.WATER, 7751), FluidAction.EXECUTE) == 7751,
                "Bottler starts with 7751 mB");
        menu.getSlot(1).set(filledCell(new FluidStack(Fluids.WATER, 250)));
        var initial = bottler.saveWithoutMetadata();
        initial.putDouble("ic2_energy", 400);
        bottler.load(initial);
        helper.runAfterDelay(
                10,
                () -> {
                    var held = menu.getSlot(1).getItem();
                    check(
                            helper,
                            bottler.saveWithoutMetadata()
                                                    .getCompound("ic2_input_fluid")
                                                    .getInt("Amount")
                                            == 7751
                                    && held.is(stack("fluid_cell").getItem())
                                    && held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                                                    .orElseThrow(AssertionError::new)
                                                    .getFluidInTank(0)
                                                    .getAmount()
                                            == 250
                                    && menu.getSlot(9).getItem().isEmpty(),
                            "Bottler must not partially drain 250 mB into a 249 mB gap");
                    var state = bottler.saveWithoutMetadata();
                    state.getCompound("ic2_input_fluid").putInt("Amount", 7750);
                    bottler.load(state);
                });
        helper.runAfterDelay(
                120,
                () -> {
                    var returned = menu.getSlot(9).getItem();
                    check(
                            helper,
                            bottler.saveWithoutMetadata()
                                                    .getCompound("ic2_input_fluid")
                                                    .getInt("Amount")
                                            == 8000
                                    && menu.getSlot(1).getItem().isEmpty()
                                    && returned.is(stack("fluid_cell").getItem())
                                    && returned.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                                            .orElseThrow(AssertionError::new)
                                            .getFluidInTank(0)
                                            .isEmpty(),
                            "Bottler completes its normal job when all 250 mB fits");
                    helper.succeed();
                });
    }

    /** Coolant cells use whole-container 249/250 and 999/1000 capacity boundaries. */
    @GameTest(template = "smoke", batch = "fluidBoundaries", timeoutTicks = 90)
    public static void reactorCoolantWholeContainer(GameTestHelper helper) {
        var near = helper.absolutePos(BlockPos.ZERO);
        var core = new BlockPos((near.getX() & ~15) + 7, 150, (near.getZ() & ~15) + 7);
        var level = helper.getLevel();
        for (int x = -2; x <= 2; x++)
            for (int y = -2; y <= 2; y++)
                for (int z = -2; z <= 2; z++)
                    if (Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) == 2)
                        level.setBlockAndUpdate(
                                core.offset(x, y, z), block("reactor_vessel").defaultBlockState());
        level.setBlockAndUpdate(core, block("nuclear_reactor").defaultBlockState());
        for (var face : net.minecraft.core.Direction.values())
            level.setBlockAndUpdate(
                    core.relative(face), block("reactor_chamber").defaultBlockState());
        level.setBlockAndUpdate(core.above(2), block("reactor_fluid_port").defaultBlockState());
        var reactor = (com.industrialage.ia2.industrial.PowerNode) machine(helper, core);
        var coolant = ForgeRegistries.FLUIDS.getValue(new ResourceLocation("ic2", "coolant"));
        helper.runAfterDelay(
                25,
                () -> {
                    check(
                            helper,
                            reactor.isFluidReactor(),
                            "Intact six-chamber reactor must enter fluid mode");
                    var owner = player(helper, core);
                    var menu = ((MenuProvider) reactor).createMenu(1, owner.getInventory(), owner);
                    var saved = reactor.saveWithoutMetadata();
                    saved.putInt("coolant", 9751);
                    reactor.load(saved);
                    menu.getSlot(54).set(filledCell(new FluidStack(coolant, 250)));
                    helper.runAfterDelay(
                            5,
                            () -> {
                                check(
                                        helper,
                                        reactor.coolantAmount(false) == 9751
                                                && fluidAmount(menu.getSlot(54).getItem()) == 250
                                                && menu.getSlot(55).getItem().isEmpty(),
                                        "249 mB gap preserves a 250 mB coolant cell");
                                var state = reactor.saveWithoutMetadata();
                                state.putInt("coolant", 9750);
                                reactor.load(state);
                            });
                    helper.runAfterDelay(
                            10,
                            () -> {
                                check(
                                        helper,
                                        reactor.coolantAmount(false) == 10000
                                                && menu.getSlot(54).getItem().isEmpty()
                                                && menu.getSlot(55)
                                                        .getItem()
                                                        .is(stack("fluid_cell").getItem())
                                                && fluidAmount(menu.getSlot(55).getItem()) == 0,
                                        "250 mB gap accepts the whole coolant cell");
                                menu.getSlot(55).set(ItemStack.EMPTY);
                                var state = reactor.saveWithoutMetadata();
                                state.putInt("coolant", 9001);
                                reactor.load(state);
                                menu.getSlot(54).set(filledCell(new FluidStack(coolant, 1000)));
                            });
                    helper.runAfterDelay(
                            15,
                            () -> {
                                check(
                                        helper,
                                        reactor.coolantAmount(false) == 9001
                                                && fluidAmount(menu.getSlot(54).getItem()) == 1000
                                                && menu.getSlot(55).getItem().isEmpty(),
                                        "999 mB gap preserves a 1000 mB coolant cell");
                                var state = reactor.saveWithoutMetadata();
                                state.putInt("coolant", 9000);
                                reactor.load(state);
                            });
                    helper.runAfterDelay(
                            20,
                            () -> {
                                check(
                                        helper,
                                        reactor.coolantAmount(false) == 10000
                                                && menu.getSlot(54).getItem().isEmpty()
                                                && menu.getSlot(55)
                                                        .getItem()
                                                        .is(stack("fluid_cell").getItem())
                                                && fluidAmount(menu.getSlot(55).getItem()) == 0,
                                        "1000 mB gap accepts the whole coolant cell");
                                helper.succeed();
                            });
                });
    }

    /** Unconnected thermal machine prefills ten liters and keeps the eleventh cell. */
    @GameTest(template = "smoke", timeoutTicks = 40)
    public static void idleFluidHeaterPrefillsTank(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("fluid_heat_generator"));
        var heater = machine(helper, pos);
        var menu = ((MenuProvider) heater).createMenu(1, owner.getInventory(), owner);
        var cells = stack("lava_cell");
        cells.setCount(11);
        menu.getSlot(0).set(cells);
        helper.runAfterDelay(
                15,
                () -> {
                    check(
                            helper,
                            heater.saveWithoutMetadata()
                                                    .getCompound("ic2_input_fluid")
                                                    .getInt("Amount")
                                            == 10000
                                    && menu.getSlot(0).getItem().is(stack("lava_cell").getItem())
                                    && menu.getSlot(0).getItem().getCount() == 1
                                    && menu.getSlot(9).getItem().is(stack("fluid_cell").getItem())
                                    && menu.getSlot(9).getItem().getCount() == 10,
                            "Unconnected heater must prefill ten cells and keep the eleventh");
                    helper.succeed();
                });
    }

    /** A saturated semi-fluid generator fills ten liters, then retains the next cell. */
    @GameTest(template = "smoke", timeoutTicks = 40)
    public static void saturatedSemifluidPrefillsTank(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("semifluid_generator"));
        var generator = machine(helper, pos);
        var menu = ((MenuProvider) generator).createMenu(1, owner.getInventory(), owner);
        var biogas = ForgeRegistries.FLUIDS.getValue(new ResourceLocation("ic2", "biogas"));
        var port =
                generator
                        .getCapability(
                                ForgeCapabilities.FLUID_HANDLER, net.minecraft.core.Direction.UP)
                        .orElseThrow(AssertionError::new);
        check(
                helper,
                port.fill(new FluidStack(biogas, 9000), FluidAction.EXECUTE) == 9000,
                "Native pipe filled nine liters of biogas");
        var state = generator.saveWithoutMetadata();
        state.putDouble("ic2_energy", 16000);
        generator.load(state);
        menu.getSlot(0).set(filledCell(new FluidStack(biogas, 1000)));
        helper.runAfterDelay(
                8,
                () -> {
                    var current = generator.saveWithoutMetadata();
                    check(
                            helper,
                            current.getDouble("ic2_energy") == 16000
                                    && current.getCompound("ic2_input_fluid").getInt("Amount")
                                            == 10000
                                    && menu.getSlot(0).getItem().isEmpty()
                                    && menu.getSlot(9).getItem().is(stack("fluid_cell").getItem())
                                    && fluidAmount(menu.getSlot(9).getItem()) == 0,
                            "Saturated generator must prefill the tenth liter");
                    menu.getSlot(9).set(ItemStack.EMPTY);
                    menu.getSlot(0).set(filledCell(new FluidStack(biogas, 1000)));
                });
        helper.runAfterDelay(
                13,
                () -> {
                    check(
                            helper,
                            generator
                                                    .saveWithoutMetadata()
                                                    .getCompound("ic2_input_fluid")
                                                    .getInt("Amount")
                                            == 10000
                                    && fluidAmount(menu.getSlot(0).getItem()) == 1000
                                    && menu.getSlot(9).getItem().isEmpty(),
                            "A full ten-liter tank must preserve the next full cell");
                    helper.succeed();
                });
    }

    private static int fluidAmount(ItemStack item) {
        return item.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .orElseThrow(AssertionError::new)
                .getFluidInTank(0)
                .getAmount();
    }

    private static ItemStack filledCell(FluidStack fluid) {
        var empty = stack("fluid_cell");
        var handler =
                empty.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                        .orElseThrow(AssertionError::new);
        if (handler.fill(fluid, FluidAction.EXECUTE) != fluid.getAmount()) {
            throw new AssertionError("Could not fill universal cell");
        }
        return handler.getContainer().copy();
    }
}
