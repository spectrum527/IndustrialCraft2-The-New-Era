package com.industrialage.ia2.world;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

final class Mining {
    private static final int ADVANCED_RADIUS = 8;
    private static final int WIDTH = ADVANCED_RADIUS * 2 + 1;
    private static final int PLANE = WIDTH * WIDTH;

    private Mining() {}

    static boolean tick(WorldMachine machine, ServerLevel level, long time) {
        if (machine.kind == WorldMachineBlock.Kind.ADVANCED_MINER) {
            return time % 8 == 0 && advanced(machine, level);
        }
        if (time % 20 != 0) return false;
        return machine.withdrawing || level.hasNeighborSignal(machine.getBlockPos())
                ? withdraw(machine, level)
                : descend(machine, level);
    }

    private static boolean descend(WorldMachine machine, ServerLevel level) {
        if (!machine.inventory.canPlaceItem(0, machine.inventory.getItem(0))
                || !machine.inventory.has(2, "ic2:mining_pipe")
                || !machine.canPay(40)) return false;
        var origin = machine.getBlockPos();
        var head = origin.below(machine.pipeDepth + 1);
        if (head.getY() < WorldNative.minY(level)) return false;
        var block = level.getBlockState(head);
        if (!canDescendThrough(level, head, block)) return false;
        var tool = tool(machine);
        if (ore(block)) {
            if (!canHarvest(block, tool)) return false;
            var drops = WorldNative.drops(level, head, block, tool);
            if (!machine.inventory.store(drops)) return false;
        }
        int radius =
                machine.inventory.has(1, "ic2:advanced_scanner")
                        ? 3
                        : machine.inventory.has(1, "ic2:scanner") ? 2 : 0;
        if (radius > 0) {
            for (var pos :
                    BlockPos.betweenClosed(
                            head.offset(-radius, 0, -radius), head.offset(radius, 0, radius))) {
                if (pos.equals(head) || !level.hasChunkAt(pos) || !machine.canPay(80)) continue;
                var found = level.getBlockState(pos);
                if (ore(found)
                        && canDrill(level, pos, found)
                        && canHarvest(found, tool)
                        && machine.inventory.store(WorldNative.drops(level, pos, found, tool))) {
                    level.removeBlock(pos, false);
                    machine.pay(40);
                }
            }
        }
        var previous = head.above();
        var tip = WorldNative.block("ic2:mining_pipe_tip");
        var pipe = WorldNative.block("ic2:mining_pipe");
        if (level.getBlockState(previous).is(tip))
            level.setBlockAndUpdate(previous, pipeState(pipe, level, previous));
        level.setBlockAndUpdate(head, pipeState(tip, level, head));
        machine.inventory.removeItem(2, 1);
        machine.pipeDepth++;
        return machine.pay(40);
    }

    private static boolean withdraw(WorldMachine machine, ServerLevel level) {
        if (machine.pipeDepth <= 0 || !machine.canPay(20)) return false;
        var head = machine.getBlockPos().below(machine.pipeDepth);
        var pipe = WorldNative.block("ic2:mining_pipe");
        var tip = WorldNative.block("ic2:mining_pipe_tip");
        var state = level.getBlockState(head);
        if ((state.is(pipe) || state.is(tip))
                && !machine.inventory.store(List.of(new ItemStack(pipe)))) return false;
        if (state.is(pipe) || state.is(tip)) level.removeBlock(head, false);
        if (level.getBlockState(head.above()).is(pipe))
            level.setBlockAndUpdate(head.above(), pipeState(tip, level, head.above()));
        machine.pipeDepth--;
        return machine.pay(20);
    }

    private static BlockState pipeState(Block block, ServerLevel level, BlockPos pos) {
        var state = block.defaultBlockState();
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
                ? state.setValue(
                        BlockStateProperties.WATERLOGGED,
                        level.getFluidState(pos).getType() == Fluids.WATER)
                : state;
    }

    private static boolean advanced(WorldMachine machine, ServerLevel level) {
        eject(machine, level);
        if (!machine.inventory.canPlaceItem(0, machine.inventory.getItem(0))
                || !machine.canPay(128)
                || level.hasNeighborSignal(machine.getBlockPos())
                        != machine.inventory.has(9, "ic2:redstone_inverter_upgrade")) return false;
        var origin = machine.getBlockPos();
        var tool = new ItemStack(Items.NETHERITE_PICKAXE);
        for (int checked = 0; checked < 48; checked++) {
            if (origin.getY() - machine.scanDepth < WorldNative.minY(level)) return false;
            int index = machine.cursor;
            var pos =
                    origin.offset(
                            index % WIDTH - ADVANCED_RADIUS,
                            -machine.scanDepth,
                            index / WIDTH - ADVANCED_RADIUS);
            if (level.hasChunkAt(pos)) {
                var state = level.getBlockState(pos);
                if (ore(state) && canDrill(level, pos, state)) {
                    var drops = WorldNative.drops(level, pos, state, tool);
                    if (accepts(machine, state, drops)) {
                        if (!machine.inventory.store(drops)) {
                            eject(machine, level);
                            if (!machine.inventory.store(drops)) return false;
                        }
                        advanceCursor(machine);
                        level.removeBlock(pos, false);
                        eject(machine, level);
                        return machine.pay(128);
                    }
                }
            }
            advanceCursor(machine);
        }
        machine.setChanged();
        return false;
    }

    private static void advanceCursor(WorldMachine machine) {
        if (++machine.cursor >= PLANE) {
            machine.cursor = 0;
            machine.scanDepth++;
        }
    }

    private static boolean accepts(WorldMachine machine, BlockState state, List<ItemStack> drops) {
        String block = WorldNative.blockId(state.getBlock());
        boolean filterPresent = false;
        for (int slot = 1; slot <= 3; slot++) {
            var filter = machine.inventory.getItem(slot);
            if (filter.isEmpty()) continue;
            filterPresent = true;
            if (WorldNative.itemId(filter).equals(block)
                    || drops.stream().anyMatch(stack -> stack.is(filter.getItem()))) {
                return !machine.blacklist;
            }
        }
        return machine.blacklist || !filterPresent;
    }

    private static void eject(WorldMachine machine, ServerLevel level) {
        for (Direction side : Direction.values()) {
            var pos = machine.getBlockPos().relative(side);
            if (!level.hasChunkAt(pos)
                    || !com.industrialage.ia2.access.MachineAccess.allowsOutput(machine, side))
                continue;
            for (int slot = 4; slot < 8; slot++) {
                var offered = machine.inventory.getItem(slot);
                if (offered.isEmpty()) continue;
                int moved = WorldItemAccess.push(level, pos, side.getOpposite(), offered);
                if (moved > 0) machine.inventory.removeItem(slot, moved);
            }
        }
    }

    private static ItemStack tool(WorldMachine machine) {
        var installed = machine.inventory.getItem(0);
        var base =
                new ItemStack(
                        machine.inventory.has(0, "ic2:drill")
                                ? Items.IRON_PICKAXE
                                : machine.inventory.has(0, "ic2:iridium_drill")
                                        ? Items.NETHERITE_PICKAXE
                                        : Items.DIAMOND_PICKAXE);
        var enchantments = installed.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            base.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, enchantments);
        }
        return base;
    }

    private static boolean canHarvest(BlockState state, ItemStack tool) {
        return !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
    }

    private static boolean canDescendThrough(ServerLevel level, BlockPos pos, BlockState block) {
        return canDrill(level, pos, block) || block.is(Blocks.WATER) || block.is(Blocks.LAVA);
    }

    private static boolean canDrill(ServerLevel level, BlockPos pos, BlockState block) {
        return block.getDestroySpeed(level, pos) >= 0
                && level.getBlockEntity(pos) == null
                && block.getFluidState().isEmpty()
                && (block.isAir()
                        || block.is(BlockTags.BASE_STONE_OVERWORLD)
                        || block.is(BlockTags.BASE_STONE_NETHER)
                        || WorldTerrain.soil(block)
                        || block.is(Blocks.SAND)
                        || block.is(Blocks.GRAVEL)
                        || ore(block));
    }

    private static final class OreTags {
        static final Set<Block> VANILLA =
                Set.of(
                        Blocks.COAL_ORE,
                        Blocks.DEEPSLATE_COAL_ORE,
                        Blocks.REDSTONE_ORE,
                        Blocks.DEEPSLATE_REDSTONE_ORE,
                        Blocks.DIAMOND_ORE,
                        Blocks.DEEPSLATE_DIAMOND_ORE,
                        Blocks.LAPIS_ORE,
                        Blocks.DEEPSLATE_LAPIS_ORE,
                        Blocks.EMERALD_ORE,
                        Blocks.DEEPSLATE_EMERALD_ORE,
                        Blocks.NETHER_QUARTZ_ORE,
                        Blocks.ANCIENT_DEBRIS);
        static final java.util.List<net.minecraft.tags.TagKey<Block>> MODDED =
                List.of(
                        WorldNative.oreTag("c:ores"),
                        WorldNative.oreTag("forge:ores"),
                        WorldNative.oreTag("ic2:ores"));
    }

    static boolean ore(BlockState block) {
        if (OreTags.VANILLA.contains(block.getBlock())
                || block.is(BlockTags.COPPER_ORES)
                || block.is(BlockTags.IRON_ORES)
                || block.is(BlockTags.GOLD_ORES)) return true;
        for (var tag : OreTags.MODDED) {
            if (block.is(tag)) return true;
        }
        return false;
    }
}
