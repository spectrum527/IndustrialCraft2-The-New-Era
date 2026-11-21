package com.industrialage.ia2.wrench;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Geometry shared by wrench use and the overlay on the targeted part of a block face. */
public record WrenchSurface(
        Direction normal,
        Direction right,
        Direction up,
        double plane,
        double left,
        double bottom,
        double width,
        double height) {
    private static final double EPSILON = 0.0001;

    /** A rectangular target measured from the lower-left corner of the selected face. */
    public record Area(double u0, double v0, double u1, double v1, Direction direction) {
        /** The center has no new facing and requests dismantling. */
        public boolean center() {
            return direction == null;
        }

        boolean contains(double u, double v) {
            double rounding = 1e-7;
            return u >= u0 - rounding
                    && u <= u1 + rounding
                    && v >= v0 - rounding
                    && v <= v1 + rounding;
        }
    }

    /** Finds the actual cuboid surface hit, including the smaller faces of unconnected pipes. */
    public static WrenchSurface find(VoxelShape shape, BlockHitResult hit) {
        var side = hit.getDirection();
        var up =
                switch (side) {
                    case UP -> Direction.NORTH;
                    case DOWN -> Direction.SOUTH;
                    default -> Direction.UP;
                };
        var right =
                switch (side) {
                    case DOWN, UP, SOUTH -> Direction.EAST;
                    case NORTH -> Direction.WEST;
                    case EAST -> Direction.NORTH;
                    case WEST -> Direction.SOUTH;
                };
        var point = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        WrenchSurface largest = null;
        for (AABB box : shape.toAabbs()) {
            var low = new Vec3(box.minX, box.minY, box.minZ);
            var high = new Vec3(box.maxX, box.maxY, box.maxZ);
            double plane = Math.max(project(side, low), project(side, high));
            if (Math.abs(project(side, point) - plane) > EPSILON) continue;
            double first = project(right, low), last = project(right, high);
            double lowV = project(up, low), highV = project(up, high);
            double left = Math.min(first, last), width = Math.abs(first - last);
            double bottom = Math.min(lowV, highV), height = Math.abs(lowV - highV);
            double u = project(right, point), v = project(up, point);
            if (u < left - EPSILON
                    || u > left + width + EPSILON
                    || v < bottom - EPSILON
                    || v > bottom + height + EPSILON) continue;
            if (largest == null || width * height > largest.width * largest.height) {
                largest = new WrenchSurface(side, right, up, plane, left, bottom, width, height);
            }
        }
        return largest;
    }

    /** Returns the four separated outer targets and the central dismantle target. */
    public List<Area> areas() {
        return List.of(
                new Area(.06, .32, .26, .68, right.getOpposite()),
                new Area(.74, .32, .94, .68, right),
                new Area(.32, .06, .68, .26, up.getOpposite()),
                new Area(.32, .74, .68, .94, up),
                new Area(.34, .34, .66, .66, null));
    }

    /** Resolves a hit to the same rectangle the player sees, or null in the gaps. */
    public Area area(BlockHitResult hit, List<Area> areas) {
        var point = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        double u = (project(right, point) - left) / width;
        double v = (project(up, point) - bottom) / height;
        for (var area : areas) {
            if (area.contains(u, v)) return area;
        }
        return null;
    }

    /** Converts the coordinates of a colored rectangle into camera-relative render coordinates. */
    public Vec3 vertex(BlockPos pos, double u, double v, Vec3 camera) {
        return Vec3.atLowerCornerOf(pos)
                .subtract(camera)
                .add(vector(normal).scale(plane + .002))
                .add(vector(right).scale(left + u * width))
                .add(vector(up).scale(bottom + v * height));
    }

    private static Vec3 vector(Direction direction) {
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private static double project(Direction direction, Vec3 point) {
        return point.x * direction.getStepX()
                + point.y * direction.getStepY()
                + point.z * direction.getStepZ();
    }
}
