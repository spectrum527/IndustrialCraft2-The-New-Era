package com.industrialage.ia2.industrial;

import com.industrialage.ia2.industrial.WorkshopPlans.Liquid;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;

/** Server-side transport, electrolysis and heat-to-power machine cycles. */
final class WorkshopDevices {
    private static final String WATER = "minecraft:water";
    private static final String HYDROGEN = "ic2:hydrogen";
    private static final String OXYGEN = "ic2:oxygen";
    private static final int ELECTROLYSIS_WATER = 40;
    private static final int ELECTROLYSIS_HYDROGEN = 26;
    private static final int ELECTROLYSIS_OXYGEN = 13;

    private WorkshopDevices() {}

    static boolean tick(WorkshopNode node, ServerLevel level) {
        var facing = WorkshopBlock.direction(node.getBlockState());
        boolean active;
        switch (node.kind) {
            case FLUID_REGULATOR -> active = regulate(node, level, facing);
            case FLUID_DISTRIBUTOR -> active = distribute(node, level, facing);
            case WEIGHTED_DISTRIBUTOR -> active = weighted(node, level, facing);
            case PUMP -> active = pump(node, level, facing);
            case ELECTROLYZER -> active = electrolyze(node, level);
            case STIRLING -> active = stirling(node, level, facing);
            default -> {
                return false;
            }
        }
        node.setActive(level, active);
        return true;
    }

    private static boolean regulate(WorkshopNode node, ServerLevel level, Direction facing) {
        if (node.energy < node.kind.workingEu
                || node.inlet.amount == 0
                || node.mode == 0 && level.getGameTime() % node.kind.duration != 0) {
            return false;
        }
        int maximum = node.mode == 0 ? node.inlet.capacity : 50;
        if (push(node, node.inlet, level, facing, maximum) == 0) {
            return false;
        }
        node.energy -= node.kind.workingEu;
        node.setChanged();
        return true;
    }

    private static boolean distribute(WorkshopNode node, ServerLevel level, Direction facing) {
        if (node.inlet.amount == 0) {
            return false;
        }
        if (node.mode == 1) {
            return push(node, node.inlet, level, facing, node.inlet.capacity) > 0;
        }
        long tick = level.getGameTime();
        var faces = Direction.values();
        boolean moved = false;
        for (int offset = 0; offset < faces.length && node.inlet.amount > 0; offset++) {
            var side = faces[Math.floorMod(tick + offset, faces.length)];
            if (side != facing) {
                moved |= push(node, node.inlet, level, side, node.inlet.capacity) > 0;
            }
        }
        return moved;
    }

    static int changePriority(WorkshopNode node, Direction output) {
        int shift = output.ordinal() * 3;
        int priority = (priority(node, output) + 1) % 8;
        node.cycles = node.cycles & ~(7 << shift) | priority << shift;
        node.setChanged();
        return priority;
    }

    private static int priority(WorkshopNode node, Direction output) {
        return node.cycles >>> (output.ordinal() * 3) & 7;
    }

    private static boolean weighted(WorkshopNode node, ServerLevel level, Direction intake) {
        boolean moved = false;
        for (int priority = 7; priority >= 0 && node.inlet.amount > 0; priority--) {
            for (var output : Direction.values()) {
                if (output != intake && priority(node, output) == priority) {
                    moved |= push(node, node.inlet, level, output, node.inlet.capacity) > 0;
                }
            }
        }
        return moved;
    }

    private static boolean pump(WorkshopNode node, ServerLevel level, Direction facing) {
        for (var side : Direction.values()) {
            if (side != facing && node.outlet.amount > 0) {
                push(node, node.outlet, level, side, WorkshopCells.VOLUME);
            }
        }
        if (node.energy < node.kind.workingEu || node.inlet.amount >= node.inlet.capacity) {
            return false;
        }
        var input = node.getBlockPos().relative(facing);
        if (!level.hasChunkAt(input)) {
            return false;
        }
        var state = level.getBlockState(input);
        var source = state.getFluidState();
        String expected = node.inlet.id;
        int space = Math.min(WorkshopCells.VOLUME, node.inlet.capacity - node.inlet.amount);
        var available =
                WorkshopPlatform.pullFluid(
                        level, input, facing.getOpposite(), expected, space, true);
        String worldFluid =
                source.isSource() && state.getBlock() instanceof LiquidBlock
                        ? WorkshopPlatform.worldFluidId(source.getType())
                        : "";
        boolean fromWorld =
                available.amount() == 0
                        && space == WorkshopCells.VOLUME
                        && !worldFluid.isEmpty()
                        && (expected.isEmpty() || expected.equals(worldFluid));
        if (available.amount() == 0 && !fromWorld) {
            return false;
        }
        if (node.progress + 1 == node.kind.duration) {
            Liquid extracted;
            if (fromWorld && level.setBlockAndUpdate(input, Blocks.AIR.defaultBlockState())) {
                extracted = new Liquid(worldFluid, WorkshopCells.VOLUME);
            } else {
                extracted =
                        WorkshopPlatform.pullFluid(
                                level, input, facing.getOpposite(), expected, space, false);
            }
            if (extracted.amount() == 0) {
                return false;
            }
            node.inlet.fill(extracted.id(), extracted.amount(), false);
            node.progress = 0;
        } else {
            node.progress++;
        }
        node.energy -= node.kind.workingEu;
        node.setChanged();
        return true;
    }

    private static boolean electrolyze(WorkshopNode node, ServerLevel level) {
        push(node, node.outlet, level, Direction.DOWN, node.outlet.capacity);
        push(node, node.byproduct, level, Direction.UP, node.byproduct.capacity);
        if (node.energy < node.kind.workingEu
                || !node.inlet.has(WATER, ELECTROLYSIS_WATER)
                || node.outlet.fill(HYDROGEN, ELECTROLYSIS_HYDROGEN, true) != ELECTROLYSIS_HYDROGEN
                || node.byproduct.fill(OXYGEN, ELECTROLYSIS_OXYGEN, true) != ELECTROLYSIS_OXYGEN) {
            return false;
        }
        node.energy -= node.kind.workingEu;
        if (++node.progress >= node.kind.duration) {
            node.inlet.drain(ELECTROLYSIS_WATER, false);
            node.outlet.fill(HYDROGEN, ELECTROLYSIS_HYDROGEN, false);
            node.byproduct.fill(OXYGEN, ELECTROLYSIS_OXYGEN, false);
            node.progress = 0;
        }
        node.setChanged();
        return true;
    }

    private static boolean stirling(WorkshopNode node, ServerLevel level, Direction facing) {
        int demand = Math.min(100, (int) ((node.kind.capacity - node.energy) * 2));
        int obtained = neighboringHeat(node, level, facing, demand);
        if (obtained > 0) {
            node.energy += obtained * 0.5;
            node.setChanged();
            return true;
        }
        return false;
    }

    static int neighboringHeat(WorkshopNode node, ServerLevel level, Direction facing, int demand) {
        var neighbor = node.getBlockPos().relative(facing);
        if (demand <= 0
                || !level.hasChunkAt(neighbor)
                || !(level.getBlockEntity(neighbor) instanceof WorkshopNode source)) {
            return 0;
        }
        return source.pullHeat(level, facing.getOpposite(), demand);
    }

    static int push(
            WorkshopNode node,
            WorkshopNode.Tank tank,
            ServerLevel level,
            Direction direction,
            int maximum) {
        if (tank.amount == 0) {
            return 0;
        }
        var pos = node.getBlockPos().relative(direction);
        int accepted =
                WorkshopPlatform.pushFluid(
                        level,
                        pos,
                        direction.getOpposite(),
                        tank.id,
                        Math.min(maximum, tank.amount));
        if (accepted > 0) {
            tank.drain(accepted, false);
            node.setChanged();
        }
        return accepted;
    }
}
