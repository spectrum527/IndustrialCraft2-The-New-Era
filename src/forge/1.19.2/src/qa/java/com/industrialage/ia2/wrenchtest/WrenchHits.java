package com.industrialage.ia2.wrenchtest;

import com.industrialage.ia2.wrench.WrenchUse;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Produces real world surface hits for native player interaction tests. */
public final class WrenchHits {
    private WrenchHits() {}

    /** Hits a blue band on a neighboring face. */
    public static BlockHitResult turn(ServerLevel level, BlockPos pos, Direction direction) {
        var face = direction.getAxis() == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        return turn(level, pos, face, direction);
    }

    /** Hits the blue band for the given in-plane world direction. */
    public static BlockHitResult turn(
            ServerLevel level, BlockPos pos, Direction face, Direction direction) {
        var bounds = level.getBlockState(pos).getShape(level, pos).bounds();
        var vector = axis(direction);
        var low = new Vec3(bounds.minX, bounds.minY, bounds.minZ).dot(vector);
        var high = new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ).dot(vector);
        double band = Math.min(low, high) + Math.abs(high - low) * .84;
        var center = new Vec3(.5, .5, .5);
        var point =
                Vec3.atLowerCornerOf(pos).add(center).add(vector.scale(band - center.dot(vector)));
        return ray(level, pos, face, point);
    }

    /** Hits the central control of the specified face. */
    public static BlockHitResult center(ServerLevel level, BlockPos pos, Direction face) {
        return ray(level, pos, face, Vec3.atCenterOf(pos));
    }

    /** Locates a displayed control on a pipe whose face can shrink when the valve is closed. */
    public static BlockHitResult pipeControl(
            ServerLevel level, BlockPos pos, ServerPlayer player, Direction direction) {
        var shape = level.getBlockState(pos).getShape(level, pos);
        for (var face : Direction.values()) {
            if (face.getAxis() == direction.getAxis()) continue;
            var normal = axis(face).scale(1.5);
            for (int lane = -23; lane <= 23; lane++) {
                var point = Vec3.atCenterOf(pos).add(axis(direction).scale(lane / 48.0));
                var hit = shape.clip(point.add(normal), point.subtract(normal), pos);
                if (hit == null || hit.getDirection() != face) continue;
                var preview = WrenchUse.preview(level, player, hit);
                if (preview != null
                        && preview.selected() != null
                        && preview.selected().direction() == direction) return hit;
            }
        }
        throw new AssertionError("No visible pipe control for " + direction + " at " + pos);
    }

    /** Produces the actual intersection of a ray perpendicular to the specified face. */
    public static BlockHitResult ray(ServerLevel level, BlockPos pos, Direction face, Vec3 point) {
        var offset = axis(face).scale(1.5);
        var hit =
                level.getBlockState(pos)
                        .getShape(level, pos)
                        .clip(point.add(offset), point.subtract(offset), pos);
        if (hit == null || hit.getDirection() != face) {
            throw new AssertionError("Ray did not hit the " + face + " face of " + pos);
        }
        return hit;
    }

    private static Vec3 axis(Direction side) {
        return new Vec3(side.getStepX(), side.getStepY(), side.getStepZ());
    }
}
