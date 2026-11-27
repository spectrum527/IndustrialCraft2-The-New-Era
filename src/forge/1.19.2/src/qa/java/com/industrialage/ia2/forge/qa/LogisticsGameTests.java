package com.industrialage.ia2.forge.qa;

import static com.industrialage.ia2.forge.qa.PlayableGameTests.check;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.machine;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.place;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.player;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.stack;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.useOn;

import com.industrialage.ia2.logistics.TransportNode;
import com.industrialage.ia2.wrenchtest.WrenchHits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.BlockItem;
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
        var level = helper.getLevel();
        level.setBlockAndUpdate(
                pos,
                level.getBlockState(pos).setValue(BlockStateProperties.FACING, Direction.NORTH));
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
        crafting.getSlot(1).set(stack("tin_casing"));
        crafting.getSlot(2).set(new ItemStack(Items.GLASS_PANE));
        crafting.getSlot(3).set(stack("tin_casing"));
        check(
                helper,
                crafting.getSlot(0).getItem().is(stack("empty_cell").getItem()),
                "Basic cells are made from a pane between two tin casings");
        for (int slot : new int[] {1, 2, 3}) crafting.getSlot(slot).set(ItemStack.EMPTY);
        for (int slot : new int[] {2, 4, 6, 8}) {
            crafting.getSlot(slot).set(stack("tin_casing"));
        }
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
                held.is(stack("water_cell").getItem())
                        && FluidUtil.getFluidContained(held).orElse(FluidStack.EMPTY).getAmount()
                                == 1000
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

    /**
     * Verifies multi-segment BFS item pipe routing, extraction nozzle OUT flip, and sink isolation.
     */
    @GameTest(template = "smoke", timeoutTicks = 40)
    public static void itemPipeBfsRoutingAndExtractionNozzleFlip(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(1, 2, 1));
        var player = player(helper, origin);
        var sourcePos = origin;
        var pipe1 = origin.east();
        var pipe2 = origin.east(2);
        var pipe3 = origin.east(3);
        var destPos = origin.east(4);
        var isolatedPos = pipe2.north();

        level.setBlockAndUpdate(sourcePos, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(destPos, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(isolatedPos, Blocks.CHEST.defaultBlockState());
        var pipeBlock =
                ForgeRegistries.BLOCKS.getValue(
                        ResourceLocation.fromNamespaceAndPath("ic2", "bronze_item_pipe_small"));
        level.setBlockAndUpdate(
                pipe1,
                pipeBlock
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.WEST));
        level.setBlockAndUpdate(
                pipe2,
                pipeBlock
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(
                pipe3,
                pipeBlock
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST));

        interact(helper, player, pipe1, Direction.WEST, stack("pipe_extraction_nozzle"));
        check(
                helper,
                level.getBlockState(pipe1).getValue(BlockStateProperties.FACING) == Direction.EAST,
                "Installing an extraction nozzle onto OUT auto-flipped OUT toward the linked pipe");

        var sourceChest =
                (net.minecraft.world.level.block.entity.ChestBlockEntity)
                        level.getBlockEntity(sourcePos);
        sourceChest.setItem(0, new ItemStack(Items.IRON_INGOT, 8));

        helper.onEachTick(
                () -> {
                    var destChest =
                            (net.minecraft.world.level.block.entity.ChestBlockEntity)
                                    level.getBlockEntity(destPos);
                    var sideChest =
                            (net.minecraft.world.level.block.entity.ChestBlockEntity)
                                    level.getBlockEntity(isolatedPos);
                    if (!sideChest.getItem(0).isEmpty()) {
                        helper.fail("Non-OUT adjacent chest received leaked items");
                    }
                    if (destChest.getItem(0).getCount() == 8 && sourceChest.getItem(0).isEmpty()) {
                        helper.succeed();
                    }
                });
    }

    /**
     * Verifies a single-segment item pipe with an extraction nozzle redirects air-facing OUT and
     * delivers without stalling across straight, 90-degree bend, and late-placed target layouts.
     */
    @GameTest(template = "smoke", timeoutTicks = 60)
    public static void singleSegmentExtractionPipeDeliversWithoutStalling(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper, helper.absolutePos(new BlockPos(2, 2, 2)));
        var pipeBlock =
                ForgeRegistries.BLOCKS.getValue(
                        ResourceLocation.fromNamespaceAndPath("ic2", "bronze_item_pipe_small"));

        var straightPipe = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(straightPipe.west(), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(straightPipe.east(), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(
                straightPipe,
                pipeBlock
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH));
        interact(helper, player, straightPipe, Direction.WEST, stack("pipe_extraction_nozzle"));
        check(
                helper,
                level.getBlockState(straightPipe).getValue(BlockStateProperties.FACING)
                        == Direction.EAST,
                "Straight single-segment extraction nozzle redirected air-facing OUT to EAST");
        ((net.minecraft.world.Container) level.getBlockEntity(straightPipe.west()))
                .setItem(0, new ItemStack(Items.IRON_INGOT, 6));

        var bendPipe = helper.absolutePos(new BlockPos(1, 2, 4));
        level.setBlockAndUpdate(bendPipe.west(), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(bendPipe.south(), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(
                bendPipe,
                pipeBlock
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST));
        interact(helper, player, bendPipe, Direction.WEST, stack("pipe_extraction_nozzle"));
        check(
                helper,
                level.getBlockState(bendPipe).getValue(BlockStateProperties.FACING)
                        == Direction.SOUTH,
                "90-degree single-segment extraction nozzle redirected OUT to SOUTH target");
        level.setBlockAndUpdate(bendPipe.north(), Blocks.BARREL.defaultBlockState());
        ((net.minecraft.world.Container) level.getBlockEntity(bendPipe.north()))
                .setItem(0, new ItemStack(Items.GOLD_INGOT, 3));
        ((net.minecraft.world.Container) level.getBlockEntity(bendPipe.west()))
                .setItem(0, new ItemStack(Items.COPPER_INGOT, 5));

        var latePipe = helper.absolutePos(new BlockPos(5, 2, 2));
        level.setBlockAndUpdate(latePipe.west(), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(
                latePipe,
                pipeBlock
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH));
        interact(helper, player, latePipe, Direction.WEST, stack("pipe_extraction_nozzle"));
        ((net.minecraft.world.Container) level.getBlockEntity(latePipe.west()))
                .setItem(0, new ItemStack(Items.EMERALD, 4));

        boolean[] placedLate = {false};
        helper.onEachTick(
                () -> {
                    var bendBystander =
                            (net.minecraft.world.Container) level.getBlockEntity(bendPipe.north());
                    if (bendBystander.getItem(0).getCount() != 3) {
                        helper.fail("Extraction-nozzled bend pipe pulled from bystander container");
                    }
                    if (!placedLate[0]) {
                        var cargo =
                                machine(helper, latePipe)
                                        .getCapability(
                                                ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                                        .map(h -> h.getStackInSlot(0))
                                        .orElse(ItemStack.EMPTY);
                        if (cargo.getCount() == 4) {
                            level.setBlockAndUpdate(
                                    latePipe.south(), Blocks.BARREL.defaultBlockState());
                            placedLate[0] = true;
                        }
                        return;
                    }
                    var straightDst =
                            (net.minecraft.world.Container)
                                    level.getBlockEntity(straightPipe.east());
                    var bendDst =
                            (net.minecraft.world.Container) level.getBlockEntity(bendPipe.south());
                    var lateDst =
                            (net.minecraft.world.Container) level.getBlockEntity(latePipe.south());
                    if (straightDst.getItem(0).getCount() == 6
                            && bendDst.getItem(0).getCount() == 5
                            && lateDst.getItem(0).getCount() == 4
                            && level.getBlockState(latePipe).getValue(BlockStateProperties.FACING)
                                    == Direction.SOUTH) {
                        helper.succeed();
                    }
                });
    }

    /**
     * Verifies native rotor durability, biogas_cell, copper_ingot removal, and c:/forge: tag
     * matching.
     */
    @GameTest(template = "smoke", timeoutTicks = 20)
    public static void rotorDurabilityBiogasCopperAndDynamicTags(GameTestHelper helper) {
        check(helper, stack("wooden_rotor").getMaxDamage() == 10_800, "wooden_rotor durability");
        check(helper, stack("bronze_rotor").getMaxDamage() == 86_400, "bronze_rotor durability");
        check(helper, stack("iron_rotor").getMaxDamage() == 86_400, "iron_rotor durability");
        check(helper, stack("steel_rotor").getMaxDamage() == 172_800, "steel_rotor durability");
        check(helper, stack("carbon_rotor").getMaxDamage() == 604_800, "carbon_rotor durability");
        check(helper, stack("steam_turbine").getMaxDamage() == 0, "steam_turbine has no wear");

        check(
                helper,
                ForgeRegistries.ITEMS.containsKey(
                        ResourceLocation.fromNamespaceAndPath("ic2", "biogas_cell")),
                "ic2:biogas_cell is registered");
        check(
                helper,
                !ForgeRegistries.ITEMS.containsKey(
                        ResourceLocation.fromNamespaceAndPath("ic2", "copper_ingot")),
                "ic2:copper_ingot is removed");

        var tin = stack("tin_ingot");
        var rawTin = stack("raw_tin");
        var steelBlock = stack("steel_block");
        check(
                helper,
                com.industrialage.ia2.industrial.WorkshopPlatform.tagged(tin, "forge:ingots/tin")
                        && com.industrialage.ia2.industrial.WorkshopPlatform.tagged(
                                tin, "c:ingots/tin"),
                "tin_ingot matches both forge: and c: ingot tags");
        check(
                helper,
                com.industrialage.ia2.industrial.WorkshopPlatform.tagged(
                                rawTin, "forge:raw_materials/tin")
                        && com.industrialage.ia2.industrial.WorkshopPlatform.tagged(
                                rawTin, "c:raw_ores/tin"),
                "raw_tin matches both raw_materials and raw_ores aliases");
        check(
                helper,
                com.industrialage.ia2.industrial.WorkshopPlatform.tagged(
                                steelBlock, "forge:storage_blocks/steel")
                        && com.industrialage.ia2.industrial.WorkshopPlatform.tagged(
                                steelBlock, "c:blocks/steel"),
                "steel_block matches both storage_blocks and blocks aliases");
        helper.succeed();
    }

    /**
     * Verifies Miner preserves waterlogging on mining_pipe / mining_pipe_tip during descent and
     * withdrawal.
     */
    @GameTest(template = "smoke", timeoutTicks = 120)
    public static void wetMinerPreservesMiningPipeWaterlogging(GameTestHelper helper) {
        var level = helper.getLevel();
        var minerPos = helper.absolutePos(new BlockPos(2, 130, 2));
        var player = player(helper, minerPos);
        var pipe = PlayableGameTests.block("mining_pipe");
        var tip = PlayableGameTests.block("mining_pipe_tip");

        for (int depth = 1; depth <= 2; depth++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        level.setBlockAndUpdate(
                                minerPos.offset(dx, -depth, dz), Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }
        level.setBlockAndUpdate(minerPos.below(3), Blocks.BEDROCK.defaultBlockState());
        level.setBlockAndUpdate(minerPos.below(), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(minerPos.below(2), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(minerPos.north(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(minerPos, PlayableGameTests.block("miner").defaultBlockState());
        var menu =
                ((net.minecraft.world.MenuProvider) level.getBlockEntity(minerPos))
                        .createMenu(7, player.getInventory(), player);
        menu.getSlot(0).set(stack("drill"));
        var pipes = stack("mining_pipe");
        pipes.setCount(2);
        menu.getSlot(2).set(pipes);
        ((com.industrialage.ia2.power.WorldPowerPort) level.getBlockEntity(minerPos)).receive(2000);

        boolean[] stage = {false, false};
        helper.onEachTick(
                () -> {
                    if (!stage[0]) {
                        var lower = level.getBlockState(minerPos.below(2));
                        if (lower.is(tip)) {
                            var upper = level.getBlockState(minerPos.below());
                            check(
                                    helper,
                                    upper.is(pipe)
                                            && upper.getValue(BlockStateProperties.WATERLOGGED)
                                            && level.getFluidState(minerPos.below()).isSource()
                                            && lower.getValue(BlockStateProperties.WATERLOGGED)
                                            && level.getFluidState(minerPos.below(2)).isSource(),
                                    "Miner preserved WATERLOGGED on descent through water");
                            stage[0] = true;
                            level.setBlockAndUpdate(
                                    minerPos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                        }
                        return;
                    }
                    if (!stage[1]) {
                        var lower = level.getBlockState(minerPos.below(2));
                        if (lower.is(Blocks.WATER)) {
                            var upper = level.getBlockState(minerPos.below());
                            check(
                                    helper,
                                    level.getFluidState(minerPos.below(2)).isSource()
                                            && upper.is(tip)
                                            && upper.getValue(BlockStateProperties.WATERLOGGED)
                                            && level.getFluidState(minerPos.below()).isSource(),
                                    "Miner preserved WATERLOGGED during withdrawal");
                            stage[1] = true;
                        }
                        return;
                    }
                    if (level.getBlockState(minerPos.below()).is(Blocks.WATER)) {
                        check(
                                helper,
                                level.getFluidState(minerPos.below()).isSource()
                                        && level.getFluidState(minerPos.below(2)).isSource(),
                                "Miner restored both water sources on full withdrawal");
                        helper.succeed();
                    }
                });
    }

    /**
     * Verifies waterlogging, fluid tick propagation, and flowing-water/lava survival for item
     * pipes, fluid pipes, and mining pipes.
     */
    @GameTest(template = "smoke", timeoutTicks = 40)
    public static void pipesAndMiningPipesWaterlogging(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(1, 120, 1));
        var player = player(helper, origin);

        PipeGeometryChecks.run(level, PlayableGameTests::block);

        var itemPipe = PlayableGameTests.block("bronze_item_pipe_tiny");
        var fluidPipe = PlayableGameTests.block("bronze_fluid_pipe_medium");
        var miningPipe = PlayableGameTests.block("mining_pipe");
        var miningTip = PlayableGameTests.block("mining_pipe_tip");
        check(
                helper,
                miningPipe instanceof SimpleWaterloggedBlock
                        && miningTip instanceof SimpleWaterloggedBlock
                        && !miningPipe
                                .defaultBlockState()
                                .getValue(BlockStateProperties.WATERLOGGED)
                        && !miningTip
                                .defaultBlockState()
                                .getValue(BlockStateProperties.WATERLOGGED),
                "Mining pipes implement SimpleWaterloggedBlock with WATERLOGGED=false default");

        var wetItemPos = origin;
        var wetFluidPos = origin.south(4);
        var wetMiningPos = origin.south(8);
        var flowSource = origin.east(6);

        for (var pos : new BlockPos[] {wetItemPos, wetFluidPos, wetMiningPos}) {
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlockAndUpdate(
                            pos.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                    boolean wall = dx == -1 || dx == 2 || dz == -1 || dz == 1;
                    level.setBlockAndUpdate(
                            pos.offset(dx, 0, dz),
                            (wall || dx == 1 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
                }
            }
        }

        verifyWaterloggedPlacementAndToggle(helper, player, wetItemPos, itemPipe, "item_pipe");
        verifyWaterloggedPlacementAndToggle(helper, player, wetFluidPos, fluidPipe, "fluid_pipe");
        verifyWaterloggedPlacementAndToggle(
                helper, player, wetMiningPos, miningPipe, "mining_pipe");

        // Non-sneak water bucket on item pipe does not get swallowed by TransportBlock.interact.
        player.setShiftKeyDown(false);
        var itemBucket = new ItemStack(Items.WATER_BUCKET);
        player.setPos(wetItemPos.getX() + 0.5, wetItemPos.getY() + 1, wetItemPos.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, itemBucket);
        check(
                helper,
                !useOn(helper, player, wetItemPos, Direction.UP, itemBucket).consumesAction(),
                "Item pipe defers water bucket to vanilla waterlogging");
        // Normal water bucket on medium fluid pipe still fills its internal fluid buffer.
        var fluidBucket = new ItemStack(Items.WATER_BUCKET);
        player.setPos(wetFluidPos.getX() + 0.5, wetFluidPos.getY() + 1, wetFluidPos.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, fluidBucket);
        check(
                helper,
                useOn(helper, player, wetFluidPos, Direction.UP, fluidBucket).consumesAction()
                        && water(fluid(helper, wetFluidPos, Direction.UP)) == 1000,
                "Fluid pipe retains normal water bucket filling");
        // Sneak + water bucket on fluid pipe bypasses Block.use so vanilla waterlogging still
        // works.
        var sneakBucket = new ItemStack(Items.WATER_BUCKET);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, sneakBucket);
        check(
                helper,
                !useOn(helper, player, wetFluidPos, Direction.UP, sneakBucket).consumesAction(),
                "Sneak water bucket on fluid pipe defers to vanilla waterlogging");
        player.setShiftKeyDown(false);

        // Re-waterlog all three and open east wall to confirm updateShape schedules water ticks.
        for (var entry :
                new Object[][] {
                    {wetItemPos, itemPipe},
                    {wetFluidPos, fluidPipe},
                    {wetMiningPos, miningPipe}
                }) {
            var pos = (BlockPos) entry[0];
            var block = (SimpleWaterloggedBlock) entry[1];
            check(
                    helper,
                    block.placeLiquid(
                                    level,
                                    pos,
                                    level.getBlockState(pos),
                                    Fluids.WATER.getSource(false))
                            && level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED),
                    "Re-waterlogged " + pos);
            level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
            check(
                    helper,
                    level.getFluidTicks().hasScheduledTick(pos, Fluids.WATER),
                    "updateShape scheduled fluid tick for " + pos);
        }

        // Flowing water basin with dry item pipe, fluid pipe, and mining pipe adjacent to source.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockAndUpdate(
                        flowSource.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                boolean wall = dx == -2 || dx == 2 || dz == -2 || dz == 2;
                level.setBlockAndUpdate(
                        flowSource.offset(dx, 0, dz),
                        (wall ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            }
        }
        level.setBlockAndUpdate(flowSource.east(), itemPipe.defaultBlockState());
        level.setBlockAndUpdate(flowSource.west(), fluidPipe.defaultBlockState());
        level.setBlockAndUpdate(flowSource.south(), miningPipe.defaultBlockState());
        level.setBlockAndUpdate(flowSource, Blocks.WATER.defaultBlockState());

        helper.onEachTick(
                () -> {
                    if (level.getFluidState(wetItemPos.east()).getType() == Fluids.FLOWING_WATER
                            && level.getFluidState(wetFluidPos.east()).getType()
                                    == Fluids.FLOWING_WATER
                            && level.getFluidState(wetMiningPos.east()).getType()
                                    == Fluids.FLOWING_WATER
                            && level.getFluidState(flowSource.north()).getType()
                                    == Fluids.FLOWING_WATER) {
                        check(
                                helper,
                                level.getBlockState(flowSource.east()).is(itemPipe)
                                        && !level.getBlockState(flowSource.east())
                                                .getValue(BlockStateProperties.WATERLOGGED)
                                        && level.getBlockState(flowSource.west()).is(fluidPipe)
                                        && !level.getBlockState(flowSource.west())
                                                .getValue(BlockStateProperties.WATERLOGGED)
                                        && level.getBlockState(flowSource.south()).is(miningPipe)
                                        && !level.getBlockState(flowSource.south())
                                                .getValue(BlockStateProperties.WATERLOGGED),
                                "Flowing water did not wash away item, fluid, or mining pipes");
                        helper.succeed();
                    }
                });
    }

    /**
     * Verifies auto-routing upward vertical climbs (straight up placement against overhead sink,
     * opposing OUT, top-down opposing chain), horizontal->UP->horizontal, and DOWN->side->UP.
     */
    @GameTest(template = "smoke", batch = "pipeRouting", timeoutTicks = 120)
    public static void itemPipeVerticalUpwardAutoRouting(GameTestHelper helper) {
        var level = helper.getLevel();
        var pipeBlock = PlayableGameTests.block("bronze_item_pipe_small");
        var base = helper.absolutePos(new BlockPos(1, 30, 1));
        var player = player(helper, base);

        // 1. Straight UP placed via real BlockPlaceContext against overhead barrel bottom.
        var upSrc = base;
        var upDst = upSrc.above(4);
        level.setBlockAndUpdate(upSrc, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(upDst, Blocks.BARREL.defaultBlockState());
        placePipeAgainst(helper, player, pipeBlock, upSrc, Direction.UP);
        placePipeAgainst(helper, player, pipeBlock, upSrc.above(), Direction.UP);
        placePipeAgainst(helper, player, pipeBlock, upDst, Direction.DOWN);
        check(
                helper,
                level.getBlockState(upSrc.above(3)).getValue(BlockStateProperties.FACING)
                        == Direction.UP,
                "Vertical UP pipe placed against overhead sink faces UP into the barrel");
        ((Container) level.getBlockEntity(upSrc)).setItem(0, new ItemStack(Items.DIAMOND, 12));

        // 2. Straight UP where top pipe OUT points DOWN (opposing lower pipe).
        var oppSrc = base.east(3);
        var oppDst = oppSrc.above(4);
        level.setBlockAndUpdate(oppSrc, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(oppDst, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, oppSrc.above(1), Direction.UP);
        setPipe(level, pipeBlock, oppSrc.above(2), Direction.UP);
        setPipe(level, pipeBlock, oppSrc.above(3), Direction.DOWN);
        ((Container) level.getBlockEntity(oppSrc)).setItem(0, new ItemStack(Items.EMERALD, 12));

        // 3. Top-down opposing chain: P1(UP) -> P2(DOWN) -> P3(DOWN) -> overhead barrel.
        var topDownSrc = base.east(6);
        var topDownDst = topDownSrc.above(4);
        level.setBlockAndUpdate(topDownSrc, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(topDownDst, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, topDownSrc.above(1), Direction.UP);
        setPipe(level, pipeBlock, topDownSrc.above(2), Direction.DOWN);
        setPipe(level, pipeBlock, topDownSrc.above(3), Direction.DOWN);
        ((Container) level.getBlockEntity(topDownSrc))
                .setItem(0, new ItemStack(Items.PRISMARINE_SHARD, 12));

        // 4. Horizontal -> UP -> horizontal (terminal pipe OUT=UP into air, sink on EAST).
        var climbSrc = base.south(4);
        var climbDst = climbSrc.east(3).above();
        level.setBlockAndUpdate(climbSrc, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, climbSrc.east(), Direction.EAST);
        setPipe(level, pipeBlock, climbSrc.east(2), Direction.EAST);
        setPipe(level, pipeBlock, climbSrc.east(2).above(), Direction.UP);
        level.setBlockAndUpdate(climbDst, Blocks.BARREL.defaultBlockState());
        ((Container) level.getBlockEntity(climbSrc))
                .setItem(0, new ItemStack(Items.IRON_INGOT, 16));

        // 5. DOWN -> side -> UP (1-block gap so the rising leg is a true terminal pipe, unrotated
        // terminal OUT=UP into air).
        var dipSrc = base.offset(5, 2, 4);
        var dipDst = dipSrc.below().east(3);
        level.setBlockAndUpdate(dipSrc, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, dipSrc.below(), Direction.DOWN);
        setPipe(level, pipeBlock, dipSrc.below(2), Direction.DOWN);
        setPipe(level, pipeBlock, dipSrc.below(2).east(), Direction.EAST);
        setPipe(level, pipeBlock, dipSrc.below(2).east(2), Direction.EAST);
        setPipe(level, pipeBlock, dipSrc.below().east(2), Direction.UP);
        level.setBlockAndUpdate(dipDst, Blocks.BARREL.defaultBlockState());
        ((Container) level.getBlockEntity(dipSrc))
                .setItem(0, new ItemStack(Items.COPPER_INGOT, 12));

        helper.onEachTick(
                () -> {
                    var upBox = (Container) level.getBlockEntity(upDst);
                    var oppBox = (Container) level.getBlockEntity(oppDst);
                    var topDownBox = (Container) level.getBlockEntity(topDownDst);
                    var climbBox = (Container) level.getBlockEntity(climbDst);
                    var dipBox = (Container) level.getBlockEntity(dipDst);
                    if (upBox.countItem(Items.DIAMOND) == 12
                            && ((Container) level.getBlockEntity(upSrc)).isEmpty()
                            && oppBox.countItem(Items.EMERALD) == 12
                            && ((Container) level.getBlockEntity(oppSrc)).isEmpty()
                            && level.getBlockState(oppSrc.above(3))
                                            .getValue(BlockStateProperties.FACING)
                                    == Direction.UP
                            && topDownBox.countItem(Items.PRISMARINE_SHARD) == 12
                            && ((Container) level.getBlockEntity(topDownSrc)).isEmpty()
                            && level.getBlockState(topDownSrc.above(3))
                                            .getValue(BlockStateProperties.FACING)
                                    == Direction.UP
                            && climbBox.countItem(Items.IRON_INGOT) == 16
                            && ((Container) level.getBlockEntity(climbSrc)).isEmpty()
                            && dipBox.countItem(Items.COPPER_INGOT) == 12
                            && ((Container) level.getBlockEntity(dipSrc)).isEmpty()) {
                        helper.succeed();
                    }
                });
    }

    /**
     * Verifies L/Z/U corners, T-junction backpressure failover, and decoy isolation when an
     * explicit outlet is full.
     */
    @GameTest(template = "smoke", batch = "pipeRouting", timeoutTicks = 120)
    public static void itemPipeCornersTeeFailoverAndDecoyIsolation(GameTestHelper helper) {
        var level = helper.getLevel();
        var pipeBlock = PlayableGameTests.block("bronze_item_pipe_small");
        var base = helper.absolutePos(new BlockPos(1, 55, 1));

        // 1. Horizontal L/Z-turn (zero nozzles).
        var zSrc = base.south(2);
        var zDst = zSrc.east(3).north(2);
        level.setBlockAndUpdate(zSrc, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, zSrc.east(), Direction.EAST);
        setPipe(level, pipeBlock, zSrc.east(2), Direction.EAST);
        setPipe(level, pipeBlock, zSrc.east(2).north(), Direction.NORTH);
        setPipe(level, pipeBlock, zSrc.east(3).north(), Direction.EAST);
        level.setBlockAndUpdate(zDst, Blocks.BARREL.defaultBlockState());
        ((Container) level.getBlockEntity(zSrc)).setItem(0, new ItemStack(Items.GOLD_INGOT, 12));

        // 2. Horizontal U-turn (zero nozzles, 1-block gap).
        var uSrc = base.south(5);
        var uDst = uSrc.south(2);
        level.setBlockAndUpdate(uSrc, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, uSrc.east(), Direction.EAST);
        setPipe(level, pipeBlock, uSrc.east(2), Direction.EAST);
        setPipe(level, pipeBlock, uSrc.east(2).south(), Direction.SOUTH);
        setPipe(level, pipeBlock, uSrc.east(2).south(2), Direction.SOUTH);
        setPipe(level, pipeBlock, uSrc.east().south(2), Direction.SOUTH);
        level.setBlockAndUpdate(uDst, Blocks.BARREL.defaultBlockState());
        ((Container) level.getBlockEntity(uSrc)).setItem(0, new ItemStack(Items.QUARTZ, 12));

        // 3. T-junction failover under backpressure (north barrel only fits 6 more coal).
        var teeSrc = base.offset(6, 0, 2);
        var teeNorthDst = teeSrc.east(3).north();
        var teeSouthDst = teeSrc.east(3).south();
        level.setBlockAndUpdate(teeSrc, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, teeSrc.east(), Direction.EAST);
        setPipe(level, pipeBlock, teeSrc.east(2), Direction.EAST);
        setPipe(level, pipeBlock, teeSrc.east(2).north(), Direction.NORTH);
        setPipe(level, pipeBlock, teeSrc.east(2).south(), Direction.SOUTH);
        level.setBlockAndUpdate(teeNorthDst, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(teeSouthDst, Blocks.BARREL.defaultBlockState());
        var teeNorthBox = (Container) level.getBlockEntity(teeNorthDst);
        teeNorthBox.setItem(0, new ItemStack(Items.COAL, 58));
        for (int slot = 1; slot < teeNorthBox.getContainerSize(); slot++) {
            teeNorthBox.setItem(slot, new ItemStack(Items.STICK, 1));
        }
        ((Container) level.getBlockEntity(teeSrc)).setItem(0, new ItemStack(Items.COAL, 16));

        // 4. Explicit-outlet decoy isolation even when explicit outlet is full.
        var decoySrc = base.offset(6, 0, 6);
        var decoyPipe2 = decoySrc.east(2);
        var decoyPos = decoyPipe2.north();
        var explicitDst = decoySrc.east(4);
        level.setBlockAndUpdate(decoySrc, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, decoySrc.east(), Direction.EAST);
        setPipe(level, pipeBlock, decoyPipe2, Direction.EAST);
        setPipe(level, pipeBlock, decoySrc.east(3), Direction.EAST);
        level.setBlockAndUpdate(decoyPos, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(explicitDst, Blocks.BARREL.defaultBlockState());
        var explicitBox = (Container) level.getBlockEntity(explicitDst);
        for (int slot = 0; slot < explicitBox.getContainerSize(); slot++) {
            explicitBox.setItem(slot, new ItemStack(Items.STICK, 1));
        }
        ((Container) level.getBlockEntity(decoySrc)).setItem(0, new ItemStack(Items.REDSTONE, 8));

        int[] waitTicks = {0};
        boolean[] openedExplicit = {false};
        helper.onEachTick(
                () -> {
                    var decoyBox = (Container) level.getBlockEntity(decoyPos);
                    if (!decoyBox.isEmpty()) {
                        helper.fail("Decoy barrel leaked items while explicit outlet existed");
                    }
                    if (!openedExplicit[0]) {
                        waitTicks[0]++;
                        if (waitTicks[0] >= 25
                                && ((Container) level.getBlockEntity(decoySrc))
                                                .countItem(Items.REDSTONE)
                                        < 8) {
                            explicitBox.setItem(0, ItemStack.EMPTY);
                            openedExplicit[0] = true;
                        }
                        return;
                    }
                    var zBox = (Container) level.getBlockEntity(zDst);
                    var uBox = (Container) level.getBlockEntity(uDst);
                    var teeSouthBox = (Container) level.getBlockEntity(teeSouthDst);
                    int teeDelivered =
                            (teeNorthBox.countItem(Items.COAL) - 58)
                                    + teeSouthBox.countItem(Items.COAL);
                    if (zBox.countItem(Items.GOLD_INGOT) == 12
                            && ((Container) level.getBlockEntity(zSrc)).isEmpty()
                            && uBox.countItem(Items.QUARTZ) == 12
                            && ((Container) level.getBlockEntity(uSrc)).isEmpty()
                            && teeDelivered == 16
                            && teeNorthBox.countItem(Items.COAL) <= 64
                            && teeSouthBox.countItem(Items.COAL) >= 10
                            && ((Container) level.getBlockEntity(teeSrc)).isEmpty()
                            && explicitBox.countItem(Items.REDSTONE) == 8
                            && ((Container) level.getBlockEntity(decoySrc)).isEmpty()
                            && decoyBox.isEmpty()) {
                        helper.succeed();
                    }
                });
    }

    /**
     * Verifies extraction+injection vertical/corner climb with backward OUT & continuous flow,
     * pipe-to-pipe nozzle install rejection, extraction-only climb+corner, and NBT persistence of
     * external_intake across simulated cold reload.
     */
    @GameTest(template = "smoke", batch = "pipeRouting", timeoutTicks = 140)
    public static void itemPipeNozzleClimbContinuousFlowAndExternalIntakeReload(
            GameTestHelper helper) {
        var level = helper.getLevel();
        var pipeBlock = PlayableGameTests.block("bronze_item_pipe_small");
        var base = helper.absolutePos(new BlockPos(1, 80, 1));
        var player = player(helper, base);

        // 1. Extraction + injection across corner + UP climb + backward OUT + 24-item continuous
        // flow + pipe-face nozzle install rejection.
        var nozzleSrc = base;
        var nP1 = nozzleSrc.east();
        var nP2 = nozzleSrc.east(2);
        var nP3 = nP2.above();
        var nP4 = nP3.south();
        var nDst = nP4.east();
        level.setBlockAndUpdate(nozzleSrc, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, nP1, Direction.WEST);
        setPipe(level, pipeBlock, nP2, Direction.WEST);
        setPipe(level, pipeBlock, nP3, Direction.DOWN);
        setPipe(level, pipeBlock, nP4, Direction.NORTH);
        level.setBlockAndUpdate(nDst, Blocks.BARREL.defaultBlockState());
        interact(helper, player, nP1, Direction.WEST, stack("pipe_extraction_nozzle"));
        interact(helper, player, nP4, Direction.EAST, stack("pipe_injection_nozzle"));
        check(
                helper,
                level.getBlockState(nP4).getValue(BlockStateProperties.FACING) == Direction.EAST,
                "Injection nozzle realigned backward pipe OUT toward injection face");
        var rejectedPipeNozzle = stack("pipe_injection_nozzle");
        var pipeFaceResult = useOnPipeFace(helper, player, nP2, Direction.WEST, rejectedPipeNozzle);
        check(
                helper,
                !pipeFaceResult.consumesAction()
                        && player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
                "Nozzle installation onto a pipe-to-pipe face was rejected without consuming item");
        ((Container) level.getBlockEntity(nozzleSrc))
                .setItem(0, new ItemStack(Items.LAPIS_LAZULI, 24));

        // 2. Extraction nozzle ONLY (no injection nozzle) across horizontal -> UP -> corner.
        var extSrc = base.south(4);
        var eP1 = extSrc.east();
        var eP2 = extSrc.east(2);
        var eP3 = eP2.above();
        var eDst = eP3.south();
        level.setBlockAndUpdate(extSrc, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, eP1, Direction.NORTH);
        setPipe(level, pipeBlock, eP2, Direction.EAST);
        setPipe(level, pipeBlock, eP3, Direction.UP);
        level.setBlockAndUpdate(eDst, Blocks.BARREL.defaultBlockState());
        interact(helper, player, eP1, Direction.WEST, stack("pipe_extraction_nozzle"));
        ((Container) level.getBlockEntity(extSrc)).setItem(0, new ItemStack(Items.REDSTONE, 16));

        // 3. Auto-routed corner source pipe NBT persistence of external_intake across reload.
        var coldSrc = base.offset(5, 0, 2);
        var cP1 = coldSrc.east();
        var cP2 = cP1.above();
        var coldDst = cP2.east();
        level.setBlockAndUpdate(coldSrc, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, cP1, Direction.EAST);
        setPipe(level, pipeBlock, cP2, Direction.UP);
        ((Container) level.getBlockEntity(coldSrc))
                .setItem(0, new ItemStack(Items.AMETHYST_SHARD, 8));

        // Stage: 0 = wait for cP1 pull, 1 = wait 5 ticks post-reload without dest, 2 = dest placed
        int[] coldStage = {0, 0};
        helper.onEachTick(
                () -> {
                    if (coldStage[0] == 0) {
                        var held =
                                machine(helper, cP1)
                                        .getCapability(
                                                ForgeCapabilities.ITEM_HANDLER, Direction.EAST)
                                        .map(h -> h.getStackInSlot(0))
                                        .orElse(ItemStack.EMPTY);
                        if (held.getCount() == 4) {
                            CompoundTag saved = machine(helper, cP1).saveWithoutMetadata();
                            check(
                                    helper,
                                    saved.contains("external_intake")
                                            && saved.getByte("external_intake")
                                                    == (byte) Direction.WEST.ordinal(),
                                    "Saved item pipe NBT persisted external_intake=WEST");
                            var reloaded = new TransportNode(cP1, level.getBlockState(cP1));
                            reloaded.load(saved);
                            level.setBlockEntity(reloaded);
                            reloaded.setChanged();
                            coldStage[0] = 1;
                        }
                        return;
                    }
                    if (coldStage[0] == 1) {
                        coldStage[1]++;
                        if (coldStage[1] >= 5) {
                            var retained =
                                    machine(helper, cP1)
                                            .getCapability(
                                                    ForgeCapabilities.ITEM_HANDLER, Direction.EAST)
                                            .map(h -> h.getStackInSlot(0))
                                            .orElse(ItemStack.EMPTY);
                            check(
                                    helper,
                                    retained.getCount() == 4
                                            && ((Container) level.getBlockEntity(coldSrc))
                                                            .countItem(Items.AMETHYST_SHARD)
                                                    == 4
                                            && level.getBlockState(cP1)
                                                            .getValue(BlockStateProperties.FACING)
                                                    == Direction.EAST,
                                    "Reloaded corner source pipe kept cargo and OUT without"
                                            + " dumping back into source");
                            level.setBlockAndUpdate(coldDst, Blocks.BARREL.defaultBlockState());
                            coldStage[0] = 2;
                        }
                        return;
                    }
                    var nozzleBox = (Container) level.getBlockEntity(nDst);
                    var extBox = (Container) level.getBlockEntity(eDst);
                    var coldBox = (Container) level.getBlockEntity(coldDst);
                    if (nozzleBox.countItem(Items.LAPIS_LAZULI) == 24
                            && ((Container) level.getBlockEntity(nozzleSrc)).isEmpty()
                            && extBox.countItem(Items.REDSTONE) == 16
                            && ((Container) level.getBlockEntity(extSrc)).isEmpty()
                            && coldBox.countItem(Items.AMETHYST_SHARD) == 8
                            && ((Container) level.getBlockEntity(coldSrc)).isEmpty()
                            && level.getBlockState(cP1).getValue(BlockStateProperties.FACING)
                                    == Direction.EAST) {
                        helper.succeed();
                    }
                });
    }

    private static void setPipe(
            net.minecraft.server.level.ServerLevel level,
            Block pipeBlock,
            BlockPos pos,
            Direction out) {
        level.setBlockAndUpdate(
                pos, pipeBlock.defaultBlockState().setValue(BlockStateProperties.FACING, out));
    }

    private static void placePipeAgainst(
            GameTestHelper helper,
            ServerPlayer player,
            Block pipeBlock,
            BlockPos target,
            Direction clickedFace) {
        var stack = new ItemStack(pipeBlock);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setPos(
                target.getX() + 0.5 + clickedFace.getStepX() * 2.0,
                target.getY() + 0.5 + clickedFace.getStepY() * 2.0,
                target.getZ() + 0.5 + clickedFace.getStepZ() * 2.0);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(target)
                                .add(
                                        clickedFace.getStepX() * 0.5,
                                        clickedFace.getStepY() * 0.5,
                                        clickedFace.getStepZ() * 0.5),
                        clickedFace,
                        target,
                        false);
        var placed =
                ((BlockItem) stack.getItem())
                        .place(
                                new BlockPlaceContext(
                                        player, InteractionHand.MAIN_HAND, stack, hit));
        check(
                helper,
                placed.consumesAction(),
                "Failed to place pipe against " + target + " on " + clickedFace);
    }

    private static InteractionResult useOnPipeFace(
            GameTestHelper helper,
            ServerPlayer player,
            BlockPos pos,
            Direction side,
            ItemStack stack) {
        player.setPos(
                pos.getX() + 0.5 + side.getStepX() * 2.0,
                pos.getY() + 0.5 + side.getStepY() * 2.0,
                pos.getZ() + 0.5 + side.getStepZ() * 2.0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(pos)
                                .add(
                                        side.getStepX() * 0.5,
                                        side.getStepY() * 0.5,
                                        side.getStepZ() * 0.5),
                        side,
                        pos,
                        false);
        return player.gameMode.useItemOn(
                player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }

    private static void verifyWaterloggedPlacementAndToggle(
            GameTestHelper helper, ServerPlayer player, BlockPos pos, Block pipe, String label) {
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        var stack = new ItemStack(pipe);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(pos.below()).add(0, 0.5, 0),
                        Direction.UP,
                        pos.below(),
                        false);
        var placed =
                ((BlockItem) stack.getItem())
                        .place(
                                new BlockPlaceContext(
                                        player, InteractionHand.MAIN_HAND, stack, hit));
        var state = level.getBlockState(pos);
        check(
                helper,
                placed.consumesAction()
                        && state.is(pipe)
                        && state.getValue(BlockStateProperties.WATERLOGGED)
                        && level.getFluidState(pos).isSource()
                        && level.getFluidState(pos).getType() == Fluids.WATER,
                label + " placed inside water source sets WATERLOGGED=true");
        var scooped = ((SimpleWaterloggedBlock) pipe).pickupBlock(level, pos, state);
        check(
                helper,
                scooped.is(Items.WATER_BUCKET)
                        && !level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED)
                        && level.getFluidState(pos).isEmpty(),
                label + " pickupBlock unwaterlogs cleanly");
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
                (stack.is(stack("wrench").getItem())
                                ? player.gameMode.useItemOn(
                                        player,
                                        helper.getLevel(),
                                        stack,
                                        InteractionHand.MAIN_HAND,
                                        WrenchHits.pipeControl(
                                                helper.getLevel(), pos, player, side))
                                : useOn(helper, player, pos, side, stack))
                        .consumesAction(),
                "Native right-click accepted " + stack.getItem());
    }

    private static IFluidHandler fluid(GameTestHelper helper, BlockPos pos, Direction side) {
        return machine(helper, pos)
                .getCapability(ForgeCapabilities.FLUID_HANDLER, side)
                .orElseThrow(() -> new AssertionError("Missing fluid port at " + pos));
    }
}
