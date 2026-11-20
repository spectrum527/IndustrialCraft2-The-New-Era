package com.industrialage.ia2.forge.qa;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Independently applies native workbench, block placement, storage, and random-tick interactions.
 */
@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class PlayableGameTests {
    private PlayableGameTests() {}

    /** Crafts a device, then places its item and inserts coal through the real menu. */
    @GameTest(template = "smoke", timeoutTicks = 20)
    public static void starterWorkbenchAndMenu(GameTestHelper helper) {
        var level = helper.getLevel();
        var bench = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(bench, Blocks.CRAFTING_TABLE.defaultBlockState());
        var player = player(helper, bench);
        var crafting =
                new CraftingMenu(
                        1, player.getInventory(), ContainerLevelAccess.create(level, bench));
        player.containerMenu = crafting;
        crafting.getSlot(1).set(new ItemStack(Items.IRON_INGOT));
        crafting.getSlot(2).set(stack("forge_hammer"));
        crafting.clicked(0, 0, ClickType.PICKUP, player);
        check(helper, crafting.getCarried().is(stack("iron_plate").getItem()), "Iron plate recipe");
        check(helper, crafting.getSlot(1).getItem().isEmpty(), "Iron ingot was consumed");
        check(helper, crafting.getSlot(2).getItem().getDamageValue() == 1, "Hammer damaged by one");
        crafting.setCarried(ItemStack.EMPTY);

        crafting.getSlot(1).set(stack("copper_plate"));
        crafting.getSlot(2).set(stack("cutter"));
        crafting.clicked(0, 0, ClickType.PICKUP, player);
        check(
                helper,
                crafting.getCarried().is(stack("copper_cable").getItem())
                        && crafting.getCarried().getCount() == 2,
                "Cable recipe output");
        check(helper, crafting.getSlot(2).getItem().getDamageValue() == 1, "Cutter damaged by one");
        crafting.setCarried(ItemStack.EMPTY);

        crafting.getSlot(2).set(stack("re_battery"));
        for (int index = 4; index <= 6; index++) {
            crafting.getSlot(index).set(stack("iron_plate"));
        }
        crafting.getSlot(8).set(stack("iron_furnace"));
        crafting.clicked(0, 0, ClickType.PICKUP, player);
        var generator = crafting.getCarried().copy();
        check(helper, generator.is(stack("generator").getItem()), "Generator recipe output");
        crafting.setCarried(ItemStack.EMPTY);

        var pos = bench.relative(Direction.EAST);
        place(helper, player, pos, generator);
        var entity = machine(helper, pos);
        var menu = ((MenuProvider) entity).createMenu(2, player.getInventory(), player);
        player.containerMenu = menu;
        check(helper, menu.stillValid(player), "Generator menu reachable from placing player");
        menu.setCarried(new ItemStack(Items.LAVA_BUCKET));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        check(helper, menu.getSlot(0).getItem().isEmpty(), "Generator rejected lava from menu");
        menu.setCarried(new ItemStack(Items.COAL));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        check(
                helper,
                menu.getSlot(0).getItem().is(Items.COAL),
                "Generator accepted coal from menu");
        helper.runAfterDelay(
                3,
                () -> {
                    check(
                            helper,
                            entity.saveWithoutMetadata()
                                            .getCompound("generator")
                                            .getDouble("stored_energy")
                                    > 0,
                            "The generator converted menu fuel through native ticking");
                    helper.succeed();
                });
    }

    /** Observes all 600 native ticks of the B#41 continuous maceration regression. */
    @GameTest(template = "smoke", timeoutTicks = 620)
    public static void maceratorContinuousM09(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        place(helper, player(helper, pos), pos, stack("macerator"));
        var entity = machine(helper, pos);
        CompoundTag initial = entity.saveWithoutMetadata();
        initial.putDouble("ic2_energy", 1200);
        entity.load(initial);
        entity.setChanged();
        var in =
                entity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Missing production inventory capability"));
        var out =
                entity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN)
                        .orElseThrow(
                                () -> new AssertionError("Missing production output capability"));
        check(
                helper,
                in.insertItem(0, new ItemStack(Items.COAL, 2), false).isEmpty(),
                "Native insertion supplied two coal");
        check(
                helper,
                in.extractItem(0, 1, true).isEmpty(),
                "Automation may not extract valid input");
        int[] paid = {0};
        helper.onEachTick(
                () -> {
                    var saved = entity.saveWithoutMetadata();
                    int powder = out.getStackInSlot(0).getCount();
                    int next = powder * 300 + saved.getInt("ic2_progress");
                    if (next == 0 && paid[0] == 0) {
                        return;
                    }
                    check(
                            helper,
                            next == paid[0] + 1,
                            "Expected one paid tick at " + (paid[0] + 1));
                    double energy = saved.getDouble("ic2_energy");
                    check(
                            helper,
                            energy == 1200 - next * 2,
                            "Incorrect energy after " + next + " ticks");
                    check(
                            helper,
                            in.getStackInSlot(0).getCount() == 2 - powder,
                            "Input and output quantities diverged");
                    if (powder > 0) {
                        check(
                                helper,
                                out.getStackInSlot(0).is(stack("coal_dust").getItem()),
                                "Wrong macerator output");
                    }
                    paid[0] = next;
                    if (next == 600) {
                        helper.succeed();
                    }
                });
    }

    /** Native bone meal grows a rubber tree and the held tap can collect from a wet face. */
    @GameTest(template = "smoke", timeoutTicks = 20)
    public static void rubberTreeAndTap(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 4, 1));
        level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        var sapling = (SaplingBlock) block("rubber_sapling");
        var state = sapling.defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        for (int attempt = 0; attempt < 4 && level.getBlockState(pos).is(sapling); attempt++) {
            state = level.getBlockState(pos);
            sapling.performBonemeal(level, level.getRandom(), pos, state);
        }
        var log = block("rubber_log");
        check(
                helper,
                level.getBlockState(pos).is(log) && level.getBlockState(pos.above()).is(log),
                "Rubber sapling produced a rooted trunk with vanilla tree placement");
        var wet =
                property(
                        log.defaultBlockState(),
                        log.getStateDefinition().getProperty("resin_hole"),
                        "wet_north");
        level.setBlockAndUpdate(pos, wet);
        var player = player(helper, pos);
        var tap = stack("treetap");
        player.setItemInHand(InteractionHand.MAIN_HAND, tap);
        useOn(helper, player, pos, Direction.NORTH, tap);
        var dry = property(wet, log.getStateDefinition().getProperty("resin_hole"), "dry_north");
        check(
                helper,
                level.getBlockState(pos).equals(dry) && tap.getDamageValue() == 1,
                "Tap exhausted the contacted hole and wore by one");
        var resources = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        int resin =
                resources.stream()
                        .map(ItemEntity::getItem)
                        .filter(item -> item.is(stack("resin").getItem()))
                        .mapToInt(ItemStack::getCount)
                        .sum();
        check(helper, resin >= 1 && resin <= 3, "Wet hole provided " + resin + " resin");
        helper.succeed();
    }

    private static <T extends Comparable<T>> BlockState property(
            BlockState state, Property<T> property, String name) {
        return state.setValue(property, property.getValue(name).orElseThrow());
    }

    static void place(GameTestHelper helper, ServerPlayer player, BlockPos pos, ItemStack stack) {
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var result = useOn(helper, player, pos.below(), Direction.UP, stack);
        check(
                helper,
                result.consumesAction() && stack.isEmpty(),
                "Native survival block placement");
    }

    static InteractionResult useOn(
            GameTestHelper helper,
            ServerPlayer player,
            BlockPos target,
            Direction side,
            ItemStack stack) {
        var hit = new BlockHitResult(Vec3.atCenterOf(target), side, target, false);
        return player.gameMode.useItemOn(
                player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }

    static ServerPlayer player(GameTestHelper helper, BlockPos near) {
        var player =
                FakePlayerFactory.get(
                        helper.getLevel(), new GameProfile(UUID.randomUUID(), "IC2PlayTester"));
        player.setPos(near.getX() + 0.5, near.getY(), near.getZ() - 2);
        return player;
    }

    static ItemStack stack(String path) {
        return new ItemStack(
                ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("ic2", path)));
    }

    static Block block(String path) {
        return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("ic2", path));
    }

    static BlockEntity machine(GameTestHelper helper, BlockPos pos) {
        var blockEntity = helper.getLevel().getBlockEntity(pos);
        if (blockEntity == null) {
            helper.fail("No native machine block entity", pos);
        }
        return blockEntity;
    }

    static void check(GameTestHelper helper, boolean valid, String failure) {
        if (!valid) {
            helper.fail(failure);
        }
    }
}
