package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class WorkshopCokeKiln {
    static final String BRICKS = "refractory_bricks";
    private static final String CREOSOTE = "ic2:creosote";

    private WorkshopCokeKiln() {}

    static boolean tick(WorkshopNode node, ServerLevel level) {
        if (node.kind == WorkshopKind.COKE_GRATE) {
            WorkshopDevices.push(node, node.outlet, level, Direction.DOWN, 1000);
            return true;
        }
        if (node.kind == WorkshopKind.COKE_HATCH) {
            return true;
        }
        if (node.kind != WorkshopKind.COKE_KILN) {
            return false;
        }
        var kiln = inspect(node, level);
        var current = node.getBlockState();
        var next =
                current.setValue(WorkshopBlock.FORMED, kiln != null)
                        .setValue(
                                WorkshopBlock.ACTIVE,
                                kiln != null && run(node, kiln.hatch(), kiln.grate()));
        if (current != next) level.setBlockAndUpdate(node.getBlockPos(), next);
        return true;
    }

    static boolean takeCreosote(
            WorkshopNode node, ServerLevel level, Player player, InteractionHand hand) {
        if (!node.getBlockState().getValue(WorkshopBlock.FORMED)) return false;
        var kiln = inspect(node, level);
        return kiln != null
                && WorkshopPlatform.interactFluid(kiln.grate(), player, hand, Direction.DOWN);
    }

    static WorkshopNode.Tank displayedOutlet(WorkshopNode node) {
        if (node.kind == WorkshopKind.COKE_KILN
                && node.getLevel() instanceof ServerLevel level
                && node.getBlockState().getValue(WorkshopBlock.FORMED)) {
            var pos = center(node).below();
            if (level.hasChunkAt(pos)
                    && level.getBlockEntity(pos) instanceof WorkshopNode grate
                    && grate.kind == WorkshopKind.COKE_GRATE) return grate.outlet;
        }
        return node.outlet;
    }

    private static BlockPos center(WorkshopNode node) {
        return node.getBlockPos()
                .relative(WorkshopBlock.direction(node.getBlockState()).getOpposite());
    }

    static boolean suitable(ItemStack item) {
        return WorkshopPlatform.itemId(item).equals("minecraft:coal")
                || WorkshopPlatform.tagged(item, "minecraft:logs");
    }

    private static boolean run(WorkshopNode node, WorkshopNode hatch, WorkshopNode grate) {
        var fed = hatch.inventory.getItem(0);
        var input = node.inventory.getItem(0);
        if (!fed.isEmpty()
                && suitable(fed)
                && (input.isEmpty()
                        || WorkshopPlatform.sameItem(input, fed)
                                && input.getCount() < input.getMaxStackSize())) {
            var transfer = hatch.inventory.removeItem(0, 1);
            if (input.isEmpty()) {
                node.inventory.setItem(0, transfer);
            } else {
                input.grow(1);
                node.setChanged();
            }
        }
        input = node.inventory.getItem(0);
        if (input.isEmpty() || !suitable(input)) {
            return false;
        }
        String recipe = WorkshopPlatform.itemId(input);
        boolean coal = recipe.equals("minecraft:coal");
        int byproduct = coal ? 500 : 250;
        var result = WorkshopPlatform.stack(coal ? "ic2:coke" : "minecraft:charcoal", 1);
        if (!node.canOutputTo(0, result)
                || grate.outlet.fill(CREOSOTE, byproduct, true) != byproduct) {
            return false;
        }
        if (!node.lastRecipe.equals(recipe)) {
            node.lastRecipe = recipe;
            node.progress = 0;
        }
        if (++node.progress >= node.kind.duration) {
            node.inventory.removeItem(0, 1);
            node.appendOutput(result);
            grate.outlet.fill(CREOSOTE, byproduct, false);
            grate.setChanged();
            node.progress = 0;
        }
        node.setChanged();
        return true;
    }

    private record Formation(WorkshopNode hatch, WorkshopNode grate) {}

    private static Formation inspect(WorkshopNode controller, ServerLevel level) {
        var front = controller.getBlockPos();
        var center = center(controller);
        if (level.hasChunkAt(center)
                && level.getBlockEntity(center.above()) instanceof WorkshopNode hatch
                && level.getBlockEntity(center.below()) instanceof WorkshopNode grate
                && hatch.kind == WorkshopKind.COKE_HATCH
                && grate.kind == WorkshopKind.COKE_GRATE
                && WorkshopBlock.direction(hatch.getBlockState()) == Direction.UP
                && WorkshopBlock.direction(grate.getBlockState()) == Direction.DOWN
                && level.getBlockState(center).isAir()) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        var pos = center.offset(x, y, z);
                        if (!level.hasChunkAt(pos)) {
                            return null;
                        }
                        if (x == 0 && z == 0 || pos.equals(front)) {
                            continue;
                        }
                        if (!WorkshopPlatform.blockId(level.getBlockState(pos).getBlock())
                                .equals("ic2:" + BRICKS)) {
                            return null;
                        }
                    }
                }
            }
            return new Formation(hatch, grate);
        }
        return null;
    }
}
