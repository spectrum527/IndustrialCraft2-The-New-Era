package com.industrialage.ia2.industrial;

import com.industrialage.ia2.power.WorldPowerStorage;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class WorkshopMatter {
    static final int FIRST_PATTERN = 14;
    private static final int END_PATTERNS = FIRST_PATTERN + 12;
    private static final String UU = "ic2:uu_matter";
    private static final int UU_ENERGY = 1_000_000;
    private static final int SCAN_COST_MICROLITERS = 10;

    private WorkshopMatter() {}

    static boolean tick(WorkshopNode node, ServerLevel level) {
        var side = WorkshopBlock.direction(node.getBlockState());
        boolean active;
        switch (node.kind) {
            case CLASSIC_ELECTROLYZER -> active = classicElectrolyzer(node, level, side);
            case MASS_FABRICATOR, CLASSIC_MASS, MATTER_GENERATOR ->
                    active = fabricate(node, level, side);
            case UU_SCANNER -> active = scan(node);
            case PATTERN_STORAGE -> active = false;
            case REPLICATOR -> active = replicate(node, level);
            default -> {
                return false;
            }
        }
        node.setActive(level, active);
        return true;
    }

    private static boolean classicElectrolyzer(
            WorkshopNode node, ServerLevel level, Direction direction) {
        var pos = node.getBlockPos().relative(direction);
        String inputId = node.mode == 0 ? "ic2:water_cell" : "ic2:electrolyzed_water_cell";
        var input = node.inventory.getItem(0);
        if (!WorkshopPlatform.itemId(input).equals(inputId)) {
            if (node.progress != 0) {
                node.progress = 0;
                node.setChanged();
            }
            return false;
        }
        var result =
                WorkshopPlatform.stack(
                        node.mode == 0 ? "ic2:electrolyzed_water_cell" : "ic2:water_cell", 1);
        if (!node.canOutputTo(0, result)
                || !level.hasChunkAt(pos)
                || !(level.getBlockEntity(pos) instanceof WorldPowerStorage storage)) {
            return false;
        }
        int delta = node.mode == 0 ? -20_000 : 18_000;
        if (!storage.exchangeStoredEnergy(delta, true)) {
            return false;
        }
        if (++node.progress >= node.kind.duration) {
            storage.exchangeStoredEnergy(delta, false);
            node.inventory.removeItem(0, 1);
            node.appendOutput(result);
            node.progress = 0;
        }
        node.setChanged();
        return true;
    }

    static int scrap(String id) {
        return switch (id) {
            case "ic2:scrap" -> 150000;
            case "ic2:scrap_box" -> 1350000;
            default -> 0;
        };
    }

    static void applyAmplifier(WorkshopNode node, double received) {
        if (!massFabricator(node.kind) || node.amplifier <= 0) {
            return;
        }
        int amplified = Math.min(node.amplifier, (int) received);
        double helped = Math.min(UU_ENERGY - node.energy, amplified * 5.0);
        node.amplifier -= (int) Math.ceil(helped / 5);
        node.energy += helped;
    }

    private static boolean massFabricator(WorkshopKind kind) {
        return kind == WorkshopKind.MASS_FABRICATOR
                || kind == WorkshopKind.CLASSIC_MASS
                || kind == WorkshopKind.MATTER_GENERATOR;
    }

    private static boolean fabricate(WorkshopNode node, ServerLevel level, Direction direction) {
        if (node.kind == WorkshopKind.MATTER_GENERATOR) {
            WorkshopDevices.push(node, node.outlet, level, direction, 1000);
        }
        if (node.energy > 0 && node.energy < UU_ENERGY && node.amplifier <= 0) {
            int points = scrap(WorkshopPlatform.itemId(node.inventory.getItem(0)));
            if (points > 0) {
                node.inventory.removeItem(0, 1);
                node.amplifier = points;
                node.setChanged();
            }
        }
        if (node.energy < UU_ENERGY) {
            return node.energy > 0;
        }
        if (node.kind == WorkshopKind.MATTER_GENERATOR) {
            if (node.outlet.fill(UU, 1, true) == 0) {
                return false;
            }
            node.outlet.fill(UU, 1, false);
        } else {
            var item = WorkshopPlatform.stack(UU, 1);
            if (item.isEmpty() || !node.canOutputTo(0, item)) {
                return false;
            }
            node.appendOutput(item);
        }
        node.energy -= UU_ENERGY;
        node.setChanged();
        return true;
    }

    static boolean scannable(ItemStack item) {
        return WorkshopPlatform.itemId(item).equals("minecraft:cobblestone")
                && WorkshopPlatform.sameItem(
                        item, WorkshopPlatform.stack("minecraft:cobblestone", 1));
    }

    private static boolean scan(WorkshopNode node) {
        var input = node.inventory.getItem(0);
        node.preview =
                node.inventory.getItem(FIRST_PATTERN).isEmpty()
                        ? input
                        : node.inventory.getItem(FIRST_PATTERN);
        if (!node.inventory.getItem(FIRST_PATTERN).isEmpty() || !scannable(input)) {
            if (node.inventory.getItem(FIRST_PATTERN).isEmpty() && node.progress != 0) {
                node.progress = 0;
                node.setChanged();
            }
            return false;
        }
        if (node.energy < node.kind.workingEu) {
            return false;
        }
        node.energy -= node.kind.workingEu;
        if (++node.progress >= node.kind.duration) {
            node.inventory.setItem(FIRST_PATTERN, node.inventory.removeItem(0, 1));
        }
        node.setChanged();
        return true;
    }

    static void recordOrSelect(WorkshopNode node, ServerLevel level, Player player) {
        recordOrSelect(node, level, player, 1);
    }

    static void recordOrSelect(WorkshopNode node, ServerLevel level, Player player, int step) {
        if (node.kind == WorkshopKind.PATTERN_STORAGE) {
            for (int offset = 1; offset <= END_PATTERNS - FIRST_PATTERN; offset++) {
                int next = Math.floorMod(node.cycles + offset * step, END_PATTERNS - FIRST_PATTERN);
                if (!node.inventory.getItem(FIRST_PATTERN + next).isEmpty()) {
                    node.cycles = next;
                    node.setChanged();
                    break;
                }
            }
        } else {
            var target = neighboringStorage(node, level);
            var pattern = node.inventory.getItem(FIRST_PATTERN);
            if (target == null || pattern.isEmpty() || !record(target, pattern)) {
                return;
            }
            node.inventory.setItem(FIRST_PATTERN, ItemStack.EMPTY);
            node.progress = 0;
            node.cycles = 1;
            node.setChanged();
            WorkshopPlatform.announcePattern(player, pattern);
        }
    }

    static void useMemory(WorkshopNode node, Player player, ItemStack memory) {
        String stored = WorkshopPlatform.readCrystalPattern(memory);
        if (node.kind == WorkshopKind.PATTERN_STORAGE && !stored.isEmpty()) {
            var pattern = WorkshopPlatform.stack(stored, 1);
            if (scannable(pattern) && record(node, pattern)) {
                WorkshopPlatform.announcePattern(player, pattern);
            }
            return;
        }
        int slot =
                node.kind == WorkshopKind.UU_SCANNER
                        ? FIRST_PATTERN
                        : FIRST_PATTERN + Math.floorMod(node.cycles, END_PATTERNS - FIRST_PATTERN);
        var pattern = node.inventory.getItem(slot);
        if (stored.isEmpty() && scannable(pattern)) {
            WorkshopPlatform.writeCrystalPattern(memory, WorkshopPlatform.itemId(pattern));
            WorkshopPlatform.announcePattern(player, pattern);
        }
    }

    private static boolean record(WorkshopNode target, ItemStack pattern) {
        int free = -1;
        for (int slot = FIRST_PATTERN; slot < END_PATTERNS; slot++) {
            var current = target.inventory.getItem(slot);
            if (!current.isEmpty() && WorkshopPlatform.sameItem(current, pattern)) {
                return false;
            }
            if (free == -1 && current.isEmpty()) {
                free = slot;
            }
        }
        if (free < 0) {
            return false;
        }
        target.inventory.setItem(free, pattern.copy());
        return true;
    }

    private static WorkshopNode neighboringStorage(WorkshopNode node, ServerLevel level) {
        for (var side : Direction.values()) {
            var pos = node.getBlockPos().relative(side);
            if (level.hasChunkAt(pos)
                    && level.getBlockEntity(pos) instanceof WorkshopNode other
                    && other.kind == WorkshopKind.PATTERN_STORAGE) {
                return other;
            }
        }
        return null;
    }

    private static boolean replicate(WorkshopNode node, ServerLevel level) {
        if (node.mode == 0
                || node.energy < node.kind.workingEu
                || node.fractionalMatter < SCAN_COST_MICROLITERS && !node.inlet.has(UU, 1)) {
            return false;
        }
        var neighbor = neighboringStorage(node, level);
        if (neighbor == null) {
            return false;
        }
        var pattern =
                neighbor.inventory.getItem(
                        FIRST_PATTERN
                                + Math.floorMod(neighbor.cycles, END_PATTERNS - FIRST_PATTERN));
        node.preview = pattern;
        if (!scannable(pattern) || !node.canOutputTo(0, pattern)) {
            return false;
        }
        node.energy -= node.kind.workingEu;
        if (++node.progress >= node.kind.duration) {
            if (node.fractionalMatter < SCAN_COST_MICROLITERS) {
                node.inlet.drain(1, false);
                node.fractionalMatter += 1000;
            }
            node.fractionalMatter -= SCAN_COST_MICROLITERS;
            node.appendOutput(pattern.copy());
            node.progress = 0;
            if (node.mode == 1) {
                node.mode = 0;
            }
        }
        node.setChanged();
        return true;
    }
}
