package com.industrialage.ia2.worldtest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Independent native input, food, coloring, and power checks for an installed release. */
public final class FoodPaintChecks {
    private static final BlockPos ISOLATED = new BlockPos(66, 95, 66);
    private static final BlockPos MATCHING = ISOLATED.south(10);
    private static final BlockPos FOAMED = MATCHING.south(10);
    private static final BlockPos PALETTE = FOAMED.east(6);
    private static final List<String> PAINTS =
            List.of(
                    "white",
                    "orange",
                    "magenta",
                    "light_blue",
                    "yellow",
                    "lime",
                    "pink",
                    "gray",
                    "light_gray",
                    "cyan",
                    "purple",
                    "blue",
                    "brown",
                    "green",
                    "red",
                    "black");
    private final Function<String, Block> blocks;
    private final Function<String, Item> items;
    private final BiConsumer<String, Boolean> check;
    private boolean rejoined;

    /** Uses real native registries and game-mode input on both loaders. */
    public FoodPaintChecks(
            Function<String, Block> blocks,
            Function<String, Item> items,
            BiConsumer<String, Boolean> check) {
        this.blocks = blocks;
        this.items = items;
        this.check = check;
    }

    /** Stores sorted actual registry entries and the item form of each loaded IC2 block. */
    public static void registry(Iterable<String> blocks, Iterable<String> items)
            throws IOException {
        var rows = new java.util.TreeSet<String>();
        blocks.forEach(value -> rows.add("block\t" + value));
        items.forEach(value -> rows.add("item\t" + value));
        Files.write(Path.of("registry.tsv"), rows);
    }

    /** Starts the native drink actions and places three separately powered wire circuits. */
    public void setup(ServerLevel level, ServerPlayer player) {
        level.setChunkForced(4, 4, true);
        level.setChunkForced(4, 5, true);
        level.getChunk(4, 4);
        level.getChunk(4, 5);
        player.setGameMode(GameType.SURVIVAL);
        food(level, player);
        iridiumChests(level, player);
        level.setBlockAndUpdate(PALETTE, blocks.apply("glass_fibre_cable").defaultBlockState());
        for (int index = 0; index < PAINTS.size(); index++) {
            var paint = stack(PAINTS.get(index) + "_painter");
            click(level, player, PALETTE, paint);
            require(
                    "painter-" + PAINTS.get(index),
                    paint.getDamageValue() == 1
                            && color(level, PALETTE) == (index == 15 ? 0 : index + 1));
        }
        var expired = stack("red_painter");
        expired.setDamageValue(expired.getMaxDamage() - 1);
        click(level, player, PALETTE, expired);
        require("painter-returns-empty-brush", player.getMainHandItem().is(items.apply("painter")));
        var same = stack("red_painter");
        click(level, player, PALETTE, same);
        require("same-dye-preserves-brush", same.getDamageValue() == 0);
        var bare = PALETTE.above(2);
        level.setBlockAndUpdate(bare, blocks.apply("copper_cable").defaultBlockState());
        click(level, player, bare, same);
        require("bare-copper-refuses-dye", color(level, bare) == -1 && same.getDamageValue() == 0);

        line(level, player, ISOLATED, "blue");
        line(level, player, MATCHING, "red");
        line(level, player, FOAMED, "blue");
        require(
                "unequal-colors-hide-both-arms",
                !arm(level, ISOLATED, BlockStateProperties.EAST)
                        && !arm(level, ISOLATED.east(), BlockStateProperties.WEST));
        require(
                "matching-colors-show-both-arms",
                arm(level, MATCHING, BlockStateProperties.EAST)
                        && arm(level, MATCHING.east(), BlockStateProperties.WEST));
        var sprayer = stack("foam_sprayer");
        player.setItemInHand(InteractionHand.MAIN_HAND, sprayer);
        player.setItemInHand(InteractionHand.OFF_HAND, stack("construction_foam_bucket"));
        player.gameMode.useItem(player, level, sprayer, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        click(level, player, FOAMED.east(), sprayer);
        player.setShiftKeyDown(false);
        require(
                "foam-preserves-wire-dye",
                level.getBlockState(FOAMED.east()).is(blocks.apply("insulated_copper_foam_cable"))
                        && color(level, FOAMED.east()) == 12);
    }

    /** Observes real harvest output after the EU routes have settled. */
    public boolean tick(ServerLevel level, ServerPlayer player, int ticks) {
        if (!rejoined && ticks > 90 && harvested(level, player, MATCHING)) {
            require("same-color-wire-supplies-eu", true);
            require("mixed-colors-isolate-eu", !harvested(level, player, ISOLATED));
            require("foam-keeps-mixed-colors-isolated", !harvested(level, player, FOAMED));
            click(level, player, ISOLATED.east(), stack("black_painter"));
            click(level, player, FOAMED, stack("blue_painter"));
            require(
                    "black-restores-both-arms",
                    arm(level, ISOLATED, BlockStateProperties.EAST)
                            && arm(level, ISOLATED.east(), BlockStateProperties.WEST));
            rejoined = true;
        }
        if (!rejoined || !harvested(level, player, ISOLATED) || !harvested(level, player, FOAMED))
            return false;
        require("black-carries-eu-to-painted-wire", true);
        require("recolored-neighbor-carries-eu-through-foam", true);
        return true;
    }

    /** Reads block state written to disk by the first server. */
    public void cold(ServerLevel level) {
        level.getChunk(4, 4);
        level.getChunk(4, 5);
        require(
                "dye-persists-through-restart",
                color(level, MATCHING) == 15
                        && color(level, MATCHING.east()) == 15
                        && color(level, ISOLATED.east()) == 0
                        && color(level, FOAMED.east()) == 12);
    }

    private void line(ServerLevel level, ServerPlayer player, BlockPos first, String next) {
        level.setBlockAndUpdate(first.west(), blocks.apply("solar_generator").defaultBlockState());
        level.setBlockAndUpdate(first, blocks.apply("insulated_copper_cable").defaultBlockState());
        level.setBlockAndUpdate(
                first.east(), blocks.apply("insulated_copper_cable").defaultBlockState());
        level.setBlockAndUpdate(first.east(2), blocks.apply("crop_harvester").defaultBlockState());
        var field = first.east(3);
        level.setBlockAndUpdate(
                field.below(),
                Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
        level.setBlockAndUpdate(field.below().south(), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(
                field,
                blocks.apply("wheat_crop")
                        .defaultBlockState()
                        .setValue(BlockStateProperties.AGE_7, 7));
        click(level, player, first, stack("red_painter"));
        click(level, player, first.east(), stack(next + "_painter"));
    }

    private boolean harvested(ServerLevel level, ServerPlayer player, BlockPos first) {
        var at = level.getBlockEntity(first.east(2));
        return at instanceof MenuProvider machine
                && machine.createMenu(9, player.getInventory(), player)
                        .slots
                        .get(0)
                        .getItem()
                        .is(Items.WHEAT);
    }

    private void food(ServerLevel level, ServerPlayer player) {
        for (String coffee : List.of("cold_coffee_mug", "dark_coffee_mug", "coffee_mug")) {
            player.removeAllEffects();
            player.getFoodData().setFoodLevel(20);
            var initial = drink(level, player, coffee);
            var speed = effect(player, "speed");
            var haste = effect(player, "haste");
            require(
                    coffee + "-first-use-returns-mug",
                    initial.is(items.apply("empty_mug"))
                            && player.getFoodData().getFoodLevel() == 20
                            && speed != null
                            && speed.getDuration() == 300
                            && speed.getAmplifier() == 0
                            && haste != null
                            && haste.getDuration() == 300
                            && haste.getAmplifier() == 0);
            for (int cup = 2; cup <= 8; cup++) {
                require(
                        coffee + "-return-cup-" + cup,
                        drink(level, player, coffee).is(items.apply("empty_mug")));
            }
            boolean cold = coffee.startsWith("cold");
            boolean dark = coffee.startsWith("dark");
            speed = effect(player, "speed");
            haste = effect(player, "haste");
            require(
                    coffee + "-observed-stacking-and-cap",
                    speed != null
                            && haste != null
                            && speed.getAmplifier() == (cold ? 1 : dark ? 5 : 6)
                            && haste.getAmplifier() == speed.getAmplifier()
                            && speed.getDuration() == (cold ? 4500 : 8700)
                            && haste.getDuration() == speed.getDuration());
            var nausea = effect(player, "nausea");
            var harm = effect(player, "instant_damage");
            require(
                    coffee + "-observed-overdose",
                    cold
                            ? nausea == null && harm == null
                            : nausea != null
                                    && harm != null
                                    && nausea.getDuration() == (dark ? 600 : 400)
                                    && harm.getAmplifier() == (dark ? 2 : 1));
        }
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
        var wart = drink(level, player, "terra_wart");
        require(
                "terra-wart-clears-only-harmful-effects",
                wart.isEmpty()
                        && !player.hasEffect(MobEffects.POISON)
                        && effect(player, "speed") != null
                        && effect(player, "haste") != null
                        && effect(player, "nausea") == null
                        && player.getFoodData().getFoodLevel() == 20);
        player.removeAllEffects();
        for (String ingredient : List.of("milk_wart", "oil_berry", "bobs_yer_uncle_ranks_berry")) {
            var held = stack(ingredient);
            player.getFoodData().setFoodLevel(17);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            player.gameMode.useItem(player, level, held, InteractionHand.MAIN_HAND);
            require(ingredient + "-is-ingredient", !player.isUsingItem() && held.getCount() == 1);
        }
        var can = new ItemStack(items.apply("filled_tin_can"), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, can);
        player.gameMode.useItem(player, level, can, InteractionHand.MAIN_HAND);
        require(
                "canned-food-eaten-instantly",
                !player.isUsingItem()
                        && player.getFoodData().getFoodLevel() == 18
                        && can.getCount() == 1);
        player.gameMode.useItem(player, level, can, InteractionHand.MAIN_HAND);
        require(
                "canned-food-last-serving-clears-stack",
                player.getFoodData().getFoodLevel() == 19 && player.getMainHandItem().isEmpty());
    }

    private void iridiumChests(ServerLevel level, ServerPlayer player) {
        List<String> oreChests =
                List.of(
                        "simple_dungeon",
                        "stronghold_corridor",
                        "stronghold_crossing",
                        "stronghold_library");
        var chest = PALETTE.above(3);
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        for (String source :
                List.of(
                        "simple_dungeon",
                        "abandoned_mineshaft",
                        "desert_pyramid",
                        "jungle_temple",
                        "stronghold_corridor",
                        "stronghold_crossing",
                        "stronghold_library",
                        "end_city_treasure",
                        "shipwreck_treasure")) {
            int shards = 0;
            int ores = 0;
            int vanilla = 0;
            var command = level.getServer().createCommandSourceStack().withSuppressedOutput();
            for (int sample = 1; sample <= 384; sample++) {
                String load =
                        String.format(
                                java.util.Locale.ROOT,
                                "data merge block %d %d %d {LootTable:\"minecraft:chests/%s\",LootTableSeed:%dL}",
                                chest.getX(),
                                chest.getY(),
                                chest.getZ(),
                                source,
                                sample * 371923L);
                level.getServer().getCommands().performPrefixedCommand(command, load);
                var menu =
                        ((MenuProvider) level.getBlockEntity(chest))
                                .createMenu(4, player.getInventory(), player);
                for (int slot = 0; slot < 27; slot++) {
                    var stack = menu.slots.get(slot).getItem();
                    if (stack.is(items.apply("iridium_shard"))) shards += stack.getCount();
                    else if (stack.is(items.apply("iridium_ore"))) ores += stack.getCount();
                    else if (!stack.isEmpty()) vanilla++;
                }
            }
            boolean shipwreck = source.equals("shipwreck_treasure");
            require(
                    "chest-loot-" + source,
                    (shipwreck ? shards == 0 : shards > 0)
                            && (oreChests.contains(source) ? ores > 0 : ores == 0)
                            && vanilla >= 384);
        }
    }

    private ItemStack drink(ServerLevel level, ServerPlayer player, String item) {
        var stack = stack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItem(player, level, stack, InteractionHand.MAIN_HAND);
        if (!player.isUsingItem()) throw new AssertionError("Native use did not start for " + item);
        var remainder = stack.finishUsingItem(level, player);
        player.stopUsingItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, remainder);
        return player.getMainHandItem();
    }

    private static MobEffectInstance effect(ServerPlayer player, String name) {
        return player.getActiveEffects().stream()
                .filter(effect -> effect.getDescriptionId().equals("effect.minecraft." + name))
                .findFirst()
                .orElse(null);
    }

    private static int color(ServerLevel level, BlockPos at) {
        var state = level.getBlockState(at);
        var color = state.getBlock().getStateDefinition().getProperty("color");
        return color == null ? -1 : Integer.parseInt(state.getValue(color).toString());
    }

    private static boolean arm(
            ServerLevel level,
            BlockPos at,
            net.minecraft.world.level.block.state.properties.BooleanProperty arm) {
        return level.getBlockState(at).getValue(arm);
    }

    private static void click(
            ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack held) {
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        player.gameMode.useItemOn(player, level, held, InteractionHand.MAIN_HAND, hit);
    }

    private ItemStack stack(String path) {
        return new ItemStack(items.apply(path));
    }

    private void require(String name, boolean passed) {
        check.accept(name, passed);
    }
}
