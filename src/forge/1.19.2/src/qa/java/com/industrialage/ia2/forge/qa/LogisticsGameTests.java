package com.industrialage.ia2.forge.qa;

import static com.industrialage.ia2.forge.qa.PlayableGameTests.check;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.machine;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.place;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.player;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.stack;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.useOn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Exercises native tool and container interaction and the Forge-sided transport API. */
@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class LogisticsGameTests {
    private LogisticsGameTests() {}

    /**
     * Uses the physical wrench and checks simulation, capacity, direction and item data on a real
     * pipe.
     */
    @GameTest(template = "smoke", timeoutTicks = 20)
    public static void itemPipeNativePorts(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var player = player(helper, pos);
        place(helper, player, pos, stack("steel_item_pipe_small"));
        interact(helper, player, pos, Direction.EAST, stack("wrench"));

        var entity = machine(helper, pos);
        check(
                helper,
                entity.getBlockState().getValue(BlockStateProperties.FACING) == Direction.EAST,
                "Wrench changed the item outlet to the clicked face");
        var west =
                entity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST)
                        .orElseThrow(() -> new AssertionError("Missing input handler"));
        var east =
                entity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.EAST)
                        .orElseThrow(() -> new AssertionError("Missing output handler"));
        var tagged = new ItemStack(Items.DIAMOND, 64);
        tagged.getOrCreateTag().putInt("ic2_qa_marker", 9);
        check(
                helper,
                west.insertItem(0, tagged, true).getCount() == 60
                        && west.getStackInSlot(0).isEmpty(),
                "Simulation preserved the empty buffer");
        check(
                helper,
                east.insertItem(0, tagged, true).getCount() == 64,
                "Outlet rejected insertion");
        check(
                helper,
                west.insertItem(0, tagged, false).getCount() == 60
                        && west.getStackInSlot(0).getCount() == 4,
                "Small steel pipe accepted exactly four");
        check(
                helper,
                west.extractItem(0, 4, true).isEmpty(),
                "Input face rejected reverse extraction");
        var extracted = east.extractItem(0, 3, false);
        check(
                helper,
                extracted.getCount() == 3 && extracted.getTag().getInt("ic2_qa_marker") == 9,
                "Partial extraction conserved tagged diamonds");
        helper.getLevel().destroyBlock(pos, true, player);
        int dropped =
                helper
                        .getLevel()
                        .getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))
                        .stream()
                        .map(ItemEntity::getItem)
                        .filter(item -> item.is(Items.DIAMOND))
                        .mapToInt(ItemStack::getCount)
                        .sum();
        check(
                helper,
                dropped == 1 && west.getStackInSlot(0).isEmpty(),
                "Actual destruction released only the one remaining diamond");
        helper.succeed();
    }

    /**
     * Uses an actual water bucket and a cell, installs/removes a cover, and changes a live valve.
     */
    @GameTest(template = "smoke", timeoutTicks = 20)
    public static void nativeContainerAndCover(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 4, 1));
        var player = player(helper, pos);
        place(helper, player, pos, stack("bronze_fluid_pipe_medium"));
        var neighbor = pos.relative(Direction.EAST);
        place(helper, player, neighbor, stack("iron_tank"));

        var bucket = new ItemStack(Items.WATER_BUCKET);
        interact(helper, player, neighbor, Direction.NORTH, bucket);
        var tank = fluid(helper, neighbor, Direction.NORTH);
        check(
                helper,
                water(tank) == 1000
                        && player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET),
                "Native right-click filled the tank and returned an empty bucket");

        var cell = stack("empty_cell");
        interact(helper, player, neighbor, Direction.NORTH, cell);
        var contained =
                FluidUtil.getFluidContained(player.getItemInHand(InteractionHand.MAIN_HAND))
                        .orElse(FluidStack.EMPTY);
        check(
                helper,
                contained.getFluid() == Fluids.WATER
                        && contained.getAmount() == 1000
                        && tank.getFluidInTank(0).isEmpty(),
                "Cell filled from the native tank with 1000 mB");
        interact(
                helper,
                player,
                neighbor,
                Direction.NORTH,
                player.getItemInHand(InteractionHand.MAIN_HAND));
        check(
                helper,
                water(tank) == 1000
                        && FluidUtil.getFluidContained(
                                        player.getItemInHand(InteractionHand.MAIN_HAND))
                                .map(FluidStack::isEmpty)
                                .orElse(true),
                "Cell emptied into the native tank");

        var covers = stack("pump_cover_lv");
        covers.setCount(2);
        interact(helper, player, pos, Direction.WEST, covers);
        check(helper, covers.getCount() == 1, "First installation consumed one cover");
        interact(helper, player, pos, Direction.WEST, covers);
        check(helper, covers.getCount() == 1, "Repeated installation consumed no cover");
        var west = fluid(helper, pos, Direction.WEST);
        var water = new FluidStack(Fluids.WATER, 257);
        check(
                helper,
                west.fill(water, FluidAction.SIMULATE) == 257 && west.getFluidInTank(0).isEmpty(),
                "Fluid fill simulated without writing the stack");
        check(
                helper,
                west.fill(water, FluidAction.EXECUTE) == 257
                        && west.fill(new FluidStack(Fluids.LAVA, 1), FluidAction.EXECUTE) == 0,
                "Covered face accepted compatible water and rejected lava");
        check(
                helper,
                west.drain(1, FluidAction.EXECUTE).isEmpty(),
                "Covered face rejected reverse extraction");

        interact(helper, player, pos, Direction.WEST, stack("crowbar"));
        interact(helper, player, pos, Direction.WEST, stack("crowbar"));
        var dropped =
                helper
                        .getLevel()
                        .getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))
                        .stream()
                        .map(ItemEntity::getItem)
                        .filter(item -> item.is(stack("pump_cover_lv").getItem()))
                        .mapToInt(ItemStack::getCount)
                        .sum();
        check(
                helper,
                dropped == 1 && west.drain(1, FluidAction.SIMULATE).getAmount() == 1,
                "One removed cover dropped and its face unlocked");
        interact(helper, player, pos, Direction.EAST, stack("wrench"));
        var east = fluid(helper, pos, Direction.EAST);
        check(
                helper,
                east.drain(1, FluidAction.SIMULATE).isEmpty()
                        && !machine(helper, pos)
                                .getBlockState()
                                .getValue(BlockStateProperties.EAST),
                "Closed face restricted fluid and removed its visible arm");
        interact(helper, player, pos, Direction.EAST, stack("wrench"));
        check(
                helper,
                east.drain(1, FluidAction.SIMULATE).getAmount() == 1,
                "Wrench reopened the selected fluid face");
        helper.succeed();
    }

    /**
     * Measures real server ticks of both pump covers against stationary native tank inventories.
     */
    @GameTest(template = "smoke", timeoutTicks = 20)
    public static void nativePumpTransferRates(GameTestHelper helper) {
        var lv = pumpLine(helper, new BlockPos(0, 4, 0), "pump_cover_lv");
        var mv = pumpLine(helper, new BlockPos(0, 4, 2), "pump_cover_mv");
        helper.runAfterDelay(
                2,
                () -> {
                    int observedLv = water(lv);
                    int observedMv = water(mv);
                    helper.runAfterDelay(
                            1,
                            () -> {
                                check(
                                        helper,
                                        observedLv - water(lv) == 32,
                                        "LV moved exactly 32 mB in one server tick");
                                check(
                                        helper,
                                        observedMv - water(mv) == 128,
                                        "MV moved exactly 128 mB in one server tick");
                                helper.succeed();
                            });
                });
    }

    private static IFluidHandler pumpLine(GameTestHelper helper, BlockPos relative, String cover) {
        var source = helper.absolutePos(relative);
        var pipe = source.relative(Direction.EAST);
        var player = player(helper, pipe);
        place(helper, player, source, stack("bronze_tank"));
        place(helper, player, pipe, stack("bronze_fluid_pipe_medium"));
        interact(helper, player, pipe, Direction.WEST, stack(cover));
        var tank = fluid(helper, source, Direction.EAST);
        check(
                helper,
                tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 1000,
                "Native pump source started with 1,000 mB");
        return tank;
    }

    /** Builds both cells, then uses the universal one on a real tank and in-world source. */
    @GameTest(template = "smoke", timeoutTicks = 25)
    public static void universalCellNativeWorldAndTank(GameTestHelper helper) {
        var world = helper.getLevel();
        var bench = helper.absolutePos(new BlockPos(0, 3, 0));
        world.setBlockAndUpdate(bench, Blocks.CRAFTING_TABLE.defaultBlockState());
        var owner = player(helper, bench);
        var crafting =
                new CraftingMenu(
                        2, owner.getInventory(), ContainerLevelAccess.create(world, bench));
        owner.containerMenu = crafting;
        for (int slot : new int[] {2, 4, 6, 8}) {
            crafting.getSlot(slot).set(stack("tin_casing"));
        }
        crafting.getSlot(5).set(new ItemStack(Items.GLASS_PANE));
        check(
                helper,
                crafting.getSlot(0).getItem().is(stack("empty_cell").getItem()),
                "Original legacy pane recipe");
        crafting.getSlot(5).set(new ItemStack(Items.GLASS));
        var cell = crafting.getSlot(0).getItem().copy();
        check(
                helper,
                cell.is(stack("fluid_cell").getItem()),
                "Glass block center unlocked the separate recipe");

        var tankPos = bench.east(2);
        place(helper, owner, tankPos, stack("bronze_tank"));
        var tank = fluid(helper, tankPos, Direction.NORTH);
        var coolant = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse("ic2:coolant"));
        check(
                helper,
                tank.fill(new FluidStack(coolant, 257), FluidAction.EXECUTE) == 257,
                "Real coolant tank feed");
        interact(helper, owner, tankPos, Direction.NORTH, cell);
        var partlyFilled = owner.getItemInHand(InteractionHand.MAIN_HAND);
        var content = FluidUtil.getFluidContained(partlyFilled).orElse(FluidStack.EMPTY);
        check(
                helper,
                partlyFilled.is(cell.getItem())
                        && content.getFluid() == coolant
                        && content.getAmount() == 257,
                "Filled the original universal item with the exact partial amount");
        var itemPort = FluidUtil.getFluidHandler(partlyFilled).orElseThrow(AssertionError::new);
        check(
                helper,
                itemPort.fill(new FluidStack(Fluids.WATER, 600), FluidAction.SIMULATE) == 0,
                "Mixed fluid remained rejected");
        check(
                helper,
                itemPort.fill(new FluidStack(coolant, 1000), FluidAction.SIMULATE) == 743,
                "Simulated free capacity caps out at a liter");
        interact(helper, owner, tankPos, Direction.NORTH, partlyFilled);
        check(
                helper,
                tank.getFluidInTank(0).getAmount() == 257,
                "Returned the partial contents to the real tank");

        var source = bench.offset(2, 3, 3);
        world.setBlockAndUpdate(source.below(), Blocks.STONE.defaultBlockState());
        world.setBlockAndUpdate(source, Blocks.WATER.defaultBlockState());
        owner.setPos(source.getX() + 0.5, source.getY() + 1, source.getZ() + 0.5);
        owner.setXRot(90);
        var held = owner.getItemInHand(InteractionHand.MAIN_HAND);
        owner.gameMode.useItem(owner, world, held, InteractionHand.MAIN_HAND);
        held = owner.getItemInHand(InteractionHand.MAIN_HAND);
        check(
                helper,
                FluidUtil.getFluidContained(held).orElse(FluidStack.EMPTY).getAmount() == 1000
                        && world.getFluidState(source).isEmpty(),
                "Picked up a real world water source");
        owner.gameMode.useItem(owner, world, held, InteractionHand.MAIN_HAND);
        check(
                helper,
                world.getFluidState(source).isEmpty()
                        && FluidUtil.getFluidContained(
                                                owner.getItemInHand(InteractionHand.MAIN_HAND))
                                        .orElse(FluidStack.EMPTY)
                                        .getAmount()
                                == 1000,
                "Ordinary hand use retained the filled universal cell and left the world dry");
        world.setBlockAndUpdate(source, Blocks.AIR.defaultBlockState());
        var legacy = stack("water_cell");
        owner.setItemInHand(InteractionHand.MAIN_HAND, legacy);
        owner.gameMode.useItem(owner, world, legacy, InteractionHand.MAIN_HAND);
        check(
                helper,
                world.getFluidState(source).isEmpty()
                        && owner.getItemInHand(InteractionHand.MAIN_HAND)
                                .is(stack("water_cell").getItem()),
                "Ordinary hand use retained the named water cell and left the world dry");
        helper.succeed();
    }

    private static int water(IFluidHandler port) {
        var stack = port.getFluidInTank(0);
        return stack.getFluid() == Fluids.WATER ? stack.getAmount() : 0;
    }

    private static void interact(
            GameTestHelper helper,
            ServerPlayer player,
            BlockPos pos,
            Direction side,
            ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        check(
                helper,
                useOn(helper, player, pos, side, stack).consumesAction(),
                "Native right-click accepted " + stack.getItem());
    }

    private static IFluidHandler fluid(GameTestHelper helper, BlockPos pos, Direction side) {
        return machine(helper, pos)
                .getCapability(ForgeCapabilities.FLUID_HANDLER, side)
                .orElseThrow(() -> new AssertionError("Missing fluid port at " + pos));
    }
}
