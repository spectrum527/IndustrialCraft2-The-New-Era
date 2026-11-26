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
}
