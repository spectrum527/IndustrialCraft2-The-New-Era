package com.industrialage.ia2.energy.graph;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.energy.graph.EnergyGraph.Ports;
import com.industrialage.ia2.energy.graph.EnergyGraph.Position;
import com.industrialage.ia2.platform.CapabilityFacade.Face;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Contracts from energy/network.md sections 2 and 3. */
class EnergyGraphContractTest {
    private static final Position ORIGIN = new Position(0, 0, 0);
    private static final Set<Face> NONE = Set.of();

    private static Ports ports(Set<Face> inputs, Set<Face> outputs, boolean conductor) {
        return new Ports(inputs, outputs, conductor);
    }

    @Test
    void eachOfSixAdjacentFacesRequiresBothDirectionalPorts() {
        Face[] outward = {Face.EAST, Face.WEST, Face.UP, Face.DOWN, Face.SOUTH, Face.NORTH};
        Face[] inward = {Face.WEST, Face.EAST, Face.DOWN, Face.UP, Face.NORTH, Face.SOUTH};
        Position[] neighbors = {
            new Position(1, 0, 0), new Position(-1, 0, 0),
            new Position(0, 1, 0), new Position(0, -1, 0),
            new Position(0, 0, 1), new Position(0, 0, -1)
        };
        for (int i = 0; i < outward.length; i++) {
            var graph = new EnergyGraph();
            var neighbor = neighbors[i];
            graph.put(ORIGIN, ports(NONE, Set.of(outward[i]), false));
            graph.put(neighbor, ports(Set.of(inward[i]), NONE, false));
            assertEquals(List.of(ORIGIN, neighbor), graph.findPath(ORIGIN, neighbor).orElseThrow());
            assertTrue(graph.findPath(neighbor, ORIGIN).isEmpty(), outward[i].name());

            graph.put(neighbor, ports(Set.of(outward[i]), NONE, false));
            assertTrue(graph.findPath(ORIGIN, neighbor).isEmpty(), outward[i].name());
            graph.put(neighbor, ports(Set.of(inward[i]), NONE, false));
            graph.put(ORIGIN, ports(NONE, Set.of(inward[i]), false));
            assertTrue(graph.findPath(ORIGIN, neighbor).isEmpty(), outward[i].name());
        }
    }

    @Test
    void endpointsCannotRelayAndRemovingConductorBreaksThenRestoresPath() {
        var graph = new EnergyGraph();
        var middle = new Position(1, 0, 0);
        var sink = new Position(2, 0, 0);
        graph.put(ORIGIN, ports(NONE, Set.of(Face.EAST), false));
        graph.put(sink, ports(Set.of(Face.WEST), NONE, false));
        var bothWays = Set.of(Face.WEST, Face.EAST);
        graph.put(middle, ports(bothWays, bothWays, false));
        assertTrue(graph.findPath(ORIGIN, sink).isEmpty());
        assertEquals(List.of(ORIGIN, middle), graph.findPath(ORIGIN, middle).orElseThrow());
        assertEquals(List.of(middle, sink), graph.findPath(middle, sink).orElseThrow());

        graph.put(middle, ports(Set.of(Face.EAST), Set.of(Face.EAST), true));
        assertTrue(graph.findPath(ORIGIN, sink).isEmpty());
        graph.put(middle, ports(Set.of(Face.WEST), Set.of(Face.WEST), true));
        assertTrue(graph.findPath(ORIGIN, sink).isEmpty());
        graph.put(middle, ports(Set.of(Face.WEST), Set.of(Face.EAST), true));
        assertEquals(List.of(ORIGIN, middle, sink), graph.findPath(ORIGIN, sink).orElseThrow());
        graph.remove(middle);
        assertTrue(graph.findPath(ORIGIN, sink).isEmpty());
        graph.put(middle, ports(bothWays, bothWays, true));
        assertEquals(List.of(ORIGIN, middle, sink), graph.findPath(ORIGIN, sink).orElseThrow());
        graph.remove(ORIGIN);
        assertTrue(graph.findPath(ORIGIN, sink).isEmpty());
        graph.put(ORIGIN, ports(NONE, Set.of(Face.EAST), false));
        graph.remove(sink);
        assertTrue(graph.findPath(ORIGIN, sink).isEmpty());
    }

    @Test
    void identicalCoordinatesInDifferentWorldGraphsStayIndependent() {
        var worldA = new EnergyGraph();
        var worldB = new EnergyGraph();
        var neighbor = new Position(1, 0, 0);
        worldA.put(ORIGIN, ports(NONE, Set.of(Face.EAST), false));
        worldB.put(neighbor, ports(Set.of(Face.WEST), NONE, false));
        assertTrue(worldA.findPath(ORIGIN, neighbor).isEmpty());
        assertTrue(worldB.findPath(ORIGIN, neighbor).isEmpty());
        worldA.put(neighbor, ports(Set.of(Face.WEST), NONE, false));
        assertEquals(List.of(ORIGIN, neighbor), worldA.findPath(ORIGIN, neighbor).orElseThrow());
        assertTrue(worldB.findPath(ORIGIN, neighbor).isEmpty());
        worldB.put(ORIGIN, ports(NONE, Set.of(Face.EAST), false));
        worldA.remove(neighbor);
        assertTrue(worldA.findPath(ORIGIN, neighbor).isEmpty());
        assertEquals(List.of(ORIGIN, neighbor), worldB.findPath(ORIGIN, neighbor).orElseThrow());
    }

    @Test
    void conductorCycleReroutesUntilAllPathsAreBroken() {
        var graph = new EnergyGraph();
        var start = new Position(1, 0, 0);
        var direct = new Position(2, 0, 0);
        var end = new Position(3, 0, 0);
        var lowerLeft = new Position(1, 0, 1);
        var lowerMiddle = new Position(2, 0, 1);
        var lowerRight = new Position(3, 0, 1);
        var sink = new Position(4, 0, 0);
        var allFaces = Set.of(Face.values());
        var wire = ports(allFaces, allFaces, true);
        graph.put(ORIGIN, ports(NONE, Set.of(Face.EAST), false));
        for (var position : List.of(start, direct, end, lowerLeft, lowerMiddle, lowerRight)) {
            graph.put(position, wire);
        }
        graph.put(sink, ports(Set.of(Face.WEST), NONE, false));
        assertTrue(graph.findPath(ORIGIN, sink).isPresent());
        assertThrows(UnsupportedOperationException.class, () -> graph.outgoing(start).add(sink));
        assertTrue(graph.outgoing(start).contains(direct));
        graph.remove(direct);
        var remaining = graph.findPath(ORIGIN, sink).orElseThrow();
        assertTrue(remaining.contains(lowerMiddle));
        graph.remove(lowerMiddle);
        assertTrue(graph.findPath(ORIGIN, sink).isEmpty());
    }

    @Test
    void packetSizesAndSourceBudgetsHonorLevelAndOfferBoundaries() {
        int[] tiers = {0, 1, 2, 3, 4, 5, 13, 14, 29, 30, 31};
        double[] sizes = {
            8,
            32,
            128,
            512,
            2048,
            8192,
            536870912,
            2147483648.0,
            Math.scalb(1.0, 61),
            Math.scalb(1.0, 63),
            Math.scalb(1.0, 63)
        };
        for (int i = 0; i < tiers.length; i++) {
            assertEquals(sizes[i], PacketBudget.packetSize(tiers[i]), 0.0, "tier " + tiers[i]);
        }
        assertThrows(IllegalArgumentException.class, () -> PacketBudget.packetSize(-1));
        assertEquals(32.0, PacketBudget.sourceBudget(1, 100, 1).eu(), 0.0);
        assertEquals(32.0, PacketBudget.sourceBudget(1, 33, 1).eu(), 0.0);
        assertEquals(33.0, PacketBudget.sourceBudget(2, 33, 1).eu(), 0.0);
        assertEquals(100.0, PacketBudget.sourceBudget(1, 100, 4).eu(), 0.0);
        assertEquals(10.0, PacketBudget.sourceBudget(1, 10, 1).eu(), 0.0);
        assertEquals(32.0, PacketBudget.sourceBudget(1, Double.POSITIVE_INFINITY, 1).eu(), 0.0);
        assertEquals(0.0, PacketBudget.sourceBudget(1, 0, 1).eu(), 0.0);
        assertEquals(0.0, PacketBudget.sourceBudget(1, -1, 1).eu(), 0.0);
        assertEquals(0.0, PacketBudget.sourceBudget(1, 100, 0).eu(), 0.0);
        assertEquals(0.0, PacketBudget.sourceBudget(1, 100, -1).eu(), 0.0);
        assertEquals(0.0, PacketBudget.sourceBudget(1, Double.NaN, 1).eu(), 0.0);
        assertEquals(0.0, PacketBudget.sourceBudget(-1, 100, 1).eu(), 0.0);
        assertTrue(PacketBudget.sourceBudget(-1, 100, 1).invalidTier());
        assertFalse(PacketBudget.sourceBudget(1, Double.NaN, 1).invalidTier());
    }
}
