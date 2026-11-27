package com.industrialage.ia2.industrial;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

final class WorkshopKinetic {
    static final Map<String, Integer> ROTOR_RATINGS = WindRotorMount.ratings();
    static final List<String> ROTOR_MATERIALS = WindRotorMount.names();

    private WorkshopKinetic() {}

    static boolean tick(WorkshopNode node, ServerLevel level) {
        var facing = WorkshopBlock.direction(node.getBlockState());
        boolean active;
        switch (node.kind) {
            case KINETIC -> active = convert(node, level, facing);
            case WIND_KINETIC -> {
                int available = wind(node, level, facing);
                if (available > 0) {
                    boolean overWind =
                            windStrength(node, level, facing)
                                    > WindRotorMount.maxSafeWind(node.inventory.getItem(0));
                    if (wearRotor(node, overWind)) {
                        node.rotorSampleTick = Long.MIN_VALUE;
                        available = 0;
                    }
                }
                node.auxiliaryUnits = available;
                node.publishRotorVisual(level, available);
                active = available > 0;
            }
            case WATER_KINETIC -> {
                int available = water(node, level);
                if (available > 0 && wearRotor(node, false)) {
                    node.rotorSampleTick = Long.MIN_VALUE;
                    node.waterConnectivity = null;
                    available = 0;
                }
                node.auxiliaryUnits = available;
                node.publishRotorVisual(level, available);
                active = available > 0;
            }
            case ELECTRIC_KINETIC, MANUAL_KINETIC ->
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
            node.auxiliaryUnits = 0;
            return false;
        }
        int obtained = draw(input, level, facing.getOpposite(), room);
        node.auxiliaryUnits = obtained;
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
        int motors = node.inventory.installedComponents("ic2:electric_motor");
        int provided = Math.min(maximum, Math.min((int) (node.energy * 4), motors * 100));
        node.energy -= provided * 0.25;
        return provided;
    }

    static boolean validWaterRotor(net.minecraft.world.item.ItemStack stack) {
        return WindRotorMount.type(stack) >= 2;
    }

    private static int water(WorkshopNode node, ServerLevel level) {
        long tick = level.getGameTime();
        if (node.rotorSampleTick != tick) {
            node.rotorSampleTick = tick;
            node.rotorSample = sampleWater(node, level);
        }
        return node.rotorSample;
    }

    private static int sampleWater(WorkshopNode node, ServerLevel level) {
        if (!validWaterRotor(node.inventory.getItem(0))
                || WindRotorClearance.overlaps(node, level)
                || !RotorSweep.clear(node, level, true)) {
            node.waterConnectivity = null;
            return 0;
        }
        if (node.waterConnectivity == null) node.waterConnectivity = new WaterConnectivity();
        return node.waterConnectivity.available(node, level);
    }

    static int wind(WorkshopNode node, ServerLevel level, Direction side) {
        long tick = level.getGameTime();
        if (node.rotorSampleTick != tick) {
            node.rotorSampleTick = tick;
            node.rotorSample = sampleWind(node, level, side);
        }
        return node.rotorSample;
    }

    private static int sampleWind(WorkshopNode node, ServerLevel level, Direction side) {
        int quality = rotor(node);
        if (quality == 0
                || !side.getAxis().isHorizontal()
                || WindRotorClearance.overlaps(node, level)) {
            return 0;
        }
        if (!RotorSweep.clear(node, level, false)) return 0;
        return windStrength(node, level, side) * quality / 100;
    }

    static int windStrength(WorkshopNode node, ServerLevel level, Direction side) {
        var center = node.getBlockPos().relative(side.getOpposite());
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
        return Math.max(0, altitude + variation);
    }

    record WearOutcome(int nextWearTicks, int nextDamage, boolean broke) {}

    static WearOutcome advanceRotorWear(
            int wearTicks, int currentDamage, int maxDamage, boolean overWind) {
        if (maxDamage <= 0) return new WearOutcome(wearTicks, currentDamage, false);
        int updatedTicks = wearTicks + 1;
        if (updatedTicks < 20) return new WearOutcome(updatedTicks, currentDamage, false);
        updatedTicks -= 20;
        int damage = overWind ? 4 : 1;
        int next = currentDamage + damage;
        if (next >= maxDamage) {
            return new WearOutcome(updatedTicks, maxDamage, true);
        }
        return new WearOutcome(updatedTicks, next, false);
    }

    private static boolean wearRotor(WorkshopNode node, boolean overWind) {
        var rotor = node.inventory.getItem(0);
        int maxDamage = WindRotorMount.maxDurability(rotor);
        if (maxDamage <= 0) return false;
        var outcome =
                advanceRotorWear(node.rotorWearTicks, rotor.getDamageValue(), maxDamage, overWind);
        node.rotorWearTicks = outcome.nextWearTicks();
        if (outcome.broke()) {
            node.inventory.setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
            node.setChanged();
            return true;
        }
        if (outcome.nextDamage() != rotor.getDamageValue()) {
            rotor.setDamageValue(outcome.nextDamage());
            node.setChanged();
        }
        return false;
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
        boolean superheated = WorkshopThermal.isSuperheatedSteam(node.inlet.id);
        int output = superheated ? 400 : 200;
        boolean distill = node.cycles == 9;
        if (maximum < output
                || distill && node.outlet.fill(WorkshopThermal.DISTILLED, 1, true) != 1) {
            return 0;
        }
        if (superheated
                && (node.byproduct.fill(WorkshopThermal.STEAM, 90, true) != 90
                        || !WorkshopPlatform.hasExternalFluidPort(
                                level, node.getBlockPos().relative(side.getOpposite()), side))) {
            return 0;
        }
        if (superheated) {
            node.byproduct.fill(WorkshopThermal.STEAM, 90, false);
        }
        if (distill) {
            node.outlet.fill(WorkshopThermal.DISTILLED, 1, false);
        }
        node.inlet.drain(100, false);
        node.cycles = (node.cycles + 1) % 10;
        return output;
    }
}
