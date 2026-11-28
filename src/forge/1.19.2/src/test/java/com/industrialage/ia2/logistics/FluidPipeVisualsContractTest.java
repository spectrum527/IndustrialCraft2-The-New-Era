package com.industrialage.ia2.logistics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/**
 * Forge 1.19.2 host-adapter contracts for per-face fluid pipe flow tracking, fork stabilization,
 * and fluid quad UV/geometry emission.
 */
class FluidPipeVisualsContractTest {
    private record EmittedQuad(
            Direction face,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            Direction flowDir) {}

    @Test
    void perFaceMaskRoundTripsAllSixFacesAndClampsInvalidBits() {
        int mask = 0;
        mask = PipeVisuals.withFlow(mask, Direction.WEST, PipeVisuals.FLOW_IN);
        mask = PipeVisuals.withFlow(mask, Direction.NORTH, PipeVisuals.FLOW_OUT);
        mask = PipeVisuals.withFlow(mask, Direction.UP, PipeVisuals.FLOW_OUT);

        assertEquals(PipeVisuals.FLOW_IN, PipeVisuals.flowOf(mask, Direction.WEST));
        assertEquals(PipeVisuals.FLOW_OUT, PipeVisuals.flowOf(mask, Direction.NORTH));
        assertEquals(PipeVisuals.FLOW_OUT, PipeVisuals.flowOf(mask, Direction.UP));
        assertEquals(PipeVisuals.FLOW_NONE, PipeVisuals.flowOf(mask, Direction.EAST));
        assertEquals(PipeVisuals.FLOW_NONE, PipeVisuals.flowOf(mask, Direction.SOUTH));
        assertEquals(PipeVisuals.FLOW_NONE, PipeVisuals.flowOf(mask, Direction.DOWN));

        assertEquals(Direction.EAST, PipeVisuals.armFlowDir(mask, Direction.WEST));
        assertEquals(Direction.NORTH, PipeVisuals.armFlowDir(mask, Direction.NORTH));
        assertEquals(Direction.UP, PipeVisuals.armFlowDir(mask, Direction.UP));
        assertNull(PipeVisuals.armFlowDir(mask, Direction.EAST));

        int corrupted = 0b11 << (Direction.SOUTH.ordinal() * 2);
        assertEquals(PipeVisuals.FLOW_NONE, PipeVisuals.flowOf(corrupted, Direction.SOUTH));
        assertNull(PipeVisuals.armFlowDir(corrupted, Direction.SOUTH));
    }

    @Test
    void branchedRoundRobinFlowStabilizesWithoutFlickering() {
        var tee = new FluidFlowTracker();
        int expectedTee =
                PipeVisuals.withFlow(
                        PipeVisuals.withFlow(
                                PipeVisuals.withFlow(0, Direction.WEST, PipeVisuals.FLOW_IN),
                                Direction.NORTH,
                                PipeVisuals.FLOW_OUT),
                        Direction.SOUTH,
                        PipeVisuals.FLOW_OUT);

        for (long tick = 0; tick < 60; tick++) {
            tee.record(Direction.WEST, FluidFlowTracker.IN, tick, 32);
            Direction branch = (tick % 2 == 0) ? Direction.NORTH : Direction.SOUTH;
            tee.record(branch, FluidFlowTracker.OUT, tick, 32);
            int mask = tee.sampleMask(tick);
            if (tick >= 1) {
                assertEquals(
                        expectedTee,
                        mask,
                        "T-fork mask must stay constant without flickering at tick " + tick);
            }
        }

        var fiveWay = new FluidFlowTracker();
        Direction[] outlets = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.UP, Direction.DOWN
        };
        int expectedFiveWay = PipeVisuals.withFlow(0, Direction.WEST, PipeVisuals.FLOW_IN);
        for (Direction out : outlets) {
            expectedFiveWay = PipeVisuals.withFlow(expectedFiveWay, out, PipeVisuals.FLOW_OUT);
        }
        for (long tick = 0; tick < 50; tick++) {
            fiveWay.record(Direction.WEST, FluidFlowTracker.IN, tick, 32);
            fiveWay.record(outlets[(int) (tick % outlets.length)], FluidFlowTracker.OUT, tick, 32);
            int mask = fiveWay.sampleMask(tick);
            if (tick >= outlets.length - 1) {
                assertEquals(
                        expectedFiveWay,
                        mask,
                        "5-way fork mask must stay stable across round-robin cycles at tick "
                                + tick);
            }
        }
    }

    @Test
    void sameFaceSloshResistsFlippingWhileGenuineReversalAndExpiryWork() {
        var tracker = new FluidFlowTracker();
        tracker.record(Direction.EAST, FluidFlowTracker.OUT, 0, 20);
        assertEquals(
                PipeVisuals.FLOW_OUT, PipeVisuals.flowOf(tracker.sampleMask(0), Direction.EAST));

        for (long tick = 1; tick <= 20; tick++) {
            int dir = (tick % 2 == 1) ? FluidFlowTracker.IN : FluidFlowTracker.OUT;
            tracker.record(Direction.EAST, dir, tick, 4);
            assertEquals(
                    PipeVisuals.FLOW_OUT,
                    PipeVisuals.flowOf(tracker.sampleMask(tick), Direction.EAST),
                    "1-tick alternating slosh must not flip face at tick " + tick);
        }

        var snap = tracker.snapshot();

        tracker.record(Direction.EAST, FluidFlowTracker.IN, 21, 32);
        assertEquals(
                PipeVisuals.FLOW_OUT, PipeVisuals.flowOf(tracker.sampleMask(21), Direction.EAST));
        tracker.record(Direction.EAST, FluidFlowTracker.IN, 22, 32);
        assertEquals(
                PipeVisuals.FLOW_OUT, PipeVisuals.flowOf(tracker.sampleMask(22), Direction.EAST));
        tracker.record(Direction.EAST, FluidFlowTracker.IN, 23, 32);
        assertEquals(
                PipeVisuals.FLOW_IN, PipeVisuals.flowOf(tracker.sampleMask(23), Direction.EAST));

        tracker.restore(snap);
        assertEquals(
                PipeVisuals.FLOW_OUT, PipeVisuals.flowOf(tracker.sampleMask(21), Direction.EAST));

        assertEquals(
                PipeVisuals.FLOW_OUT,
                PipeVisuals.flowOf(
                        tracker.sampleMask(20 + FluidFlowTracker.HOLD_TICKS), Direction.EAST));
        assertEquals(
                0, tracker.sampleMask(20 + FluidFlowTracker.HOLD_TICKS + 1), "Stale flow expires");
    }

    @Test
    void coreAndArmFlowDirectionsMatchPipeTopologies() {
        int straight =
                PipeVisuals.withFlow(
                        PipeVisuals.withFlow(0, Direction.WEST, PipeVisuals.FLOW_IN),
                        Direction.EAST,
                        PipeVisuals.FLOW_OUT);
        assertEquals(Direction.EAST, PipeVisuals.coreFlowDir(straight));

        int bend =
                PipeVisuals.withFlow(
                        PipeVisuals.withFlow(0, Direction.WEST, PipeVisuals.FLOW_IN),
                        Direction.NORTH,
                        PipeVisuals.FLOW_OUT);
        assertNull(PipeVisuals.coreFlowDir(bend));

        int split = PipeVisuals.withFlow(bend, Direction.SOUTH, PipeVisuals.FLOW_OUT);
        assertNull(PipeVisuals.coreFlowDir(split));

        int merge =
                PipeVisuals.withFlow(
                        PipeVisuals.withFlow(
                                PipeVisuals.withFlow(0, Direction.WEST, PipeVisuals.FLOW_IN),
                                Direction.NORTH,
                                PipeVisuals.FLOW_IN),
                        Direction.EAST,
                        PipeVisuals.FLOW_OUT);
        assertNull(PipeVisuals.coreFlowDir(merge));
        assertNull(PipeVisuals.coreFlowDir(0));
    }

    @Test
    void emitFluidGeometryIsolatesBranchArmsAndStabilizesLowFillVerticalFork() {
        int size = 2;
        float c0 = (6 - size) / 16.0F + 0.72F / 16.0F;
        float c1 = (10 + size) / 16.0F - 0.72F / 16.0F;

        int teeMask =
                PipeVisuals.withFlow(
                        PipeVisuals.withFlow(
                                PipeVisuals.withFlow(0, Direction.WEST, PipeVisuals.FLOW_IN),
                                Direction.NORTH,
                                PipeVisuals.FLOW_OUT),
                        Direction.SOUTH,
                        PipeVisuals.FLOW_OUT);
        List<EmittedQuad> teeQuads = new ArrayList<>();
        PipeVisuals.emitFluidGeometry(
                false,
                false,
                true,
                true,
                true,
                false,
                size,
                0.6F,
                false,
                teeMask,
                (face, x0, y0, z0, x1, y1, z1, dir) ->
                        teeQuads.add(new EmittedQuad(face, x0, y0, z0, x1, y1, z1, dir)));

        assertFalse(teeQuads.isEmpty());
        for (var q : teeQuads) {
            if (q.flowDir != null) {
                assertNotEquals(
                        q.face.getAxis(),
                        q.flowDir.getAxis(),
                        "No quad may flow perpendicular to its own face");
            }
            if (q.x1 <= c0 + 1e-4F) {
                assertEquals(Direction.EAST, q.flowDir, "WEST inlet arm flows EAST toward core");
            } else if (q.z1 <= c0 + 1e-4F) {
                assertEquals(Direction.NORTH, q.flowDir, "NORTH outlet arm flows NORTH");
            } else if (q.z0 >= c1 - 1e-4F) {
                assertEquals(Direction.SOUTH, q.flowDir, "SOUTH outlet arm flows SOUTH");
            } else {
                assertNull(q.flowDir, "Fork core must use isotropic still texture");
            }
        }

        int vertForkMask =
                PipeVisuals.withFlow(
                        PipeVisuals.withFlow(
                                PipeVisuals.withFlow(0, Direction.WEST, PipeVisuals.FLOW_IN),
                                Direction.UP,
                                PipeVisuals.FLOW_OUT),
                        Direction.EAST,
                        PipeVisuals.FLOW_OUT);
        List<EmittedQuad> vertQuads = new ArrayList<>();
        PipeVisuals.emitFluidGeometry(
                true,
                false,
                false,
                false,
                true,
                true,
                size,
                0.25F,
                false,
                vertForkMask,
                (face, x0, y0, z0, x1, y1, z1, dir) ->
                        vertQuads.add(new EmittedQuad(face, x0, y0, z0, x1, y1, z1, dir)));

        long upArmQuads = vertQuads.stream().filter(q -> q.y0 >= c1 - 1e-4F).count();
        assertEquals(4, upArmQuads, "Active UP branch must render all 4 vertical arm faces");
        vertQuads.stream()
                .filter(q -> q.y0 >= c1 - 1e-4F)
                .forEach(q -> assertEquals(Direction.UP, q.flowDir));
        vertQuads.stream()
                .filter(q -> q.x0 >= c1 - 1e-4F)
                .forEach(q -> assertEquals(Direction.EAST, q.flowDir));

        int deadEndMask = PipeVisuals.withFlow(0, Direction.WEST, PipeVisuals.FLOW_IN);
        List<EmittedQuad> deadEndQuads = new ArrayList<>();
        PipeVisuals.emitFluidGeometry(
                false,
                false,
                false,
                false,
                true,
                false,
                size,
                0.8F,
                false,
                deadEndMask,
                (face, x0, y0, z0, x1, y1, z1, dir) ->
                        deadEndQuads.add(new EmittedQuad(face, x0, y0, z0, x1, y1, z1, dir)));
        var eastCap =
                deadEndQuads.stream()
                        .filter(q -> q.face == Direction.EAST)
                        .findFirst()
                        .orElseThrow();
        assertNull(eastCap.flowDir, "Dead-end cap normal to flow axis must render still");
        var topCore =
                deadEndQuads.stream()
                        .filter(q -> q.face == Direction.UP && q.x0 >= c0 - 1e-4F)
                        .findFirst()
                        .orElseThrow();
        assertEquals(Direction.EAST, topCore.flowDir);
    }

    @Test
    void buildQuadOrientsPlusVAlongFlowDirWithProportionalSpans() {
        float x0 = 0.20F, y0 = 0.30F, z0 = 0.10F;
        float x1 = 0.70F, y1 = 0.55F, z1 = 0.90F;
        float[] pos = new float[12];
        float[] uv = new float[8];

        for (Direction face : Direction.values()) {
            for (Direction flowDir : Direction.values()) {
                if (flowDir.getAxis() == face.getAxis()) continue;
                PipeVisuals.buildQuad(face, x0, y0, z0, x1, y1, z1, flowDir, pos, uv);

                float maxV = Math.max(Math.max(uv[1], uv[3]), Math.max(uv[5], uv[7]));
                float minV = Math.min(Math.min(uv[1], uv[3]), Math.min(uv[5], uv[7]));
                float maxU = Math.max(Math.max(uv[0], uv[2]), Math.max(uv[4], uv[6]));
                float minU = Math.min(Math.min(uv[0], uv[2]), Math.min(uv[4], uv[6]));
                float hiX = 0, hiY = 0, hiZ = 0, loX = 0, loY = 0, loZ = 0;
                for (int i = 0; i < 4; i++) {
                    float v = uv[i * 2 + 1];
                    if (Math.abs(v - maxV) < 1e-5F) {
                        hiX += pos[i * 3];
                        hiY += pos[i * 3 + 1];
                        hiZ += pos[i * 3 + 2];
                    } else {
                        loX += pos[i * 3];
                        loY += pos[i * 3 + 1];
                        loZ += pos[i * 3 + 2];
                    }
                }
                float vx = (hiX - loX) * 0.5F;
                float vy = (hiY - loY) * 0.5F;
                float vz = (hiZ - loZ) * 0.5F;
                float dot =
                        vx * flowDir.getStepX() + vy * flowDir.getStepY() + vz * flowDir.getStepZ();
                assertTrue(
                        dot > 0.1F,
                        "Expected +V to align with "
                                + flowDir
                                + " on face "
                                + face
                                + ", got dot="
                                + dot);

                float expectedV =
                        Math.abs(
                                (x1 - x0) * flowDir.getStepX()
                                        + (y1 - y0) * flowDir.getStepY()
                                        + (z1 - z0) * flowDir.getStepZ());
                assertEquals(
                        expectedV,
                        maxV - minV,
                        1e-4F,
                        "V span must match length along " + flowDir + " on face " + face);
                assertTrue(maxU - minU >= 0.12F - 1e-4F);
            }
        }
    }
}
