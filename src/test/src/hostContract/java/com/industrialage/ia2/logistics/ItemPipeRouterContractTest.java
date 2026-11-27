package com.industrialage.ia2.logistics;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.logistics.ItemPipeRouter.Edge;
import com.industrialage.ia2.logistics.ItemPipeRouter.Graph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** Host-adapter contracts for ItemPipeRouter BFS routing, nozzle OUT flipping, and CellFamily. */
class ItemPipeRouterContractTest {
    private static final class MockPipeGraph implements Graph<String> {
        private final Map<String, List<Edge<String>>> adj = new HashMap<>();
        private final Set<String> readySinks = new HashSet<>();
        private final Set<String> explicitOutlets = new HashSet<>();
        private final Set<String> implicitSinks = new HashSet<>();
        private final Set<String> incompatible = new HashSet<>();
        private final Set<String> full = new HashSet<>();

        void link(String from, Direction side, String to) {
            adj.computeIfAbsent(from, ignored -> new ArrayList<>()).add(new Edge<>(to, side));
            adj.computeIfAbsent(to, ignored -> new ArrayList<>())
                    .add(new Edge<>(from, side.getOpposite()));
        }

        void setReadySink(String node, boolean ready) {
            if (ready) readySinks.add(node);
            else readySinks.remove(node);
        }

        void setExplicitOutlet(String node, boolean explicit) {
            if (explicit) explicitOutlets.add(node);
            else explicitOutlets.remove(node);
        }

        void setImplicitSink(String node, boolean ready) {
            if (ready) implicitSinks.add(node);
            else implicitSinks.remove(node);
        }

        void setIncompatible(String node, boolean value) {
            if (value) incompatible.add(node);
            else incompatible.remove(node);
        }

        void setFull(String node, boolean value) {
            if (value) full.add(node);
            else full.remove(node);
        }

        @Override
        public List<Edge<String>> links(String node) {
            return adj.getOrDefault(node, List.of());
        }

        @Override
        public boolean isReadySink(String node) {
            return readySinks.contains(node);
        }

        @Override
        public boolean hasExplicitOutlet(String node) {
            return explicitOutlets.contains(node);
        }

        @Override
        public boolean isReadyImplicitSink(String node) {
            return implicitSinks.contains(node);
        }

        @Override
        public boolean isCompatible(String node) {
            return !incompatible.contains(node);
        }

        @Override
        public boolean canReceive(String node, Direction entryFace) {
            return !incompatible.contains(node) && !full.contains(node);
        }
    }

    @Test
    void multiSegmentRoutingTraversesUnrotatedIntermediatePipesToReadySink() {
        // Pipe 1 (arbitrary OUT) --EAST--> Pipe 2 (unrotated OUT) --EAST--> Pipe 3 (ready sink)
        var graph = new MockPipeGraph();
        graph.link("pipe1", Direction.EAST, "pipe2");
        graph.link("pipe2", Direction.EAST, "pipe3");
        graph.setReadySink("pipe3", true);

        assertEquals(Direction.EAST, ItemPipeRouter.route("pipe1", graph, 0));
        assertEquals(Direction.EAST, ItemPipeRouter.route("pipe2", graph, 1));
    }

    @Test
    void explicitOutletAndNonOutletIsolationStopsRoutingWhenNoReadySinkExists() {
        var graph = new MockPipeGraph();
        graph.link("pipe1", Direction.EAST, "pipe2");
        graph.link("pipe2", Direction.EAST, "pipe3");

        // Pipe 3 adjacent container is not on OUT and has no INJECTION nozzle -> not a ready sink.
        assertNull(ItemPipeRouter.route("pipe1", graph, 0));
        assertNull(ItemPipeRouter.route("pipe2", graph, 0));

        // Even if the source itself were marked ready, source never routes to itself.
        graph.setReadySink("pipe1", true);
        assertNull(ItemPipeRouter.route("pipe1", graph, 0));

        // Enabling Pipe 3 as an explicit OUT or INJECTION sink immediately unlocks routing.
        graph.setReadySink("pipe3", true);
        assertEquals(Direction.EAST, ItemPipeRouter.route("pipe1", graph, 0));
    }

    @Test
    void squareLoopDetoursAroundFullOrIncompatibleBranchAndHandlesBackpressure() {
        // Short branch: source --EAST--> top --SOUTH--> sink (2 hops)
        // Long branch:  source --SOUTH--> left --SOUTH--> corner --EAST--> sink (3 hops)
        var graph = new MockPipeGraph();
        graph.link("source", Direction.EAST, "top");
        graph.link("top", Direction.SOUTH, "sink");
        graph.link("source", Direction.SOUTH, "left");
        graph.link("left", Direction.SOUTH, "corner");
        graph.link("corner", Direction.EAST, "sink");
        graph.setReadySink("sink", true);

        // Shortest clear path prefers EAST via top.
        assertEquals(Direction.EAST, ItemPipeRouter.route("source", graph, 0));
        assertEquals(Direction.EAST, ItemPipeRouter.route("source", graph, 1));

        // When immediate neighbor "top" is full (same item), router detours via SOUTH -> left.
        graph.setFull("top", true);
        assertEquals(Direction.SOUTH, ItemPipeRouter.route("source", graph, 0));
        assertEquals(Direction.SOUTH, ItemPipeRouter.route("left", graph, 1));
        assertEquals(Direction.EAST, ItemPipeRouter.route("corner", graph, 2));

        // When "top" holds an incompatible item, router also detours via SOUTH -> left.
        graph.setFull("top", false);
        graph.setIncompatible("top", true);
        assertEquals(Direction.SOUTH, ItemPipeRouter.route("source", graph, 0));

        // When both branches are blocked by full/incompatible pipes, router returns null.
        graph.setFull("left", true);
        assertNull(ItemPipeRouter.route("source", graph, 0));

        // When the sink becomes full (backpressure), router returns null cleanly.
        graph.setIncompatible("top", false);
        graph.setFull("left", false);
        graph.setReadySink("sink", false);
        assertNull(ItemPipeRouter.route("source", graph, 0));
    }

    @Test
    void equalLengthForksRoundRobinByGameTime() {
        var graph = new MockPipeGraph();
        graph.link("source", Direction.EAST, "north_leg");
        graph.link("north_leg", Direction.SOUTH, "sink");
        graph.link("source", Direction.SOUTH, "south_leg");
        graph.link("south_leg", Direction.EAST, "sink");
        graph.setReadySink("sink", true);

        assertEquals(Direction.EAST, ItemPipeRouter.route("source", graph, 0));
        assertEquals(Direction.SOUTH, ItemPipeRouter.route("source", graph, 1));
        assertEquals(Direction.EAST, ItemPipeRouter.route("source", graph, 2));
    }

    @Test
    void extractionNozzleAutoFlipsConflictingOutputFace() {
        // Prefers a connected neighboring pipe face when present.
        assertEquals(
                Direction.SOUTH,
                ItemPipeRouter.redirectedOutput(Direction.WEST, side -> side == Direction.SOUTH));
        // Ignores the extraction face itself even if predicate returned true.
        assertEquals(
                Direction.EAST,
                ItemPipeRouter.redirectedOutput(Direction.WEST, side -> side == Direction.WEST));
        // Falls back to the opposite face when no adjacent pipe is linked.
        assertEquals(
                Direction.EAST, ItemPipeRouter.redirectedOutput(Direction.WEST, side -> false));
        assertEquals(Direction.DOWN, ItemPipeRouter.redirectedOutput(Direction.UP, side -> false));
    }

    @Test
    void singleSegmentExtractionPrefersOppositeThenPerpendicularTargetContainer() {
        // Linked neighboring pipe still wins over an adjacent non-pipe target container.
        assertEquals(
                Direction.NORTH,
                ItemPipeRouter.redirectedOutput(
                        Direction.WEST,
                        side -> side == Direction.NORTH,
                        side -> side == Direction.EAST));
        // When no pipe neighbors exist, prefers the target directly opposite the extraction nozzle.
        assertEquals(
                Direction.EAST,
                ItemPipeRouter.redirectedOutput(
                        Direction.WEST,
                        side -> false,
                        side -> side == Direction.NORTH || side == Direction.EAST));
        // On a 90-degree single-segment bend, picks the perpendicular target instead of empty air.
        assertEquals(
                Direction.SOUTH,
                ItemPipeRouter.redirectedOutput(
                        Direction.WEST, side -> false, side -> side == Direction.SOUTH));
        // preferredTarget returns null when no non-extraction face matches.
        assertNull(
                ItemPipeRouter.preferredTarget(
                        side -> side == Direction.WEST, side -> side == Direction.WEST));
        assertEquals(
                Direction.SOUTH,
                ItemPipeRouter.preferredTarget(
                        side -> side == Direction.WEST || side == Direction.EAST,
                        side -> side == Direction.WEST || side == Direction.SOUTH));
    }

    @Test
    void longDistanceMixedItemPipelineRoutesPastDownstreamIncompatibleCargo() {
        // 64-block single-lane pipeline with a bend and vertical climb; downstream pipes hold a
        // different item in transit while the immediate next hop is free.
        var graph = new MockPipeGraph();
        for (int i = 0; i < 63; i++) {
            Direction step = i < 30 ? Direction.EAST : (i < 45 ? Direction.NORTH : Direction.UP);
            graph.link("p" + i, step, "p" + (i + 1));
        }
        graph.setReadySink("p63", true);
        graph.setIncompatible("p5", true);
        graph.setIncompatible("p22", true);
        graph.setIncompatible("p40", true);
        graph.setIncompatible("p63", true);

        // Pass-2 topology fallback advances smoothly behind downstream mixed items.
        assertEquals(Direction.EAST, ItemPipeRouter.route("p0", graph, 0));
        assertEquals(Direction.EAST, ItemPipeRouter.route("p1", graph, 1));
        // Immediate neighbor incompatible still blocks transfer until that neighbor clears,
        // while routePreview still preserves the true downstream axis so visuals never jump.
        assertNull(ItemPipeRouter.route("p4", graph, 2));
        assertEquals(Direction.EAST, ItemPipeRouter.routePreview("p4", graph, 2));
        // Full immediate neighbor in a back-to-back stream also keeps preview aligned to EAST.
        graph.setIncompatible("p5", false);
        graph.setFull("p5", true);
        assertNull(ItemPipeRouter.route("p4", graph, 2));
        assertEquals(Direction.EAST, ItemPipeRouter.routePreview("p4", graph, 2));
        // Once p5 clears, p4 advances immediately even while p22/p40/p63 still carry other items.
        graph.setFull("p5", false);
        assertEquals(Direction.EAST, ItemPipeRouter.route("p4", graph, 3));
    }

    @Test
    void coldLoadedCargoAdvancesWithoutLongMinValueOverflow() {
        assertTrue(TransportNode.itemReadyToAdvance(0L, Long.MIN_VALUE));
        assertTrue(TransportNode.itemReadyToAdvance(1_000L, Long.MIN_VALUE));
        assertFalse(TransportNode.itemReadyToAdvance(100L, 100L));
        assertFalse(
                TransportNode.itemReadyToAdvance(
                        100L + TransportNode.ITEM_TRANSIT_TICKS - 1L, 100L));
        assertTrue(TransportNode.itemReadyToAdvance(100L + TransportNode.ITEM_TRANSIT_TICKS, 100L));
    }

    @Test
    void biogasCellBelongsToUniversalCellFamily() {
        assertEquals("ic2:biogas", CellFamily.UNIVERSAL.namedLiquids().get("biogas_cell"));
        assertSame(CellFamily.UNIVERSAL, CellFamily.of("biogas_cell"));
        assertTrue(CellFamily.UNIVERSAL.admits("ic2:biogas"));
        assertFalse(CellFamily.BASIC.admits("ic2:biogas"));
        assertEquals("biogas_cell", CellFamily.UNIVERSAL.resultingPath("ic2:biogas", 1000));
        assertEquals("fluid_cell", CellFamily.UNIVERSAL.resultingPath("ic2:biogas", 500));
        assertFalse(CellFamily.BASIC.namedLiquids().containsKey("biogas_cell"));
    }

    @Test
    void verticalUpwardAndCornerTopologiesRouteAcrossImplicitTerminalSinks() {
        // 1. Straight UP
        var straightUp = new MockPipeGraph();
        straightUp.link("p0", Direction.UP, "p1");
        straightUp.link("p1", Direction.UP, "p2");
        straightUp.link("p2", Direction.UP, "p3");
        straightUp.setImplicitSink("p3", true);
        assertEquals(Direction.UP, ItemPipeRouter.route("p0", straightUp, 0));
        assertEquals(Direction.UP, ItemPipeRouter.route("p1", straightUp, 1));
        assertEquals(Direction.UP, ItemPipeRouter.route("p2", straightUp, 2));

        // 2. Horizontal -> UP -> Horizontal
        var climb = new MockPipeGraph();
        climb.link("p0", Direction.EAST, "p1");
        climb.link("p1", Direction.UP, "p2");
        climb.link("p2", Direction.UP, "p3");
        climb.link("p3", Direction.NORTH, "p4");
        climb.setImplicitSink("p4", true);
        assertEquals(Direction.EAST, ItemPipeRouter.route("p0", climb, 0));
        assertEquals(Direction.UP, ItemPipeRouter.route("p1", climb, 1));
        assertEquals(Direction.UP, ItemPipeRouter.route("p2", climb, 2));
        assertEquals(Direction.NORTH, ItemPipeRouter.route("p3", climb, 3));

        // 3. DOWN -> side -> UP
        var dipAndClimb = new MockPipeGraph();
        dipAndClimb.link("p0", Direction.DOWN, "p1");
        dipAndClimb.link("p1", Direction.SOUTH, "p2");
        dipAndClimb.link("p2", Direction.UP, "p3");
        dipAndClimb.link("p3", Direction.UP, "p4");
        dipAndClimb.setImplicitSink("p4", true);
        assertEquals(Direction.DOWN, ItemPipeRouter.route("p0", dipAndClimb, 0));
        assertEquals(Direction.SOUTH, ItemPipeRouter.route("p1", dipAndClimb, 1));
        assertEquals(Direction.UP, ItemPipeRouter.route("p2", dipAndClimb, 2));
        assertEquals(Direction.UP, ItemPipeRouter.route("p3", dipAndClimb, 3));

        // 4. L-turn, U-turn, Z-turn, and T-junction
        var uTurn = new MockPipeGraph();
        uTurn.link("p0", Direction.EAST, "p1");
        uTurn.link("p1", Direction.SOUTH, "p2");
        uTurn.link("p2", Direction.WEST, "p3");
        uTurn.setImplicitSink("p3", true);
        assertEquals(Direction.EAST, ItemPipeRouter.route("p0", uTurn, 0));
        assertEquals(Direction.SOUTH, ItemPipeRouter.route("p1", uTurn, 1));
        assertEquals(Direction.WEST, ItemPipeRouter.route("p2", uTurn, 2));

        var zTurn = new MockPipeGraph();
        zTurn.link("p0", Direction.EAST, "p1");
        zTurn.link("p1", Direction.NORTH, "p2");
        zTurn.link("p2", Direction.EAST, "p3");
        zTurn.setImplicitSink("p3", true);
        assertEquals(Direction.EAST, ItemPipeRouter.route("p0", zTurn, 0));
        assertEquals(Direction.NORTH, ItemPipeRouter.route("p1", zTurn, 1));
        assertEquals(Direction.EAST, ItemPipeRouter.route("p2", zTurn, 2));

        var tee = new MockPipeGraph();
        tee.link("p0", Direction.EAST, "junction");
        tee.link("junction", Direction.NORTH, "northEnd");
        tee.link("junction", Direction.SOUTH, "southEnd");
        tee.setImplicitSink("northEnd", true);
        tee.setImplicitSink("southEnd", true);
        assertEquals(Direction.EAST, ItemPipeRouter.route("p0", tee, 0));
        var firstBranch = ItemPipeRouter.route("junction", tee, 0);
        var secondBranch = ItemPipeRouter.route("junction", tee, 1);
        assertNotEquals(firstBranch, secondBranch);
        assertTrue(firstBranch == Direction.NORTH || firstBranch == Direction.SOUTH);
        assertTrue(secondBranch == Direction.NORTH || secondBranch == Direction.SOUTH);
    }

    @Test
    void explicitOutletPreservesDecoyIsolationEvenWhenFull() {
        // p0 --EAST--> p1 (decoy chest adjacent) --EAST--> p2 (explicit outlet)
        var graph = new MockPipeGraph();
        graph.link("p0", Direction.EAST, "p1");
        graph.link("p1", Direction.EAST, "p2");
        graph.setImplicitSink("p1", true);
        graph.setExplicitOutlet("p2", true);

        // Even while explicit sink p2 is full (isReadySink=false), decoy p1 must never receive
        // cargo.
        var blockedByFullExplicit = ItemPipeRouter.routeDecision("p0", graph, 0);
        assertNull(blockedByFullExplicit.hop());
        assertFalse(blockedByFullExplicit.hasSink());

        // When explicit sink p2 has room, routing goes past p1 straight toward p2.
        graph.setReadySink("p2", true);
        var active = ItemPipeRouter.routeDecision("p0", graph, 0);
        assertEquals(Direction.EAST, active.hop());
        assertTrue(active.hasSink());
        assertEquals(Direction.EAST, ItemPipeRouter.route("p1", graph, 1));
    }

    @Test
    void blockedActiveRouteDistinguishesBackpressureFromSinklessNetwork() {
        // Horizontal -> UP -> Horizontal where immediate neighbor is momentarily full (e.g. during
        // ITEM_TRANSIT_TICKS glide).
        var graph = new MockPipeGraph();
        graph.link("p0", Direction.EAST, "p1");
        graph.link("p1", Direction.UP, "p2");
        graph.link("p2", Direction.EAST, "p3");
        graph.setExplicitOutlet("p3", true);
        graph.setReadySink("p3", true);

        // Clear path
        var clear = ItemPipeRouter.routeDecision("p0", graph, 0);
        assertEquals(Direction.EAST, clear.hop());
        assertTrue(clear.hasSink());

        // Transiently full immediate neighbor: hop is null, but hasSink MUST stay true so caller
        // waits instead of kicking backward via sendToDirectedPipe.
        graph.setFull("p1", true);
        var blocked = ItemPipeRouter.routeDecision("p0", graph, 1);
        assertNull(blocked.hop());
        assertTrue(blocked.hasSink());
        assertEquals(Direction.EAST, ItemPipeRouter.routePreview("p0", graph, 1));

        // Sink itself full (no ready sink anywhere): hasSink becomes false.
        graph.setFull("p1", false);
        graph.setReadySink("p3", false);
        var noSink = ItemPipeRouter.routeDecision("p0", graph, 2);
        assertNull(noSink.hop());
        assertFalse(noSink.hasSink());
    }

    @Test
    void continuousMultiItemFlowAcrossUpwardAndCornerPipesNeverStallsOrKicksBack() {
        // Simulate 5-pipe horizontal -> UP -> corner line with backward-facing OUT on intermediate
        // and sink pipes, capacity=4, transit=ITEM_TRANSIT_TICKS, and adversarial tick order
        // (upstream ticks before downstream).
        String[] pipes = {"p0", "p1", "p2", "p3", "p4"};
        Direction[] forwardDirs = {Direction.EAST, Direction.UP, Direction.UP, Direction.NORTH};
        // Intentionally adversarial OUT values (pointing backward or into air!)
        Direction[] outDirs = {
            Direction.EAST, Direction.WEST, Direction.DOWN, Direction.DOWN, Direction.SOUTH
        };
        int[] cargo = new int[pipes.length];
        long[] arrived = new long[pipes.length];
        Direction[] cargoFrom = new Direction[pipes.length];
        java.util.Arrays.fill(arrived, Long.MIN_VALUE);

        int sourceItems = 32;
        int delivered = 0;
        int capacity = 4;

        for (long tick = 0; tick < 120 && delivered < 32; tick++) {
            // Tick upstream before downstream to maximize transient-fullness collisions.
            for (int i = 0; i < pipes.length; i++) {
                if (cargo[i] > 0 && TransportNode.itemReadyToAdvance(tick, arrived[i])) {
                    if (i == pipes.length - 1) {
                        delivered += cargo[i];
                        cargo[i] = 0;
                        cargoFrom[i] = null;
                    } else {
                        var graph = new MockPipeGraph();
                        for (int e = 0; e < forwardDirs.length; e++) {
                            graph.link(pipes[e], forwardDirs[e], pipes[e + 1]);
                        }
                        graph.setExplicitOutlet(pipes[pipes.length - 1], true);
                        graph.setReadySink(pipes[pipes.length - 1], true);
                        for (int k = 0; k < pipes.length; k++) {
                            if (cargo[k] >= capacity) graph.setFull(pipes[k], true);
                        }
                        var decision = ItemPipeRouter.routeDecision(pipes[i], graph, tick);
                        if (decision.hop() != null) {
                            assertEquals(
                                    forwardDirs[i],
                                    decision.hop(),
                                    "Pipe " + pipes[i] + " must route forward at tick " + tick);
                            int move = Math.min(cargo[i], capacity - cargo[i + 1]);
                            if (cargo[i + 1] == 0) {
                                arrived[i + 1] = tick;
                                cargoFrom[i + 1] = decision.hop().getOpposite();
                            }
                            cargo[i + 1] += move;
                            cargo[i] -= move;
                            if (cargo[i] == 0) cargoFrom[i] = null;
                        } else if (!decision.hasSink() && outDirs[i] != cargoFrom[i]) {
                            fail("Active route must never fall back to directed push at " + tick);
                        }
                    }
                }
                if (i == 0 && cargo[0] == 0 && sourceItems > 0) {
                    int take = Math.min(capacity, sourceItems);
                    cargo[0] = take;
                    sourceItems -= take;
                    arrived[0] = tick;
                    cargoFrom[0] = Direction.WEST;
                }
            }
        }
        assertEquals(0, sourceItems);
        assertEquals(32, delivered);
        for (int c : cargo) assertEquals(0, c);
    }

    @Test
    void multiExtractionRedirectAndInjectionOutputResolutionAvoidConflicts() {
        // Two extraction nozzles on WEST and EAST: installing on WEST must not fall back to EAST.
        Direction out =
                ItemPipeRouter.redirectedOutput(
                        Direction.WEST,
                        side -> side == Direction.WEST || side == Direction.EAST,
                        side -> false,
                        side -> false);
        assertNotEquals(Direction.WEST, out);
        assertNotEquals(Direction.EAST, out);

        // Injection nozzle installed on UP when OUT was pointing DOWN into upstream pipe -> aligns
        // to UP.
        assertEquals(
                Direction.UP,
                ItemPipeRouter.resolvedInjectionOutput(Direction.UP, Direction.DOWN, true, false));
        // Injection nozzle installed on UP on single segment where OUT pointed DOWN into source
        // container -> aligns to UP.
        assertEquals(
                Direction.UP,
                ItemPipeRouter.resolvedInjectionOutput(Direction.UP, Direction.DOWN, false, true));
        // Injection nozzle installed on WEST when OUT faces empty EAST air -> keeps EAST.
        assertEquals(
                Direction.EAST,
                ItemPipeRouter.resolvedInjectionOutput(
                        Direction.WEST, Direction.EAST, false, false));
    }
}
