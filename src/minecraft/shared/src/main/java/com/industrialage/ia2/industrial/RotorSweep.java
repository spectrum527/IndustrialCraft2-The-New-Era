package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;

/** Checks the rotor's actual oriented swept disk without loading neighboring chunks. */
final class RotorSweep {
    private RotorSweep() {}

    static boolean clear(WorkshopNode node, ServerLevel level, boolean submerged) {
        int diameter = WindRotorMount.diameter(node.kind, node.inventory.getItem(0));
        if (diameter == 0) return false;
        Direction face = WindRotorMount.face(node);
        var center = node.getBlockPos().relative(face);
        var position = new BlockPos.MutableBlockPos();
        int extent = diameter / 2;
        double radius = diameter / 2.0;
        for (int first = -extent; first <= extent; first++) {
            for (int second = -extent; second <= extent; second++) {
                double nearestFirst = Math.max(0, Math.abs(first) - 0.5);
                double nearestSecond = Math.max(0, Math.abs(second) - 0.5);
                if (nearestFirst * nearestFirst + nearestSecond * nearestSecond > radius * radius) {
                    continue;
                }
                switch (face.getAxis()) {
                    case X ->
                            position.set(
                                    center.getX(), center.getY() + first, center.getZ() + second);
                    case Y ->
                            position.set(
                                    center.getX() + first, center.getY(), center.getZ() + second);
                    case Z ->
                            position.set(
                                    center.getX() + first, center.getY() + second, center.getZ());
                }
                if (!level.hasChunkAt(position)
                        || !level.getBlockState(position)
                                .getCollisionShape(level, position)
                                .isEmpty()
                        || submerged && !level.getFluidState(position).is(FluidTags.WATER)) {
                    return false;
                }
            }
        }
        return true;
    }
}
