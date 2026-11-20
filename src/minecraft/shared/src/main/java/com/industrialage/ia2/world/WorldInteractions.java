package com.industrialage.ia2.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class WorldInteractions {
    private WorldInteractions() {}

    static boolean block(
            Level level, Player player, InteractionHand hand, BlockPos pos, Direction face) {
        var stack = player.getItemInHand(hand);
        var state = level.getBlockState(pos);
        String item = WorldNative.itemId(stack);
        String path = WorldNative.blockId(state.getBlock());
        if (!level.mayInteract(player, pos) || !player.mayBuild()) return false;
        boolean server = level instanceof ServerLevel;
        if (MachineAutomation.selectSide(level, player, stack, face)) return true;

        if (stack.getItem() instanceof FoamEquip sprayer) {
            if (sprayer.refillBucket(player, hand, stack, server)) return true;
            if (player.isShiftKeyDown()
                    && sprayer.room(stack) > 0
                    && WorldFluidAccess.refillFoam(level, pos, face, stack, sprayer)) return true;
            if (sprayer.spray(level, pos, face, player, hand, stack)) return true;
        }
        if (state.getBlock() instanceof WorldBlocks.WetFoam foam && stack.is(Items.SAND)) {
            if (level instanceof ServerLevel world
                    && foam.harden(world, pos, state)
                    && !player.isCreative()) stack.shrink(1);
            return true;
        }
        if (state.hasProperty(FoamCasing.HARDENED)
                && !state.getValue(FoamCasing.HARDENED)
                && stack.is(Items.SAND)) {
            if (server
                    && level.setBlockAndUpdate(pos, state.setValue(FoamCasing.HARDENED, true))
                    && !player.isCreative()) stack.shrink(1);
            return true;
        }
        if (item.equals("ic2:water_sheet") || item.equals("ic2:lava_sheet"))
            return ObscuredWall.applySheet(
                    level, pos, player, stack, state, item.equals("ic2:lava_sheet"));
        if (item.equals("ic2:obscurator"))
            return ObscuredWall.use(level, pos, player, stack, state);
        if (item.startsWith("ic2:") && item.endsWith("_painter")) {
            String dye = item.substring("ic2:".length(), item.length() - "_painter".length());
            int color = CableColors.dye(dye);
            if (color < 0) return false;
            boolean painted = WorldNative.paintCable(level, pos, state, color);
            if (!painted
                    && (state.getBlock() instanceof WorldBlocks.PaintedWall
                            || state.getBlock() instanceof ObscuredWall)) {
                var wall = WorldNative.block("ic2:" + dye + "_wall");
                painted =
                        !state.is(wall)
                                && (!server
                                        || level.setBlockAndUpdate(pos, wall.defaultBlockState()));
            }
            if (painted && server && !player.isCreative()) {
                WorldNative.damage(stack, 1, player, hand);
                if (stack.isEmpty())
                    player.setItemInHand(hand, new ItemStack(WorldNative.item("ic2:painter")));
            }
            return painted;
        }
        if (state.getBlock() instanceof WorldBlocks.Scaffold && item.equals("ic2:iron_fence")) {
            if (path.startsWith("ic2:reinforced_")) return false;
            var reinforced = WorldNative.block("ic2:reinforced_" + path.substring(4));
            if (server
                    && level.setBlockAndUpdate(
                            pos, WorldNative.copySharedState(state, reinforced.defaultBlockState()))
                    && !player.isCreative()) stack.shrink(1);
            return true;
        }
        if (state.getBlock() instanceof CropBlock crop) {
            return useCrop(level, player, hand, pos, item, crop, stack);
        }
        if (hand == InteractionHand.MAIN_HAND && state.getBlock() instanceof WorldMachineBlock) {
            if (server && level.getBlockEntity(pos) instanceof WorldMachine machine) {
                if (item.endsWith("wrench") && player.isShiftKeyDown()) {
                    if (((ServerLevel) level).destroyBlock(pos, false, player)
                            && !player.isCreative()) {
                        Block.popResource(level, pos, new ItemStack(state.getBlock()));
                    }
                } else if (item.endsWith("wrench")) {
                    var property = BlockStateProperties.HORIZONTAL_FACING;
                    Direction next =
                            face.getAxis().isHorizontal()
                                    ? face
                                    : state.getValue(property).getClockWise();
                    level.setBlockAndUpdate(pos, state.setValue(property, next));
                } else {
                    machine.interact(player, face);
                }
            }
            return true;
        }
        return false;
    }

    private static boolean useCrop(
            Level level,
            Player player,
            InteractionHand hand,
            BlockPos pos,
            String item,
            CropBlock crop,
            ItemStack stack) {
        if (crop.species == null && item.equals("ic2:crop_stick")) {
            var state = level.getBlockState(pos);
            if (state.getValue(CropBlock.CROSSED)) return false;
            if (level instanceof ServerLevel
                    && level.setBlockAndUpdate(pos, state.setValue(CropBlock.CROSSED, true))
                    && !player.isCreative()) stack.shrink(1);
            return true;
        }
        var seed = CropSeed.read(stack);
        var species = seed == null ? CropSpecies.bySeed(stack) : seed.crop();
        if (crop.species == null && species != null) {
            if (level instanceof ServerLevel world
                    && level.getBlockEntity(pos) instanceof CropPlot plot) {
                plot.plant(world, species, seed == null ? CropGenes.WILD : seed.genes());
                if (!player.isCreative()) stack.shrink(1);
            }
            return true;
        }
        boolean nutrient = item.equals("ic2:fertilizer");
        boolean irrigation =
                item.equals("ic2:hydration_cell") || item.equals("minecraft:water_bucket");
        boolean protection = item.equals("ic2:weed_ex_cell") || item.equals("ic2:weed_ex_bucket");
        boolean analyze = item.equals("ic2:cropnalyzer");
        boolean emptyHand = hand == InteractionHand.MAIN_HAND && stack.isEmpty();
        if (!nutrient && !irrigation && !protection && !analyze && !emptyHand) return false;
        if (!(level instanceof ServerLevel world)
                || !(level.getBlockEntity(pos) instanceof CropPlot plot)) return true;
        if (nutrient && plot.nutrients <= 100) {
            plot.nutrients += 100;
            consume(player, hand, stack, ItemStack.EMPTY);
        } else if (irrigation && plot.hydration < 200) {
            plot.hydration = 200;
            var field = level.getBlockState(pos.below());
            if (field.is(Blocks.FARMLAND))
                level.setBlockAndUpdate(
                        pos.below(), field.setValue(BlockStateProperties.MOISTURE, 7));
            consume(
                    player,
                    hand,
                    stack,
                    item.endsWith("_bucket") ? new ItemStack(Items.BUCKET) : cell());
        } else if (protection && (crop.species == CropSpecies.WEED || plot.protection == 0)) {
            plot.clearWeed(world);
            if (level.getBlockEntity(pos) instanceof CropPlot again) {
                again.protection = 100;
                again.setChanged();
            }
            consume(
                    player,
                    hand,
                    stack,
                    item.endsWith("_bucket") ? new ItemStack(Items.BUCKET) : cell());
        } else if (analyze && crop.species != null) {
            scan(player, stack, crop.species, plot.genes, false);
        } else if (emptyHand && plot.ripe() && crop.species != CropSpecies.WEED) {
            Block.popResource(level, pos, plot.harvestOutput());
            plot.harvest(world);
        } else if (emptyHand && crop.species == CropSpecies.WEED) {
            plot.clearWeed(world);
        }
        plot.setChanged();
        return true;
    }

    static boolean item(Level level, Player player, InteractionHand hand) {
        var tool = player.getItemInHand(hand);
        var other =
                player.getItemInHand(
                        hand == InteractionHand.MAIN_HAND
                                ? InteractionHand.OFF_HAND
                                : InteractionHand.MAIN_HAND);
        if (MachineAutomation.filter(level, player, tool, other)) return true;
        boolean server = level instanceof ServerLevel;
        if (tool.getItem() instanceof FoamEquip sprayer) {
            if (sprayer.refillBucket(player, hand, tool, server)) return true;
            if (sprayer.pack()
                    && player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                    && !player.isShiftKeyDown()) {
                if (server) {
                    player.setItemSlot(EquipmentSlot.CHEST, tool.copy());
                    if (!player.isCreative()) tool.shrink(1);
                }
                return true;
            }
        }
        String item = WorldNative.itemId(tool);
        if (item.equals("ic2:cropnalyzer")) {
            var opposite =
                    hand == InteractionHand.MAIN_HAND
                            ? InteractionHand.OFF_HAND
                            : InteractionHand.MAIN_HAND;
            var held = player.getItemInHand(opposite);
            var bag = CropSeed.read(held);
            if (bag == null) return false;
            if (server) {
                var results = scan(player, tool, bag.crop(), bag.genes(), true);
                if (results != bag.genes()) {
                    var single = held.split(1);
                    CropSeed.write(single, bag.crop(), results);
                    if (held.isEmpty()) player.setItemInHand(opposite, single);
                    else if (!player.getInventory().add(single)) player.drop(single, false);
                }
            }
            return true;
        }
        if (item.equals("ic2:scanner") || item.equals("ic2:advanced_scanner")) {
            if (level instanceof ServerLevel world)
                scanOre(world, player, tool, item.equals("ic2:advanced_scanner"));
            return true;
        }
        return false;
    }

    static net.minecraft.world.level.block.state.BlockState stripped(
            net.minecraft.world.level.block.state.BlockState old) {
        String candidate =
                switch (WorldNative.blockId(old.getBlock())) {
                    case "ic2:rubber_log" -> "ic2:stripped_rubber_log";
                    case "ic2:rubber_wood" -> "ic2:stripped_rubber_wood";
                    default -> null;
                };
        if (candidate == null) return null;
        return WorldNative.block(candidate)
                .defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, old.getValue(RotatedPillarBlock.AXIS));
    }

    private static void scanOre(
            ServerLevel world, Player player, ItemStack tool, boolean advanced) {
        if (!player.isCreative() && !WorldNative.spend(tool, advanced ? 250 : 50)) {
            WorldNative.reply(player, Component.translatable("ic2.world.empty_charge"));
            return;
        }
        int radius = advanced ? 6 : 3;
        var origin = player.blockPosition();
        int ores = 0;
        for (var pos :
                BlockPos.betweenClosed(
                        origin.offset(-radius, -radius, -radius),
                        origin.offset(radius, radius, radius))) {
            if (world.hasChunkAt(pos) && Mining.ore(world.getBlockState(pos))) ores++;
        }
        WorldNative.reply(player, Component.translatable("ic2.world.ore_count", ores));
    }

    private static CropGenes scan(
            Player player, ItemStack tool, CropSpecies crop, CropGenes genes, boolean advance) {
        int step = Math.min(4, genes.scan() + 1);
        if (!player.isCreative() && !WorldNative.spend(tool, 60 + step * 30)) {
            WorldNative.reply(player, Component.translatable("ic2.world.empty_charge"));
            return genes;
        }
        var name = Component.translatable("block.ic2." + crop.path());
        if (advance) {
            genes = genes.scanMore();
            WorldNative.reply(
                    player, Component.translatable("ic2.world.scan_stage", name, genes.scan()));
        }
        if (!advance || genes.scan() >= 4) {
            WorldNative.reply(
                    player,
                    Component.translatable(
                            "ic2.world.genes",
                            name,
                            genes.growth(),
                            genes.gain(),
                            genes.resistance()));
        }
        return genes;
    }

    private static void consume(
            Player player, InteractionHand hand, ItemStack stack, ItemStack returnItem) {
        if (player.isCreative()) return;
        stack.shrink(1);
        if (returnItem.isEmpty()) return;
        if (stack.isEmpty()) player.setItemInHand(hand, returnItem);
        else if (!player.getInventory().add(returnItem)) player.drop(returnItem, false);
    }

    private static ItemStack cell() {
        return new ItemStack(WorldNative.item("ic2:empty_cell"));
    }
}
