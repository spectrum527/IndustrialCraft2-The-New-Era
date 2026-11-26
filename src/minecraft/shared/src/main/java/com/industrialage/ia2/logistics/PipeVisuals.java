package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec.Kind;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared client-side interpolation and inner-bore fluid geometry for transparent item and fluid
 * pipes.
 */
final class PipeVisuals {
    private static final float[] ITEM_SCALES = {0.24F, 0.34F, 0.44F, 0.54F};
    private static final float WALL_INSET = 0.72F / 16.0F;

    private PipeVisuals() {}

    /** Sink receiving one outward-oriented fluid quad in block-local [0, 1] coordinates. */
    interface FluidQuadSink {
        void emit(
                Direction face,
                float x0,
                float y0,
                float z0,
                float x1,
                float y1,
                float z1,
                Direction flowDir);
    }

    /** Computes smooth normalized transit progress in [0, 1] for an item pipe on the client. */
    static float itemProgress(TransportNode node, float partialTick) {
        if (node.spec.kind() != Kind.ITEM_PIPE || node.cargo.isEmpty()) return 0.0F;
        var level = node.getLevel();
        if (level == null) return 0.5F;
        float elapsed = (level.getGameTime() - node.clientCargoStartTick) + partialTick;
        float progress = Mth.clamp(elapsed / (float) TransportNode.ITEM_TRANSIT_TICKS, 0.0F, 1.0F);
        return node.clientCargoStalled ? Math.min(progress, 0.5F) : progress;
    }

    /** Writes the interpolated block-local item center (x, y, z) into {@code out}. */
    static void itemPosition(TransportNode node, float partialTick, float[] out) {
        var state = node.getBlockState();
        Direction to = node.clientCargoTo;
        if (to != null && !TransportBlock.connected(state, to)) {
            to = null;
        }
        Direction from = node.clientCargoFrom;
        if (from == null) {
            if (to != null && TransportBlock.connected(state, to.getOpposite())) {
                from = to.getOpposite();
            } else if (state.hasProperty(TransportBlock.OUT)) {
                Direction back = state.getValue(TransportBlock.OUT).getOpposite();
                if (TransportBlock.connected(state, back)) from = back;
            }
        }
        float p = itemProgress(node, partialTick);
        if (to == null) {
            p = Math.min(p, 0.5F);
        }
        float startX = from == null ? 0.5F : 0.5F + from.getStepX() * 0.5F;
        float startY = from == null ? 0.5F : 0.5F + from.getStepY() * 0.5F;
        float startZ = from == null ? 0.5F : 0.5F + from.getStepZ() * 0.5F;
        if (p <= 0.5F || to == null) {
            float t = p * 2.0F;
            out[0] = Mth.lerp(t, startX, 0.5F);
            out[1] = Mth.lerp(t, startY, 0.5F);
            out[2] = Mth.lerp(t, startZ, 0.5F);
        } else {
            float t = (p - 0.5F) * 2.0F;
            float endX = 0.5F + to.getStepX() * 0.5F;
            float endY = 0.5F + to.getStepY() * 0.5F;
            float endZ = 0.5F + to.getStepZ() * 0.5F;
            out[0] = Mth.lerp(t, 0.5F, endX);
            out[1] = Mth.lerp(t, 0.5F, endY);
            out[2] = Mth.lerp(t, 0.5F, endZ);
        }
    }

    /** Returns the item render scale matching the pipe's inner bore diameter. */
    static float itemScale(int size) {
        return ITEM_SCALES[Mth.clamp(size, 0, ITEM_SCALES.length - 1)];
    }

    /** Returns a gentle spin angle in degrees for cargo traveling inside a pipe. */
    static float itemSpinDegrees(TransportNode node, float partialTick) {
        var level = node.getLevel();
        if (level == null) return 0.0F;
        return ((level.getGameTime() & 0xFFFFL) + partialTick) * 4.5F;
    }

    /** Smoothly eases the rendered fluid fill fraction toward the latest synced server volume. */
    static float smoothFluidFill(TransportNode node) {
        float target = node.clientFluidFillFraction();
        long now = System.nanoTime();
        if (node.clientFluidRenderNanos == 0L) {
            node.clientFluidFill = target;
            node.clientFluidRenderNanos = now;
            return target;
        }
        float dt = Math.min(0.25F, Math.max(0.0F, (now - node.clientFluidRenderNanos) * 1.0E-9F));
        node.clientFluidRenderNanos = now;
        float alpha = 1.0F - (float) Math.exp(-dt * 14.0F);
        node.clientFluidFill += (target - node.clientFluidFill) * alpha;
        if (target <= 0.0F && node.clientFluidFill < 0.004F) {
            node.clientFluidFill = 0.0F;
        }
        return Mth.clamp(node.clientFluidFill, 0.0F, 1.0F);
    }

    /** Emits seamless core and connected-arm fluid quads strictly inside the transparent bore. */
    static void emitFluidGeometry(
            BlockState state,
            int size,
            float fill,
            boolean gas,
            Direction flowDir,
            FluidQuadSink sink) {
        if (fill <= 0.004F) return;
        float c0 = (6 - size) / 16.0F + WALL_INSET;
        float c1 = (10 + size) / 16.0F - WALL_INSET;
        float bore = c1 - c0;
        if (bore <= 0.005F) return;

        boolean up = TransportBlock.connected(state, Direction.UP);
        boolean down = TransportBlock.connected(state, Direction.DOWN);
        boolean north = TransportBlock.connected(state, Direction.NORTH);
        boolean south = TransportBlock.connected(state, Direction.SOUTH);
        boolean west = TransportBlock.connected(state, Direction.WEST);
        boolean east = TransportBlock.connected(state, Direction.EAST);

        float visualFill = Mth.clamp(0.18F + fill * 0.82F, 0.18F, 1.0F);
        boolean verticalFull = (up && (fill > 0.45F || gas || flowDir == Direction.UP));
        float y0;
        float y1;
        if (gas) {
            y1 = c1;
            y0 = c1 - bore * visualFill;
        } else {
            y0 = c0;
            y1 = verticalFull ? c1 : c0 + bore * visualFill;
        }
        boolean fullTop = y1 >= c1 - 0.001F;
        boolean fullBottom = y0 <= c0 + 0.001F;
        boolean renderUpArm = up && (fullTop || gas || flowDir == Direction.UP);
        boolean renderDownArm = down && (fullBottom || !gas || flowDir == Direction.DOWN);

        // Central core prism: skip faces joined to a rendered arm to avoid internal seams.
        if (!renderUpArm) sink.emit(Direction.UP, c0, y0, c0, c1, y1, c1, flowDir);
        if (!renderDownArm) sink.emit(Direction.DOWN, c0, y0, c0, c1, y1, c1, flowDir);
        if (!north) sink.emit(Direction.NORTH, c0, y0, c0, c1, y1, c1, flowDir);
        if (!south) sink.emit(Direction.SOUTH, c0, y0, c0, c1, y1, c1, flowDir);
        if (!west) sink.emit(Direction.WEST, c0, y0, c0, c1, y1, c1, flowDir);
        if (!east) sink.emit(Direction.EAST, c0, y0, c0, c1, y1, c1, flowDir);

        // Horizontal arms: omit both inner core seam and outer block-boundary cap.
        if (north) {
            emitArmSides(Direction.NORTH, c0, y0, 0.0F, c1, y1, c0, flowDir, sink);
        }
        if (south) {
            emitArmSides(Direction.SOUTH, c0, y0, c1, c1, y1, 1.0F, flowDir, sink);
        }
        if (west) {
            emitArmSides(Direction.WEST, 0.0F, y0, c0, c0, y1, c1, flowDir, sink);
        }
        if (east) {
            emitArmSides(Direction.EAST, c1, y0, c0, 1.0F, y1, c1, flowDir, sink);
        }
        if (renderDownArm) {
            emitArmSides(Direction.DOWN, c0, 0.0F, c0, c1, y0, c1, flowDir, sink);
        }
        if (renderUpArm) {
            emitArmSides(Direction.UP, c0, y1, c0, c1, 1.0F, c1, flowDir, sink);
        }
    }

    private static void emitArmSides(
            Direction arm,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            Direction flowDir,
            FluidQuadSink sink) {
        for (Direction face : TransportNode.SIDES) {
            if (face == arm || face == arm.getOpposite()) continue;
            sink.emit(face, x0, y0, z0, x1, y1, z1, flowDir);
        }
    }

    /**
     * Writes the 4 vertex positions (12 floats) and normalized [0, 1] UVs (8 floats, oriented so +V
     * flows along {@code flowDir}) for one outward-facing quad.
     */
    static void buildQuad(
            Direction face,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            Direction flowDir,
            float[] pos12,
            float[] uv8) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        switch (face) {
            case UP -> {
                setPos(pos12, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
                setHorizontalCapUv(uv8, dx, dz, flowDir, true);
            }
            case DOWN -> {
                setPos(pos12, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
                setHorizontalCapUv(uv8, dx, dz, flowDir, false);
            }
            case NORTH -> {
                setPos(pos12, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
                setSideUv(uv8, dx, dy, flowDir, Direction.WEST, Direction.EAST);
            }
            case SOUTH -> {
                setPos(pos12, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
                setSideUv(uv8, dx, dy, flowDir, Direction.EAST, Direction.WEST);
            }
            case WEST -> {
                setPos(pos12, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
                setSideUv(uv8, dz, dy, flowDir, Direction.SOUTH, Direction.NORTH);
            }
            case EAST -> {
                setPos(pos12, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
                setSideUv(uv8, dz, dy, flowDir, Direction.NORTH, Direction.SOUTH);
            }
        }
    }

    private static void setPos(
            float[] p,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz) {
        p[0] = ax;
        p[1] = ay;
        p[2] = az;
        p[3] = bx;
        p[4] = by;
        p[5] = bz;
        p[6] = cx;
        p[7] = cy;
        p[8] = cz;
        p[9] = dx;
        p[10] = dy;
        p[11] = dz;
    }

    private static void setHorizontalCapUv(
            float[] uv, float dx, float dz, Direction flowDir, boolean top) {
        float uSpan = Math.min(1.0F, Math.max(0.12F, dx));
        float vSpan = Math.min(1.0F, Math.max(0.12F, dz));
        if (flowDir == Direction.EAST) {
            setUv(uv, 0, 0, uSpan, 0, uSpan, vSpan, 0, vSpan);
        } else if (flowDir == Direction.WEST) {
            setUv(uv, uSpan, vSpan, 0, vSpan, 0, 0, uSpan, 0);
        } else if (flowDir == (top ? Direction.NORTH : Direction.SOUTH)) {
            setUv(uv, 0, vSpan, 0, 0, uSpan, 0, uSpan, vSpan);
        } else {
            setUv(uv, 0, 0, 0, vSpan, uSpan, vSpan, uSpan, 0);
        }
    }

    private static void setSideUv(
            float[] uv,
            float horiz,
            float vert,
            Direction flowDir,
            Direction rightDir,
            Direction leftDir) {
        float uSpan = Math.min(1.0F, Math.max(0.12F, horiz));
        float vSpan = Math.min(1.0F, Math.max(0.12F, vert));
        if (flowDir == rightDir) {
            // Rotate UV so animated +V flows toward rightDir.
            setUv(uv, 0, 0, uSpan, 0, uSpan, vSpan, 0, vSpan);
        } else if (flowDir == leftDir) {
            // Rotate UV so animated +V flows toward leftDir.
            setUv(uv, uSpan, vSpan, 0, vSpan, 0, 0, uSpan, 0);
        } else if (flowDir == Direction.UP) {
            setUv(uv, 0, vSpan, 0, 0, uSpan, 0, uSpan, vSpan);
        } else {
            setUv(uv, 0, 0, 0, vSpan, uSpan, vSpan, uSpan, 0);
        }
    }

    private static void setUv(
            float[] uv,
            float u0,
            float v0,
            float u1,
            float v1,
            float u2,
            float v2,
            float u3,
            float v3) {
        uv[0] = u0;
        uv[1] = v0;
        uv[2] = u1;
        uv[3] = v1;
        uv[4] = u2;
        uv[5] = v2;
        uv[6] = u3;
        uv[7] = v3;
    }
}
