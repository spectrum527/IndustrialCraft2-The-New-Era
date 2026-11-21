package com.industrialage.ia2.celltest;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Exercises hand use through the game mode and observes original items and native world states. */
public final class HeldCellChecks {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

    /** A real fluid type and its volume in millibuckets. */
    public record Liquid(Fluid fluid, int amount) {}

    /** Supplies a tank through its native fluid port, with an optional simulation. */
    @FunctionalInterface
    public interface TankFill {
        int fill(BlockPos pos, Liquid liquid, boolean simulate);
    }

    /** Loader-specific registries and native fluid capability operations. */
    public record Host(
            Function<String, Item> item,
            Function<String, Block> block,
            Function<ItemStack, Liquid> cellLiquid,
            BiFunction<ItemStack, Liquid, ItemStack> seedCell,
            Function<BlockPos, Liquid> tankLiquid,
            TankFill tankFill) {}

    private final ServerLevel world;
    private final ServerPlayer player;
    private final BlockPos source;
    private final Host host;
    private final BiConsumer<String, Boolean> check;

    private HeldCellChecks(
            ServerLevel world,
            ServerPlayer player,
            BlockPos source,
            Host host,
            BiConsumer<String, Boolean> check) {
        this.world = world;
        this.player = player;
        this.source = source;
        this.host = host;
        this.check = check;
    }

    /** Runs the survival and creative matrix on the same live world and actual player inventory. */
    public static void run(
            ServerLevel world,
            ServerPlayer player,
            BlockPos source,
            Host host,
            BiConsumer<String, Boolean> check) {
        new HeldCellChecks(world, player, source, host, check).run();
    }

    private void run() {
        player.setGameMode(GameType.SURVIVAL);
        check.accept("held-cells-survival-player", !player.isCreative());
        world.setBlockAndUpdate(source.below(), Blocks.STONE.defaultBlockState());
        for (var kind : CellKind.values()) {
            collect(kind, Fluids.WATER, 1, false);
            collect(kind, Fluids.LAVA, 1, false);
            collect(kind, Fluids.WATER, 3, false);
            collect(kind, Fluids.LAVA, 3, true);
            rejectedWorld(kind.id + "-flowing-water", stack(kind.id, 2), Fluids.WATER, 1);
            rejectedWorld(kind.id + "-flowing-lava", stack(kind.id, 2), Fluids.LAVA, 1);
            pourIntoPortsOnly(kind, Fluids.WATER, "ore_washing_plant");
            pourIntoPortsOnly(kind, Fluids.LAVA, "geo_generator");
            creative(kind);
        }

        var partial = host.seedCell().apply(stack("fluid_cell", 1), new Liquid(Fluids.WATER, 257));
        rejectedWorld("partial-cell-needs-room-for-whole-world-source", partial, Fluids.WATER, 0);
        rejectedWorld("partial-cell-rejects-different-world-fluid", partial, Fluids.LAVA, 0);
        var tank = source.east();
        prepareTarget(tank, "bronze_tank");
        check.accept(
                "partial-cell-lava-target-seeded",
                host.tankFill().fill(tank, new Liquid(Fluids.LAVA, 900), false) == 900);
        rejectedTarget("partial-cell-rejects-mixed-tank-liquid", tank, partial);
        prepareTarget(tank, "bronze_tank");
        check.accept(
                "partial-cell-water-target-seeded",
                host.tankFill().fill(tank, new Liquid(Fluids.WATER, 900), false) == 900);
        useOn(tank, partial.copy());
        check.accept(
                "partial-cell-absorbs-only-remaining-capacity",
                fluidItem(held(), "fluid_cell", Fluids.WATER, 1, 1000)
                        && host.tankLiquid().apply(tank).equals(new Liquid(Fluids.WATER, 157)));
    }

    private void collect(CellKind kind, Fluid fluid, int count, boolean stuffed) {
        player.getInventory().clearContent();
        if (stuffed) {
            for (int slot = 0; slot < 36; slot++) {
                player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
        }
        var empty = stack(kind.id, count);
        var filled = kind.filled(fluid);
        placeFluid(fluid, 0);
        useInWorld(empty);
        String label =
                kind.id
                        + "-collects-"
                        + name(fluid)
                        + "-"
                        + count
                        + (stuffed ? "-full-inventory" : "");
        check.accept(label + "-source-removed", world.getFluidState(source).isEmpty());
        if (count == 1) {
            check.accept(label + "-single-hand-result", fluidItem(held(), filled, fluid, 1, 1000));
            return;
        }
        check.accept(
                label + "-one-shell-consumed",
                fluidItem(held(), kind.id, Fluids.EMPTY, count - 1, 0));
        int inInventory = countInInventory(filled, fluid, 1000);
        int dropped = 0;
        for (var drop : world.getEntitiesOfClass(ItemEntity.class, new AABB(source).inflate(4))) {
            if (fluidItem(drop.getItem(), filled, fluid, 1, 1000)) {
                dropped++;
                drop.discard();
            }
        }
        check.accept(
                label + "-exactly-one-filled-cell",
                inInventory == (stuffed ? 0 : 1) && dropped == (stuffed ? 1 : 0));
    }

    private void creative(CellKind kind) {
        player.getInventory().clearContent();
        player.setGameMode(GameType.CREATIVE);
        try {
            check.accept(kind.id + "-creative-player", player.isCreative());
            var empty = stack(kind.id, 3);
            for (int use = 0; use < 2; use++) {
                placeFluid(Fluids.WATER, 0);
                useInWorld(empty.copy());
                check.accept(
                        kind.id + "-creative-keeps-empty-and-one-result-" + use,
                        world.getFluidState(source).isEmpty()
                                && ItemStack.matches(empty, held())
                                && countInInventory(kind.filled(Fluids.WATER), Fluids.WATER, 1000)
                                        == 1);
            }
        } finally {
            player.setGameMode(GameType.SURVIVAL);
        }
    }

    private void pourIntoPortsOnly(CellKind kind, Fluid fluid, String machine) {
        var filled =
                kind == CellKind.UNIVERSAL
                        ? host.seedCell().apply(stack(kind.id, 1), new Liquid(fluid, 1000))
                        : stack(kind.filled(fluid), 2);
        var label = kind.id + "-full-" + name(fluid);
        world.setBlockAndUpdate(source, Blocks.AIR.defaultBlockState());
        useInWorld(filled.copy());
        check.accept(
                label + "-air-use-over-ground-keeps-cell-and-world",
                world.getFluidState(source).isEmpty() && ItemStack.matches(filled, held()));
        useOn(source.below(), filled.copy());
        check.accept(
                label + "-ground-right-click-keeps-cell-and-world",
                world.getBlockState(source.below()).is(Blocks.STONE)
                        && world.getFluidState(source).isEmpty()
                        && ItemStack.matches(filled, held()));
        var place = source.east();
        for (var path : new String[] {"bronze_tank", machine}) {
            prepareTarget(place, path);
            check.accept(
                    label + "-" + path + "-would-accept-piped-fluid",
                    host.tankFill().fill(place, new Liquid(fluid, 1000), true) == 1000);
            player.getInventory().clearContent();
            useOn(place, filled.copy());
            check.accept(
                    label + "-fills-" + path + "-by-exactly-one-liter",
                    host.tankLiquid().apply(place).equals(new Liquid(fluid, 1000))
                            && world.getFluidState(place.above()).isEmpty());
            int remaining = filled.getCount() - 1;
            check.accept(
                    label + "-returns-original-empty-cell-from-" + path,
                    remaining == 0
                            ? fluidItem(held(), kind.id, Fluids.EMPTY, 1, 0)
                            : fluidItem(held(), kind.filled(fluid), fluid, remaining, 1000)
                                    && countInInventory(kind.id, Fluids.EMPTY, 0) == 1);
        }
    }

    private void rejectedWorld(String label, ItemStack input, Fluid fluid, int level) {
        placeFluid(fluid, level);
        var prior = world.getBlockState(source);
        useInWorld(input.copy());
        check.accept(
                label, world.getBlockState(source) == prior && ItemStack.matches(input, held()));
    }

    private void rejectedTarget(String label, BlockPos pos, ItemStack input) {
        var state = world.getBlockState(pos);
        var prior = host.tankLiquid().apply(pos);
        useOn(pos, input.copy());
        check.accept(
                label,
                world.getBlockState(pos) == state
                        && ItemStack.matches(input, held())
                        && host.tankLiquid().apply(pos).equals(prior));
    }

    private void prepareTarget(BlockPos pos, String path) {
        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        world.setBlockAndUpdate(pos, host.block().apply(path).defaultBlockState());
    }

    private void placeFluid(Fluid fluid, int level) {
        var block = fluid == Fluids.WATER ? Blocks.WATER : Blocks.LAVA;
        world.setBlockAndUpdate(
                source, block.defaultBlockState().setValue(LiquidBlock.LEVEL, level));
        check.accept(
                "world-fluid-state-" + name(fluid) + "-" + level,
                world.getFluidState(source).isSource() == (level == 0));
    }

    private void useInWorld(ItemStack input) {
        player.setPos(source.getX() + .5, source.getY() + 1, source.getZ() + .5);
        player.setXRot(90);
        player.setYRot(0);
        player.setItemInHand(HAND, input);
        player.gameMode.useItem(player, world, input, HAND);
    }

    private void useOn(BlockPos pos, ItemStack input) {
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        player.setItemInHand(HAND, input);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false);
        player.gameMode.useItemOn(player, world, input, HAND, hit);
        player.closeContainer();
    }

    private int countInInventory(String item, Fluid fluid, int amount) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (fluidItem(stack, item, fluid, stack.getCount(), amount)) count += stack.getCount();
        }
        return count;
    }

    private boolean fluidItem(ItemStack item, String id, Fluid fluid, int count, int amount) {
        return item.is(host.item().apply(id))
                && item.getCount() == count
                && host.cellLiquid().apply(item).equals(new Liquid(fluid, amount));
    }

    private ItemStack stack(String path, int amount) {
        return new ItemStack(host.item().apply(path), amount);
    }

    private ItemStack held() {
        return player.getItemInHand(HAND);
    }

    private static String name(Fluid fluid) {
        return fluid == Fluids.WATER ? "water" : "lava";
    }

    private enum CellKind {
        UNIVERSAL("fluid_cell"),
        CLASSIC("empty_cell");

        final String id;

        CellKind(String id) {
            this.id = id;
        }

        String filled(Fluid fluid) {
            return this == UNIVERSAL ? id : HeldCellChecks.name(fluid) + "_cell";
        }
    }
}
