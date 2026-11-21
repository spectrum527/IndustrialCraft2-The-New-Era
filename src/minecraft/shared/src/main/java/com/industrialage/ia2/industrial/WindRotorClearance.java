package com.industrialage.ia2.industrial;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Loaded wind turbines indexed by chunk; no lookup forces another chunk to load. */
final class WindRotorClearance {
    private static final WeakHashMap<ServerLevel, Map<Long, Set<WorkshopNode>>> LOADED =
            new WeakHashMap<>();

    private WindRotorClearance() {}

    static void attach(WorkshopNode node) {
        if (node.kind != WorkshopKind.WIND_KINETIC
                || !(node.getLevel() instanceof ServerLevel level)) return;
        LOADED.computeIfAbsent(level, ignored -> new HashMap<>())
                .computeIfAbsent(
                        chunkKey(node.getBlockPos().getX() >> 4, node.getBlockPos().getZ() >> 4),
                        ignored -> Collections.newSetFromMap(new WeakHashMap<>()))
                .add(node);
    }

    static void detach(WorkshopNode node) {
        if (!(node.getLevel() instanceof ServerLevel level)) return;
        var chunks = LOADED.get(level);
        if (chunks == null) return;
        long key = chunkKey(node.getBlockPos().getX() >> 4, node.getBlockPos().getZ() >> 4);
        var bucket = chunks.get(key);
        if (bucket != null) {
            bucket.remove(node);
            if (bucket.isEmpty()) chunks.remove(key);
        }
        if (chunks.isEmpty()) LOADED.remove(level);
    }

    static boolean overlaps(WorkshopNode node, ServerLevel level) {
        var chunks = LOADED.get(level);
        if (chunks == null) return false;
        var face = WindRotorMount.face(node);
        int diameter = WindRotorMount.diameter(node.inventory.getItem(0));
        if (!face.getAxis().isHorizontal() || diameter == 0) return false;
        var center = hub(node, face);
        double radius = diameter * 0.5;
        int chunkX = node.getBlockPos().getX() >> 4;
        int chunkZ = node.getBlockPos().getZ() >> 4;
        // Two maximum-radius rotors can only intersect at a center separation below 13 blocks.
        for (int x = chunkX - 1; x <= chunkX + 1; x++) {
            for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
                var bucket = chunks.get(chunkKey(x, z));
                if (bucket == null) continue;
                for (var other : bucket) {
                    if (other != node
                            && !other.isRemoved()
                            && other.getLevel() == level
                            && intersects(face, center, radius, other)) return true;
                }
            }
        }
        return false;
    }

    private static boolean intersects(
            Direction face, Vec3 center, double radius, WorkshopNode other) {
        var otherFace = WindRotorMount.face(other);
        int diameter = WindRotorMount.diameter(other.inventory.getItem(0));
        if (!otherFace.getAxis().isHorizontal() || diameter == 0) return false;
        var otherCenter = hub(other, otherFace);
        double otherRadius = diameter * 0.5;
        double dy = Math.abs(center.y - otherCenter.y);
        if (dy >= radius + otherRadius) return false;
        if (face.getAxis() == otherFace.getAxis()) {
            double plane =
                    face.getAxis() == Direction.Axis.X
                            ? Math.abs(center.x - otherCenter.x)
                            : Math.abs(center.z - otherCenter.z);
            if (plane >= 1.0) return false;
            double horizontal =
                    face.getAxis() == Direction.Axis.X
                            ? center.z - otherCenter.z
                            : center.x - otherCenter.x;
            return dy * dy + horizontal * horizontal
                    < (radius + otherRadius) * (radius + otherRadius);
        }
        // Perpendicular disks cross along a vertical line if their vertical ranges overlap.
        var xDisk = face.getAxis() == Direction.Axis.X ? center : otherCenter;
        var zDisk = face.getAxis() == Direction.Axis.Z ? center : otherCenter;
        double xRadius = face.getAxis() == Direction.Axis.X ? radius : otherRadius;
        double zRadius = face.getAxis() == Direction.Axis.Z ? radius : otherRadius;
        double xOffset = Math.abs(xDisk.z - zDisk.z);
        double zOffset = Math.abs(zDisk.x - xDisk.x);
        if (xOffset >= xRadius || zOffset >= zRadius) return false;
        double xVertical = Math.sqrt(xRadius * xRadius - xOffset * xOffset);
        double zVertical = Math.sqrt(zRadius * zRadius - zOffset * zOffset);
        return dy < xVertical + zVertical;
    }

    private static Vec3 hub(WorkshopNode node, Direction face) {
        return Vec3.atCenterOf(node.getBlockPos())
                .add(
                        face.getStepX() * WindRotorMount.OFFSET,
                        0,
                        face.getStepZ() * WindRotorMount.OFFSET);
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }
}
