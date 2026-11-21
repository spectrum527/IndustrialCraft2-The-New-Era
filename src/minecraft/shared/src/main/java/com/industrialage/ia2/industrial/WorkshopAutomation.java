package com.industrialage.ia2.industrial;

import com.industrialage.ia2.content.ProcessingUpgrades;
import com.industrialage.ia2.world.MachineAutomation;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

final class WorkshopAutomation {
    private WorkshopAutomation() {}

    static boolean supported(WorkshopKind kind) {
        return switch (kind) {
            case METAL_FORMER,
                            ORE_WASHER,
                            CENTRIFUGE,
                            INDUCTION,
                            RECYCLER,
                            BLOCK_CUTTER,
                            CANNER,
                            CLASSIC_CANNER,
                            SOLID_CANNER,
                            FLUID_BOTTLER,
                            ELECTROLYZER,
                            PUMP,
                            UU_ASSEMBLY ->
                    true;
            default -> false;
        };
    }

    static boolean canInstall(WorkshopNode node, ItemStack item) {
        if (!supported(node.kind)) {
            return false;
        }
        if (node.kind == WorkshopKind.ELECTROLYZER) {
            return acceptsElectrolyzerUpgrade(item);
        }
        return switch (WorkshopPlatform.itemId(item)) {
            case "ic2:fluid_pulling_upgrade" ->
                    node.kind != WorkshopKind.PUMP && node.inlet.capacity > 0;
            case "ic2:fluid_ejector_upgrade" -> node.outlet.capacity > 0;
            case "ic2:redstone_inverter_upgrade" -> node.kind == WorkshopKind.INDUCTION;
            case "ic2:overclocker_upgrade" -> node.kind != WorkshopKind.INDUCTION;
            default -> MachineAutomation.isProcessingUpgrade(item);
        };
    }

    static boolean acceptsElectrolyzerUpgrade(ItemStack item) {
        return WorkshopPlatform.itemId(item).equals("ic2:fluid_pulling_upgrade");
    }

    static ProcessingUpgrades basic(WorkshopNode node) {
        return supported(node.kind) && node.kind != WorkshopKind.ELECTROLYZER
                ? MachineAutomation.installed(stacks(node))
                : ProcessingUpgrades.NONE;
    }

    static ProcessingUpgrades.Stats stats(
            WorkshopNode node, int duration, ProcessingUpgrades upgrades) {
        int reserve = Math.max(0, node.kind.capacity - duration * node.kind.workingEu);
        return upgrades.stats(duration, node.kind.workingEu, reserve, node.kind.tier);
    }

    static void tick(WorkshopNode node, ServerLevel level) {
        if (!supported(node.kind)) {
            return;
        }
        var installed = stacks(node);
        if (installed.stream().allMatch(ItemStack::isEmpty)) {
            return;
        }
        long tick = level.getGameTime();
        boolean washer = node.kind == WorkshopKind.ORE_WASHER;
        int inputs = node.kind.inputs(node.mode) + (washer ? 1 : 0);
        int outputs = node.kind.outputSlots() + (washer ? 1 : 0);
        int inputLane = inputs == 0 ? -1 : Math.floorMod(tick, inputs);
        int outputLane = outputs == 0 ? -1 : Math.floorMod(tick, outputs);
        int feed =
                node.kind == WorkshopKind.UU_ASSEMBLY
                        ? 1
                        : washer && inputLane == inputs - 1
                                ? WorkshopInventory.WASHER_WATER_SLOT
                                : inputLane;
        int result =
                outputLane < 0
                        ? -1
                        : washer && outputLane == outputs - 1
                                ? WorkshopInventory.WASHER_CONTAINER_RETURN_SLOT
                                : WorkshopInventory.OUTPUT_START + outputLane;
        MachineAutomation.transfer(
                level,
                node.getBlockPos(),
                node.inventory,
                feed,
                result,
                installed,
                incoming ->
                        feed >= 0
                                && WorkshopPlans.validAutomationInput(node, level, feed, incoming));

        for (var upgrade : installed) {
            String path = WorkshopPlatform.itemId(upgrade);
            if (path.equals("ic2:fluid_pulling_upgrade")) {
                fluid(node, level, upgrade, false);
            } else if (node.kind != WorkshopKind.ELECTROLYZER
                    && path.equals("ic2:fluid_ejector_upgrade")) {
                fluid(node, level, upgrade, true);
            }
        }
    }

    private static List<ItemStack> stacks(WorkshopNode node) {
        var items = node.inventory;
        int first = WorkshopInventory.UPGRADE_START;
        return List.of(
                items.getItem(first),
                items.getItem(first + 1),
                items.getItem(first + 2),
                items.getItem(first + 3));
    }

    static boolean effectiveSignal(WorkshopNode node, ServerLevel level) {
        boolean signal = level.hasNeighborSignal(node.getBlockPos());
        if (node.kind == WorkshopKind.INDUCTION
                && stacks(node).stream()
                        .anyMatch(
                                stack ->
                                        WorkshopPlatform.itemId(stack)
                                                .equals("ic2:redstone_inverter_upgrade"))) {
            return !signal;
        }
        return signal;
    }

    private static void fluid(
            WorkshopNode node, ServerLevel level, ItemStack upgrade, boolean eject) {
        int rate = Math.min(1000, 100 * ProcessingUpgrades.transferLimit(upgrade.getCount()));
        for (var side : MachineAutomation.faces(upgrade)) {
            if (eject) {
                var tank = node.accessibleOutput(side);
                if (tank != null) {
                    WorkshopDevices.push(node, tank, level, side, rate);
                }
            } else if (node.exposesIntake(side)) {
                var adjacent = node.getBlockPos().relative(side);
                int space = Math.min(rate, node.inlet.capacity - node.inlet.amount);
                var advertised =
                        WorkshopPlatform.pullFluid(
                                level, adjacent, side.getOpposite(), node.inlet.id, space, true);
                if (advertised.amount() > 0 && node.canReceiveFluid(advertised.id())) {
                    var moved =
                            WorkshopPlatform.pullFluid(
                                    level,
                                    adjacent,
                                    side.getOpposite(),
                                    advertised.id(),
                                    advertised.amount(),
                                    false);
                    if (moved.amount() > 0) {
                        node.receiveFluid(moved.id(), moved.amount(), false);
                    }
                }
            }
        }
    }
}
