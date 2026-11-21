package com.industrialage.ia2.forge.qa;

import static com.industrialage.ia2.forge.qa.PlayableGameTests.check;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.machine;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.place;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.player;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.stack;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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

    /** Empty or blocked work never consumes a cell; two cells must not open together. */
    @GameTest(template = "smoke", timeoutTicks = 100)
    public static void washerUsesOneCellOnlyWhenWorkNeedsWater(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("ore_washing_plant"));
        var washer = machine(helper, pos);
        var menu = ((MenuProvider) washer).createMenu(1, owner.getInventory(), owner);
        var water = stack("water_cell");
        water.setCount(2);
        check(helper, menu.getSlot(6).mayPlace(water), "Washer must accept water cells");
        check(
                helper,
                !menu.getSlot(6).mayPlace(stack("lava_cell")),
                "Washer must reject lava cells");
        menu.getSlot(6).set(water);
        var saved = washer.saveWithoutMetadata();
        saved.putDouble("ic2_energy", 16000);
        washer.load(saved);
        helper.runAfterDelay(
                5,
                () -> {
                    check(
                            helper,
                            menu.getSlot(6).getItem().getCount() == 2,
                            "Idle washer opened a cell");
                    check(
                            helper,
                            menu.getSlot(14).getItem().isEmpty(),
                            "Idle washer returned a shell");
                    check(
                            helper,
                            !washer.saveWithoutMetadata().contains("ic2_input_fluid"),
                            "Idle washer filled water");
                    menu.getSlot(0).set(stack("crushed_copper"));
                });
        helper.runAfterDelay(
                12,
                () -> {
                    var state = washer.saveWithoutMetadata();
                    check(
                            helper,
                            menu.getSlot(6).getItem().getCount() == 1,
                            "Exactly one cell must remain");
                    check(
                            helper,
                            menu.getSlot(14).getItem().is(stack("empty_cell").getItem())
                                    && menu.getSlot(14).getItem().getCount() == 1,
                            "Exactly one empty cell must return");
                    check(
                            helper,
                            state.getCompound("ic2_input_fluid").getInt("Amount") == 1000,
                            "First cell must supply 1000 mB");
                    check(helper, state.getInt("ic2_progress") > 0, "Washer must actually start");
                    state.putInt("ic2_progress", 499);
                    washer.load(state);
                });
        helper.runAfterDelay(
                16,
                () -> {
                    var state = washer.saveWithoutMetadata();
                    check(
                            helper,
                            menu.getSlot(9).getItem().is(stack("purified_copper").getItem()),
                            "One real job completed");
                    check(
                            helper,
                            state.getCompound("ic2_input_fluid").getInt("Amount") == 900,
                            "One job leaves 900 mB");
                    check(
                            helper,
                            menu.getSlot(6).getItem().getCount() == 1
                                    && menu.getSlot(14).getItem().getCount() == 1,
                            "Unused second cell remains closed");
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
                                menu.getSlot(14).getItem().is(stack("empty_cell").getItem())
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
                            menu.getSlot(6).getItem().is(stack("water_cell").getItem()),
                            "Blocked products consumed a cell");
                    check(
                            helper,
                            menu.getSlot(14).getItem().isEmpty(),
                            "Blocked products returned a shell");
                    check(
                            helper,
                            !washer.saveWithoutMetadata().contains("ic2_input_fluid"),
                            "Blocked products filled water");
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
                            menu.getSlot(1).getItem().is(stack("empty_cell").getItem()),
                            "Lava cell shell was not returned");
                    check(
                            helper,
                            state.getDouble("eu") > 0 && state.getInt("millibuckets") > 0,
                            "Lava cell did not generate electricity");
                    helper.succeed();
                });
    }

    /** Pre-existing lava retains the next cell at either 7000 or 7001 mB. */
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
                port.fill(new FluidStack(Fluids.LAVA, 7000), FluidAction.EXECUTE) == 7000,
                "Pipe filled 7000 mB");
        menu.getSlot(0).set(stack("lava_cell"));
        helper.runAfterDelay(
                5,
                () -> {
                    var current = generator.saveWithoutMetadata();
                    check(
                            helper,
                            menu.getSlot(0).getItem().is(stack("lava_cell").getItem())
                                    && menu.getSlot(1).getItem().isEmpty()
                                    && current.getInt("millibuckets") < 7000
                                    && current.getDouble("eu") > 0,
                            "Queued cell stays closed while 7000 mB actually generates");
                    current.putInt("millibuckets", 7001);
                    generator.load(current);
                });
        helper.runAfterDelay(
                10,
                () -> {
                    var current = generator.saveWithoutMetadata();
                    check(
                            helper,
                            menu.getSlot(0).getItem().is(stack("lava_cell").getItem())
                                    && menu.getSlot(1).getItem().isEmpty()
                                    && current.getInt("millibuckets") < 7001,
                            "Queued cell stays closed while 7001 mB generates");
                    int room = 8000 - current.getInt("millibuckets");
                    check(
                            helper,
                            port.fill(new FluidStack(Fluids.LAVA, 2000), FluidAction.EXECUTE)
                                    == room,
                            "Pipe may still fill the whole remaining tank");
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

    /** Existing biogas is used before a second universal cell is opened. */
    @GameTest(template = "smoke", timeoutTicks = 25)
    public static void semifluidUsesTankBeforeQueuedCell(GameTestHelper helper) {
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
                    var pending = menu.getSlot(0).getItem();
                    check(
                            helper,
                            pending.is(stack("fluid_cell").getItem())
                                    && pending.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                                                    .orElseThrow(AssertionError::new)
                                                    .getFluidInTank(0)
                                                    .getAmount()
                                            == 1000
                                    && menu.getSlot(9).getItem().isEmpty()
                                    && state.getCompound("ic2_input_fluid").getInt("Amount") > 0
                                    && state.getCompound("ic2_input_fluid").getInt("Amount") < 1000
                                    && state.getDouble("ic2_energy") > 0,
                            "Biogas was consumed and next cell remained full");
                    helper.succeed();
                });
    }

    /** Air is paid per working tick; a second cell remains sealed while air remains. */
    @GameTest(template = "smoke", timeoutTicks = 25)
    public static void blastFurnaceUsesOneAirCellAtATime(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("blast_furnace"));
        var furnace = machine(helper, pos);
        var menu = ((MenuProvider) furnace).createMenu(1, owner.getInventory(), owner);
        menu.getSlot(0).set(new ItemStack(Items.IRON_INGOT));
        var air = stack("air_cell");
        air.setCount(2);
        menu.getSlot(4).set(air);
        var state = furnace.saveWithoutMetadata();
        state.putInt("ic2_heat", 60000);
        furnace.load(state);
        helper.runAfterDelay(
                8,
                () -> {
                    var current = furnace.saveWithoutMetadata();
                    int remaining = current.getCompound("ic2_input_fluid").getInt("Amount");
                    check(
                            helper,
                            menu.getSlot(4).getItem().getCount() == 1
                                    && menu.getSlot(13).getItem().is(stack("empty_cell").getItem())
                                    && menu.getSlot(13).getItem().getCount() == 1
                                    && remaining > 0
                                    && remaining < 1000
                                    && current.getInt("ic2_progress") > 0,
                            "Furnace consumed air but preserved the second cell");
                    helper.succeed();
                });
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
