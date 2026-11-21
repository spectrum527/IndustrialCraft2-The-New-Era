package com.industrialage.ia2.industrial;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Loaded same-kind turbines indexed by chunk; no lookup forces another chunk to load. */
final class WindRotorClearance {
    private static final WeakHashMap<ServerLevel, Map<Long, Set<WorkshopNode>>> LOADED =
            new WeakHashMap<>();

    private WindRotorClearance() {}

    static void attach(WorkshopNode node) {
        if (!node.kind.hasWorldRotor() || !(node.getLevel() instanceof ServerLevel level)) return;
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
        int diameter = WindRotorMount.diameter(node.kind, node.inventory.getItem(0));
        if (diameter == 0) return false;
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
                            && other.kind == node.kind
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
        int diameter = WindRotorMount.diameter(other.kind, other.inventory.getItem(0));
        if (diameter == 0) return false;
        var otherCenter = hub(other, otherFace);
        double otherRadius = diameter * 0.5;
        var axis = face.getAxis();
        var otherAxis = otherFace.getAxis();
        if (axis == otherAxis) {
            if (Math.abs(coordinate(center, axis) - coordinate(otherCenter, axis)) >= 1.0)
                return false;
            double distanceSquared = 0;
            for (var direction : Direction.Axis.values()) {
                if (direction == axis) continue;
                double delta = coordinate(center, direction) - coordinate(otherCenter, direction);
                distanceSquared += delta * delta;
            }
            double sum = radius + otherRadius;
            return distanceSquared < sum * sum;
        }
        // Perpendicular disks intersect on a line parallel to the remaining world axis.
        double offset =
                Math.abs(coordinate(center, otherAxis) - coordinate(otherCenter, otherAxis));
        double otherOffset = Math.abs(coordinate(center, axis) - coordinate(otherCenter, axis));
        if (offset >= radius || otherOffset >= otherRadius) return false;
        double half = Math.sqrt(radius * radius - offset * offset);
        double otherHalf = Math.sqrt(otherRadius * otherRadius - otherOffset * otherOffset);
        for (var direction : Direction.Axis.values()) {
            if (direction != axis && direction != otherAxis) {
                return Math.abs(coordinate(center, direction) - coordinate(otherCenter, direction))
                        < half + otherHalf;
            }
        }
        return false;
    }

    private static double coordinate(Vec3 value, Direction.Axis axis) {
        return switch (axis) {
            case X -> value.x;
            case Y -> value.y;
            case Z -> value.z;
        };
    }

    private static Vec3 hub(WorkshopNode node, Direction face) {
        return Vec3.atCenterOf(node.getBlockPos())
                .add(
                        face.getStepX() * WindRotorMount.OFFSET,
                        face.getStepY() * WindRotorMount.OFFSET,
                        face.getStepZ() * WindRotorMount.OFFSET);
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }
}
