package com.industrialage.ia2.world;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class WorldAutomation {
    private static final int ROUTER_INPUT = 18;
    private static final int EJECTOR_SLOT = 26;
    private static final int PULLER_SLOT = 25;

    private WorldAutomation() {}

    static boolean tick(WorldMachine machine, ServerLevel level, long tick) {
        return switch (machine.kind) {
            case BATCH_CRAFTER -> tick % 16 == 0 && craft(machine, level);
            case ITEM_BUFFER -> tick % 8 == 0 && buffer(machine, level);
            case SORTING_MACHINE -> tick % 8 == 0 && sort(machine, level);
            case WEIGHTED_DISTRIBUTOR -> tick % 8 == 0 && distribute(machine, level);
            case CHUNK_LOADER -> machine.loadChunks(level);
            default -> false;
        };
    }

    private static boolean craft(WorldMachine node, ServerLevel level) {
        if (!node.canPay(32) || level.hasNeighborSignal(node.getBlockPos())) return false;
        var grid = new ArrayList<ItemStack>(9);
        boolean configured = false;
        for (int i = 0; i < 9; i++) {
            var template = node.inventory.getItem(i + 18);
            var ingredient = node.inventory.getItem(i);
            if (template.isEmpty()) {
                grid.add(ItemStack.EMPTY);
            } else {
                if (!ingredient.is(template.getItem())) return false;
                grid.add(ingredient.copy());
                configured = true;
            }
        }
        if (!configured) return false;
        var result = WorldCrafting.result(level, grid);
        if (result == null || result.output().isEmpty()) return false;
        var outputs = new ArrayList<ItemStack>();
        outputs.add(result.output());
        var returnedToGrid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            var expected = grid.get(i);
            if (expected.isEmpty()) continue;
            var remainder = result.remainders().get(i);
            if (!remainder.isEmpty()
                    && expected.getCount() == 1
                    && expected.is(remainder.getItem())) {
                returnedToGrid[i] = remainder;
            } else if (!remainder.isEmpty()) {
                outputs.add(remainder);
            }
        }
        if (!node.inventory.store(outputs)) return false;
        for (int i = 0; i < 9; i++) {
            if (grid.get(i).isEmpty()) continue;
            if (returnedToGrid[i] == null) node.inventory.removeItem(i, 1);
            else node.inventory.setItem(i, returnedToGrid[i]);
        }
        return node.pay(32);
    }

    private static boolean buffer(WorldMachine node, ServerLevel server) {
        if (!node.inventory.has(EJECTOR_SLOT, "ic2:ejector_upgrade")
                || !MachineAccess.allowsUpgrade(node, front(node), true, false)) return false;
        return push(node, server, firstInput(node, 26), front(node), 16) > 0;
    }

    private static boolean sort(WorldMachine node, ServerLevel server) {
        if (!node.canPay(2) || server.hasNeighborSignal(node.getBlockPos())) return false;
        for (int input = 0; input < ROUTER_INPUT; input++) {
            var stack = node.inventory.getItem(input);
            if (stack.isEmpty()) continue;
            Direction output = Direction.DOWN;
            for (Direction side : Direction.values()) {
                var filter = node.inventory.getItem(ROUTER_INPUT + side.ordinal());
                if (matchesSortFilter(filter, stack)) {
                    output = side;
                    break;
                }
            }
            if (push(node, server, input, output, 16) > 0) return node.pay(2);
        }
        return false;
    }

    private static boolean matchesSortFilter(ItemStack filter, ItemStack stack) {
        if (filter.isEmpty()) return false;
        String tag = MachineAutomation.tagSpec(filter);
        if (tag != null) {
            return WorkshopPlatform.tagged(stack, tag);
        }
        return WorldNative.sameStack(filter, stack);
    }

    private static boolean distribute(WorldMachine node, ServerLevel server) {
        Direction inlet = front(node);
        boolean pulled =
                node.inventory.has(PULLER_SLOT, "ic2:pulling_upgrade") && pull(node, server, inlet);
        int input = firstInput(node, ROUTER_INPUT);
        if (input < 0) return pulled;
        int max = Math.min(16, node.inventory.getItem(input).getCount());
        int used = 0;
        var outputs = new ArrayList<Direction>(6);
        for (Direction side : Direction.values()) {
            if (side != inlet && weight(node, side) > 0) outputs.add(side);
        }
        if (outputs.isEmpty()) outputs.add(inlet.getOpposite());
        outputs.sort(Comparator.comparingInt((Direction side) -> weight(node, side)).reversed());
        for (Direction output : outputs) {
            used += push(node, server, input, output, max - used);
            if (used >= max) break;
        }
        return pulled || used > 0;
    }

    private static int weight(WorldMachine node, Direction side) {
        return node.inventory.getItem(ROUTER_INPUT + side.ordinal()).getCount();
    }

    private static int firstInput(WorldMachine node, int slots) {
        for (int i = 0; i < slots; i++) {
            if (!node.inventory.getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    private static boolean pull(WorldMachine node, ServerLevel server, Direction side) {
        if (!MachineAccess.allowsUpgrade(node, side, false, false)) return false;
        for (int slot = 0; slot < ROUTER_INPUT; slot++) {
            if (!node.inventory.getItem(slot).isEmpty()) continue;
            var from = node.getBlockPos().relative(side);
            if (!server.hasChunkAt(from)) return false;
            var stack = WorldItemAccess.pull(server, from, side.getOpposite(), 16);
            if (stack.isEmpty()) return false;
            node.inventory.setItem(slot, stack);
            return true;
        }
        return false;
    }

    private static int push(
            WorldMachine node, ServerLevel server, int index, Direction side, int limit) {
        if (index < 0 || limit <= 0) return 0;
        var from = node.inventory.getItem(index);
        if (from.isEmpty()) return 0;
        if (!MachineAccess.allowsOutput(node, side)) return 0;
        var to = node.getBlockPos().relative(side);
        if (!server.hasChunkAt(to)) return 0;
        var transfer = from.copy();
        transfer.setCount(Math.min(from.getCount(), limit));
        int accepted = WorldItemAccess.push(server, to, side.getOpposite(), transfer);
        if (accepted > 0) node.inventory.removeItem(index, accepted);
        return accepted;
    }

    static Direction front(WorldMachine node) {
        return node.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }
}
