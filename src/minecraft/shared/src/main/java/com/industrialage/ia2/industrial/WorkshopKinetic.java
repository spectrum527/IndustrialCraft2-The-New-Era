package com.industrialage.ia2.industrial;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biomes;

final class WorkshopKinetic {
    static final Map<String, Integer> ROTOR_RATINGS =
            Map.of(
                    "ic2:wooden_rotor",
                    70,
                    "ic2:iron_rotor",
                    100,
                    "ic2:bronze_rotor",
                    100,
                    "ic2:steel_rotor",
                    120,
                    "ic2:carbon_rotor",
                    150);

    static final List<String> ROTOR_MATERIALS =
            List.of("wooden", "iron", "bronze", "steel", "carbon");

    private WorkshopKinetic() {}

    static boolean tick(WorkshopNode node, ServerLevel level) {
        var facing = WorkshopBlock.direction(node.getBlockState());
        boolean active;
        switch (node.kind) {
            case KINETIC -> active = convert(node, level, facing);
            case ELECTRIC_KINETIC, MANUAL_KINETIC, WIND_KINETIC, WATER_KINETIC ->
                    active = node.kineticAt >= level.getGameTime() - 1;
            case STEAM_TURBINE -> {
                WorkshopDevices.push(node, node.byproduct, level, facing.getOpposite(), 1000);
                WorkshopDevices.push(node, node.outlet, level, Direction.DOWN, 100);
                active = node.kineticAt >= level.getGameTime() - 1;
            }
            case STIRLING_KINETIC -> {
                WorkshopDevices.push(node, node.outlet, level, Direction.DOWN, 100);
                active = node.kineticAt >= level.getGameTime() - 1;
            }
            default -> {
                return false;
            }
        }
        node.setActive(level, active);
        return true;
    }

    private static boolean convert(WorkshopNode node, ServerLevel level, Direction facing) {
        var pos = node.getBlockPos().relative(facing);
        int room = (int) ((node.kind.capacity - node.energy) * 4);
        if (room <= 0
                || !level.hasChunkAt(pos)
                || !(level.getBlockEntity(pos) instanceof WorkshopNode input)) {
            return false;
        }
        int obtained = draw(input, level, facing.getOpposite(), room);
        if (obtained <= 0) {
            return false;
        }
        node.energy += obtained * 0.25;
        node.setChanged();
        return true;
    }

    static void crank(WorkshopNode node, ServerLevel level) {
        if (node.heat >= 120
                || node.handledAt != Long.MIN_VALUE && level.getGameTime() - node.handledAt < 5) {
            return;
        }
        node.heat = Math.min(120, node.heat + 40);
        node.handledAt = level.getGameTime();
        node.setChanged();
    }

    private static int draw(WorkshopNode node, ServerLevel level, Direction side, int maximum) {
        if (node.isRemoved() || WorkshopBlock.direction(node.getBlockState()) != side) {
            return 0;
        }
        long tick = level.getGameTime();
        if (node.kineticAt == tick) {
            return 0;
        }
        int delivered =
                switch (node.kind) {
                    case ELECTRIC_KINETIC -> motor(node, maximum);
                    case MANUAL_KINETIC -> {
                        int work = Math.min(node.heat, maximum);
                        node.heat -= work;
                        yield work;
                    }
                    case WATER_KINETIC -> Math.min(water(node, level), maximum);
                    case WIND_KINETIC -> Math.min(wind(node, level, side), maximum);
                    case STIRLING_KINETIC -> stirling(node, level, side, maximum);
                    case STEAM_TURBINE -> turbine(node, level, side, maximum);
                    default -> 0;
                };
        if (delivered > 0) {
            node.kineticAt = tick;
            node.setChanged();
        }
        return delivered;
    }

    private static int motor(WorkshopNode node, int maximum) {
        var motors = node.inventory.getItem(0);
        if (!WorkshopPlatform.itemId(motors).equals("ic2:electric_motor")) {
            return 0;
        }
        int provided =
                Math.min(
                        maximum,
                        Math.min((int) (node.energy * 4), Math.min(10, motors.getCount()) * 100));
        node.energy -= provided * 0.25;
        return provided;
    }

    private static int water(WorkshopNode node, ServerLevel level) {
        if (rotor(node) == 0) {
            return 0;
        }
        var pos = node.getBlockPos();
        if (level.getFluidState(pos.below()).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            var biome = level.getBiome(pos);
            if (biome.is(Biomes.DEEP_OCEAN)
                    || biome.is(Biomes.DEEP_COLD_OCEAN)
                    || biome.is(Biomes.DEEP_FROZEN_OCEAN)
                    || biome.is(Biomes.DEEP_LUKEWARM_OCEAN)) {
                return 400;
            }
            if (biome.is(Biomes.OCEAN)
                    || biome.is(Biomes.COLD_OCEAN)
                    || biome.is(Biomes.FROZEN_OCEAN)
                    || biome.is(Biomes.WARM_OCEAN)
                    || biome.is(Biomes.LUKEWARM_OCEAN)) {
                return 300;
            }
            if (biome.is(Biomes.RIVER) || biome.is(Biomes.FROZEN_RIVER)) {
                return 37;
            }
        }
        return 0;
    }

    static int wind(WorkshopNode node, ServerLevel level, Direction side) {
        int quality = rotor(node);
        if (quality == 0 || !side.getAxis().isHorizontal()) {
            return 0;
        }
        var center = node.getBlockPos().relative(side.getOpposite());
        var move = new BlockPos.MutableBlockPos();
        boolean alongX = side.getAxis() == Direction.Axis.X;
        for (int vertical = -2; vertical <= 2; vertical++) {
            for (int horizontal = -2; horizontal <= 2; horizontal++) {
                move.set(
                        center.getX() + (alongX ? 0 : horizontal),
                        center.getY() + vertical,
                        center.getZ() + (alongX ? horizontal : 0));
                if (!level.hasChunkAt(move) || !level.getBlockState(move).isAir()) {
                    return 0;
                }
            }
        }
        long sampled = level.getGameTime() / 200L;
        int variation =
                10
                                * Math.floorMod(
                                        sampled
                                                + node.getBlockPos().getX()
                                                + node.getBlockPos().getZ(),
                                        4)
                        - 10;
        int altitude = Math.max(0, Math.min(200, center.getY() - 64));
        return Math.max(0, (altitude + variation) * quality / 100);
    }

    static int rotor(WorkshopNode node) {
        return ROTOR_RATINGS.getOrDefault(WorkshopPlatform.itemId(node.inventory.getItem(0)), 0);
    }

    private static int stirling(WorkshopNode node, ServerLevel level, Direction side, int maximum) {
        String hotWater = "ic2:hot_water";
        if (maximum < 12 || node.inlet.amount <= 0 || node.outlet.fill(hotWater, 1, true) != 1) {
            return 0;
        }
        int heat = WorkshopDevices.neighboringHeat(node, level, side.getOpposite(), 3);
        if (heat < 3) {
            return 0;
        }
        node.inlet.drain(1, false);
        node.outlet.fill(hotWater, 1, false);
        return 12;
    }

    private static int turbine(WorkshopNode node, ServerLevel level, Direction side, int maximum) {
        if (node.inlet.amount < 100
                || !WorkshopPlatform.itemId(node.inventory.getItem(0))
                        .equals("ic2:steam_turbine")) {
            return 0;
        }
        boolean superheated = node.inlet.id.equals(WorkshopThermal.SUPERHEATED);
        int output = superheated ? 400 : 200;
        boolean distill = node.cycles == 9;
        if (maximum < output
                || distill && node.outlet.fill(WorkshopThermal.DISTILLED, 1, true) != 1) {
            return 0;
        }
        if (node.byproduct.fill(WorkshopThermal.STEAM, 90, true) != 90
                || superheated
                        && !WorkshopPlatform.hasExternalFluidPort(
                                level, node.getBlockPos().relative(side.getOpposite()), side)) {
            return 0;
        }
        node.byproduct.fill(WorkshopThermal.STEAM, 90, false);
        if (distill) {
            node.outlet.fill(WorkshopThermal.DISTILLED, 1, false);
        }
        node.inlet.drain(100, false);
        node.cycles = (node.cycles + 1) % 10;
        return output;
    }
}
