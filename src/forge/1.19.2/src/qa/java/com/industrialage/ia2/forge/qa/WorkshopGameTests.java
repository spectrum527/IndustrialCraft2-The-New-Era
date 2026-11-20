package com.industrialage.ia2.forge.qa;

import static com.industrialage.ia2.forge.qa.PlayableGameTests.check;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.machine;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.place;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.player;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.stack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/** Exercises advanced recipes with actual Forge world inputs. */
@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class WorkshopGameTests {
    private WorkshopGameTests() {}

    /** Opens a scrap box by hand and with a real dispenser powered by redstone. */
    @GameTest(template = "smoke")
    public static void scrapBoxOpensByHandAndDispenser(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = helper.absolutePos(new BlockPos(2, 3, 2));
        var owner = player(helper, base);
        var box = stack("scrap_box");
        box.setCount(2);
        owner.setItemInHand(InteractionHand.MAIN_HAND, box);
        box.getItem().use(level, owner, InteractionHand.MAIN_HAND);
        var handArea = AABB.ofSize(owner.position(), 3, 5, 3);
        var manual = level.getEntitiesOfClass(ItemEntity.class, handArea);
        check(
                helper,
                owner.getMainHandItem().getCount() == 1
                        && manual.size() == 1
                        && manual.get(0).getItem().getCount() == 1
                        && !manual.get(0).getItem().is(box.getItem()),
                "Native item use consumed one box and dropped one real reward");
        for (var entity : manual) {
            entity.discard();
        }
        level.setBlockAndUpdate(
                base,
                Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.UP));
        var dispenser = (DispenserBlockEntity) level.getBlockEntity(base);
        dispenser.setItem(0, new ItemStack(box.getItem(), 2));
        level.setBlockAndUpdate(base.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.succeedWhen(
                () -> {
                    var output =
                            level.getEntitiesOfClass(
                                    ItemEntity.class,
                                    AABB.ofSize(Vec3.atCenterOf(base.above()), 3, 4, 3));
                    check(
                            helper,
                            dispenser.getItem(0).getCount() == 1
                                    && output.size() == 1
                                    && output.get(0).getItem().getCount() == 1
                                    && !output.get(0).getItem().is(box.getItem()),
                            "Redstone caused the vanilla dispenser to spend one box and eject one reward");
                });
    }

    /**
     * Sends exactly one coal's production through installed copper cable and extrudes a real ingot.
     */
    @GameTest(template = "smoke", timeoutTicks = 440)
    public static void metalFormerWithNativeGrid(GameTestHelper helper) {
        var source = helper.absolutePos(new BlockPos(0, 2, 0));
        var cable = source.relative(Direction.EAST);
        var former = cable.relative(Direction.EAST);
        var player = player(helper, cable);
        place(helper, player, source, stack("generator"));
        place(helper, player, cable, stack("insulated_copper_cable"));
        place(helper, player, former, stack("metal_former"));
        check(
                helper,
                items(helper, former, Direction.UP)
                        .insertItem(0, new ItemStack(Items.COPPER_INGOT), false)
                        .isEmpty(),
                "Former accepted an ingot");
        check(
                helper,
                items(helper, source, Direction.UP)
                        .insertItem(0, new ItemStack(Items.COAL), false)
                        .isEmpty(),
                "Generator accepted coal");
        helper.succeedWhen(
                () -> {
                    var sourceTag =
                            machine(helper, source).saveWithoutMetadata().getCompound("generator");
                    check(helper, sourceTag.getInt("fuel_ticks") == 0, "Await final burned tick");
                    var output = items(helper, former, Direction.DOWN).getStackInSlot(0);
                    check(
                            helper,
                            output.is(stack("copper_cable").getItem()) && output.getCount() == 3,
                            "Default extrusion produced three uninsulated copper cables");
                    check(
                            helper,
                            sourceTag.getDouble("stored_energy") == 0
                                    && machine(helper, former)
                                                    .saveWithoutMetadata()
                                                    .getDouble("ic2_energy")
                                            == 2000,
                            "One 4000-EU coal paid exactly 2000 EU for the 200-tick metal former job");
                });
    }

    /** Validates a water gate and all three ore-washing outputs. */
    @GameTest(template = "smoke", timeoutTicks = 560)
    public static void oreWasherNativeWaterGate(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        place(helper, player(helper, pos), pos, stack("ore_washing_plant"));
        var washer = machine(helper, pos);
        var loaded = washer.saveWithoutMetadata();
        loaded.putDouble("ic2_energy", 20000);
        washer.load(loaded);
        check(
                helper,
                items(helper, pos, Direction.UP)
                        .insertItem(0, stack("crushed_copper"), false)
                        .isEmpty(),
                "First input accepted crushed copper");
        helper.runAfterDelay(
                3,
                () -> {
                    var dry = washer.saveWithoutMetadata();
                    check(
                            helper,
                            dry.getInt("ic2_progress") == 0
                                    && dry.getDouble("ic2_energy") == 20_000,
                            "Dry washer preserved energy and input");
                    var port =
                            washer.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
                                    .orElseThrow(
                                            () -> new AssertionError("Washer has no water port"));
                    check(
                            helper,
                            port.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.SIMULATE) == 0,
                            "Washer rejected lava");
                    check(
                            helper,
                            port.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE)
                                    == 1000,
                            "Native port filled 1000 mB of water");
                });
        helper.succeedWhen(
                () -> {
                    var output = items(helper, pos, Direction.DOWN);
                    check(
                            helper,
                            output.getStackInSlot(0).is(stack("purified_copper").getItem())
                                    && output.getStackInSlot(0).getCount() == 1,
                            "One purified copper");
                    check(
                            helper,
                            output.getStackInSlot(1).is(stack("small_copper_dust").getItem())
                                    && output.getStackInSlot(1).getCount() == 2,
                            "Two small copper dust");
                    check(
                            helper,
                            output.getStackInSlot(2).is(stack("stone_dust").getItem())
                                    && output.getStackInSlot(2).getCount() == 1,
                            "One stone dust");
                    check(
                            helper,
                            !washer.saveWithoutMetadata().contains("ic2_input_fluid"),
                            "Job consumed precisely the liter");
                });
    }

    /** Samples one centrifuge batch against the independently measured preheated EU ledger. */
    @GameTest(template = "smoke", timeoutTicks = 530)
    public static void preheatedCentrifugeBatch(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 4, 1));
        place(helper, player(helper, pos), pos, stack("centrifuge"));
        var centrifuge = machine(helper, pos);
        var state = centrifuge.saveWithoutMetadata();
        state.putDouble("ic2_energy", 48000);
        state.putInt("ic2_heat", 1000);
        centrifuge.load(state);
        check(
                helper,
                items(helper, pos, Direction.UP)
                        .insertItem(0, stack("crushed_tin"), false)
                        .isEmpty(),
                "Centrifuge accepted crushed tin");
        helper.succeedWhen(
                () -> {
                    var output = items(helper, pos, Direction.DOWN);
                    check(
                            helper,
                            output.getStackInSlot(0).is(stack("tin_dust").getItem()),
                            "Centrifuge tin dust");
                    check(
                            helper,
                            output.getStackInSlot(1).is(stack("small_iron_dust").getItem()),
                            "Small iron dust");
                    check(
                            helper,
                            output.getStackInSlot(2).is(stack("stone_dust").getItem()),
                            "Stone dust");
                    var saved = centrifuge.saveWithoutMetadata();
                    check(
                            helper,
                            saved.getDouble("ic2_energy") == 23501
                                    && saved.getInt("ic2_heat") == 1001,
                            "500 native job ticks used exactly 24499 EU including heat support");
                });
    }

    /** Fits an actual native upgrade item and checks its duration and exact paid EU balance. */
    @GameTest(template = "smoke", timeoutTicks = 165)
    public static void overclockedFormerPaysRealEu(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 4, 1));
        var owner = player(helper, pos);
        place(helper, owner, pos, stack("metal_former"));
        var former = machine(helper, pos);
        var saved = former.saveWithoutMetadata();
        saved.putDouble("ic2_energy", 4000);
        former.load(saved);
        var menu = ((MenuProvider) former).createMenu(4, owner.getInventory(), owner);
        var upgrade = stack("overclocker_upgrade");
        check(
                helper,
                menu.getSlot(18).mayPlace(upgrade),
                "Allows the speed upgrade in its native menu");
        check(
                helper,
                !menu.getSlot(18).mayPlace(stack("fluid_ejector_upgrade")),
                "Dry processor rejects the fluid upgrade");
        menu.getSlot(18).set(upgrade);
        check(
                helper,
                items(helper, pos, Direction.UP)
                        .insertItem(0, new ItemStack(Items.COPPER_INGOT), false)
                        .isEmpty(),
                "Native ingot accepted");
        helper.succeedWhen(
                () -> {
                    var result = items(helper, pos, Direction.DOWN).getStackInSlot(0);
                    check(
                            helper,
                            result.is(stack("copper_cable").getItem()) && result.getCount() == 3,
                            "One overclocker yields the actual job before tick 165");
                    check(
                            helper,
                            former.saveWithoutMetadata().getDouble("ic2_energy") == 1760,
                            "Paid exactly 140 ticks at 16 EU");
                });
    }

    /** Tracks completed native coal conversion into 8,000 stored heat and one observed ash. */
    @GameTest(template = "smoke", timeoutTicks = 430)
    public static void nativeSolidHeatWithoutConsumer(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 4, 1));
        place(helper, player(helper, pos), pos, stack("solid_heat_generator"));
        var source = machine(helper, pos);
        check(
                helper,
                ItemHandlerHelper.insertItemStacked(
                                items(helper, pos, Direction.NORTH),
                                new ItemStack(Items.COAL),
                                false)
                        .isEmpty(),
                "Heat source accepted coal");
        helper.succeedWhen(
                () -> {
                    var saved = source.saveWithoutMetadata();
                    var ash = items(helper, pos, Direction.DOWN).getStackInSlot(0);
                    check(
                            helper,
                            saved.getInt("ic2_heat") == 8000
                                    && saved.getInt("ic2_heat_fuel") == 0
                                    && ash.is(stack("ashes").getItem())
                                    && ash.getCount() == 1,
                            "One coal supplied 400 work ticks and 8000 buffered heat");
                });
    }

    private static IItemHandler items(GameTestHelper helper, BlockPos pos, Direction side) {
        return machine(helper, pos)
                .getCapability(ForgeCapabilities.ITEM_HANDLER, side)
                .orElseThrow(() -> new AssertionError("Missing workshop item port at " + pos));
    }
}
