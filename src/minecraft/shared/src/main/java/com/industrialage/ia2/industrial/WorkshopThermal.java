package com.industrialage.ia2.industrial;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

final class WorkshopThermal {
    static final String WATER = "minecraft:water";
    static final String DISTILLED = "ic2:distilled_water";
    static final String STEAM = "ic2:steam";
    static final String SUPERHEATED = "ic2:superheated_steam";
    private static final String BIOGAS = "ic2:biogas";

    private WorkshopThermal() {}

    static boolean tick(WorkshopNode node, ServerLevel level) {
        boolean active;
        var facing = WorkshopBlock.direction(node.getBlockState());
        switch (node.kind) {
            case SEMIFLUID -> active = semifluid(node);
            case RADIOISOTOPE_HEAT -> active = pellets(node) > 0;
            case FLUID_HEAT -> {
                node.refillContainers(0, WorkshopInventory.OUTPUT_START);
                active = node.inlet.amount > 0;
            }
            case FERMENTER -> active = ferment(node, level, facing);
            case STEAM_BOILER -> active = boil(node, level, facing);
            case CONDENSER -> active = condense(node, level, facing);
            case STEAM_REPRESSURIZER -> active = repressurize(node, level, facing);
            case SOLAR_DISTILLER -> active = distill(node, level, facing);
            case HEAT_EXCHANGER -> {
                WorkshopDevices.push(node, node.outlet, level, facing.getOpposite(), 1000);
                active = node.handledAt >= level.getGameTime() - 1;
            }
            default -> {
                return false;
            }
        }
        node.setActive(level, active);
        return true;
    }

    static boolean isWater(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, WATER, "minecraft:water", "c:water");
    }

    static boolean isDistilledWater(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, DISTILLED, "c:distilled_water");
    }

    static boolean isSteam(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, STEAM, "c:steam");
    }

    static boolean isSuperheatedSteam(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, SUPERHEATED, "c:superheated_steam");
    }

    static boolean isBiogas(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, BIOGAS, "c:biogas");
    }

    static boolean isCreosote(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, "ic2:creosote", "c:creosote");
    }

    static boolean isLava(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, "minecraft:lava", "minecraft:lava", "c:lava");
    }

    static boolean isPahoehoeLava(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, "ic2:pahoehoe_lava", "c:pahoehoe_lava");
    }

    static boolean isBiomass(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, "ic2:biomass", "c:biomass");
    }

    static boolean isCoolant(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, "ic2:coolant", "c:coolant");
    }

    static boolean isHotCoolant(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, "ic2:hot_coolant", "c:hot_coolant");
    }

    static boolean isAir(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, "ic2:air", "c:air");
    }

    static boolean isUuMatter(String fluid) {
        return WorkshopPlatform.fluidMatches(fluid, "ic2:uu_matter", "c:uu_matter");
    }

    static boolean validFuel(WorkshopKind kind, String fluid) {
        return isBiogas(fluid)
                || isCreosote(fluid)
                || kind == WorkshopKind.FLUID_HEAT && (isLava(fluid) || isPahoehoeLava(fluid));
    }

    private static boolean semifluid(WorkshopNode node) {
        node.refillContainers(0, WorkshopInventory.OUTPUT_START);
        int produced = isBiogas(node.inlet.id) ? 16 : 8;
        if (node.inlet.amount == 0 || node.energy + produced > node.kind.capacity) {
            return false;
        }
        node.inlet.drain(1, false);
        node.energy += produced;
        node.setChanged();
        return true;
    }

    private static int pellets(WorkshopNode node) {
        int pellets = 0;
        for (int slot = 0; slot < node.kind.inputs(node.mode); slot++) {
            if (WorkshopPlatform.itemId(node.inventory.getItem(slot)).equals("ic2:rtg_pellet")) {
                pellets++;
            }
        }
        return pellets;
    }

    static int drawHeat(WorkshopNode node, ServerLevel level, int maximum) {
        long time = level.getGameTime();
        if (node.handledAt != time) {
            node.handledAt = time;
            node.auxiliaryUnits = 0;
        }
        int nativeHeat;
        if (node.kind == WorkshopKind.RADIOISOTOPE_HEAT) {
            nativeHeat =
                    switch (pellets(node)) {
                        case 1 -> 2;
                        case 2 -> 4;
                        case 3 -> 10;
                        case 4 -> 20;
                        case 5 -> 30;
                        case 6 -> 40;
                        default -> 0;
                    };
        } else {
            nativeHeat = node.inlet.amount == 0 ? 0 : isBiogas(node.inlet.id) ? 40 : 20;
        }
        int offered = Math.max(0, Math.min(maximum, nativeHeat - node.auxiliaryUnits));
        if (offered > 0) {
            if (node.kind == WorkshopKind.FLUID_HEAT && node.auxiliaryUnits == 0) {
                node.inlet.drain(1, false);
            }
            node.auxiliaryUnits += offered;
            node.setChanged();
        }
        return offered;
    }

    private static boolean ferment(WorkshopNode node, ServerLevel level, Direction facing) {
        WorkshopDevices.push(node, node.outlet, level, facing.getOpposite(), 400);
        if (node.inlet.amount < 20
                || !isBiomass(node.inlet.id)
                || node.outlet.fill(BIOGAS, 400, true) != 400) {
            return false;
        }
        boolean fertilize = node.cycles == 24;
        var fertilizer = WorkshopPlatform.stack("ic2:fertilizer", 1);
        if (fertilize && (fertilizer.isEmpty() || !node.canOutputTo(0, fertilizer))) {
            return false;
        }
        int obtained = WorkshopDevices.neighboringHeat(node, level, facing, 100);
        if (obtained <= 0) {
            return false;
        }
        node.heat += obtained;
        if (node.heat >= 800) {
            node.heat -= 800;
            node.inlet.drain(20, false);
            node.outlet.fill(BIOGAS, 400, false);
            node.cycles = (node.cycles + 1) % 25;
            if (fertilize) {
                node.appendOutput(fertilizer);
            }
        }
        node.setChanged();
        return true;
    }

    private static boolean boil(WorkshopNode node, ServerLevel level, Direction facing) {
        WorkshopDevices.push(node, node.outlet, level, facing.getOpposite(), 1000);
        int delivered = WorkshopDevices.neighboringHeat(node, level, facing, 100);
        int temperature =
                Math.min(100000, Math.max(0, node.heat + (delivered > 0 ? delivered : -1)));
        String gas = node.mode == 0 ? STEAM : SUPERHEATED;
        int threshold = node.mode == 0 ? 20_000 : 60_000;
        boolean producing =
                node.inlet.amount > 0
                        && temperature >= threshold
                        && node.outlet.fill(gas, 100, true) == 100
                        && node.cycles < 10_000;
        int boilCost = node.mode == 0 ? 110 : 300;
        if (producing) {
            if (isWater(node.inlet.id) && !isDistilledWater(node.inlet.id)) {
                node.cycles++;
            }
            node.inlet.drain(1, false);
            node.outlet.fill(gas, 100, false);
            temperature = Math.max(0, temperature - boilCost);
        }
        if (node.heat != temperature || producing) {
            node.heat = temperature;
            node.setChanged();
        }
        return delivered > 0 || producing;
    }

    static int exchangeHeat(WorkshopNode node, ServerLevel level, int maximum) {
        long tick = level.getGameTime();
        if (node.handledAt != tick) {
            node.handledAt = tick;
            node.auxiliaryUnits = 0;
        }
        int able = Math.min(maximum, 100 - node.auxiliaryUnits);
        int required = Math.max(0, (able - node.heat + 9) / 10);
        int water = Math.min(node.inlet.amount, node.outlet.fill("ic2:coolant", required, true));
        if (water > 0) {
            node.inlet.drain(water, false);
            node.outlet.fill("ic2:coolant", water, false);
            node.heat += water * 10;
        }
        int heat = Math.min(able, node.heat);
        if (heat > 0) {
            node.heat -= heat;
            node.auxiliaryUnits += heat;
            node.setChanged();
        }
        return heat;
    }

    private static boolean distill(WorkshopNode node, ServerLevel level, Direction facing) {
        WorkshopDevices.push(node, node.outlet, level, facing, 1000);
        var sky = node.getBlockPos().above();
        if (node.inlet.amount == 0
                || node.outlet.fill(DISTILLED, 1, true) != 1
                || !level.dimensionType().hasSkyLight()
                || level.getSkyDarken() >= 4
                || !level.canSeeSky(sky)
                || level.isRainingAt(sky)) {
            return false;
        }
        if (++node.progress >= node.kind.duration) {
            node.inlet.drain(1, false);
            node.outlet.fill(DISTILLED, 1, false);
            node.progress = 0;
        }
        node.setChanged();
        return true;
    }

    private static boolean repressurize(WorkshopNode node, ServerLevel level, Direction facing) {
        WorkshopDevices.push(node, node.outlet, level, facing, 1000);
        int batch = Math.min(node.inlet.amount, 100);
        int ratio = isSuperheatedSteam(node.inlet.id) ? 2 : 1;
        if (batch <= 0
                || node.energy < node.kind.workingEu
                || node.outlet.fill(STEAM, batch * ratio, true) != batch * ratio) {
            return false;
        }
        node.inlet.drain(batch, false);
        node.outlet.fill(STEAM, batch * ratio, false);
        node.energy -= node.kind.workingEu;
        node.setChanged();
        return true;
    }

    private static boolean condense(WorkshopNode node, ServerLevel level, Direction facing) {
        WorkshopDevices.push(node, node.outlet, level, facing, 1000);
        if (node.energy < node.kind.workingEu
                || node.inlet.amount < 1000
                || node.outlet.fill(DISTILLED, 1, true) != 1) {
            return false;
        }
        node.inlet.drain(1000, false);
        node.outlet.fill(DISTILLED, 1, false);
        node.energy -= node.kind.workingEu;
        node.setChanged();
        return true;
    }
}
