package com.industrialage.ia2.energy.graph;

import com.industrialage.ia2.platform.CapabilityFacade.Face;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/** The effective energy topology of one world. Use it from the owning world thread. */
public final class EnergyGraph {
    private static final List<Face> FACES = List.of(Face.values());
    private final Map<Position, Node> nodes = new HashMap<>();

    /** Creates an empty topology. */
    public EnergyGraph() {}

    /** Sets the effective ports at a position and refreshes its direct connections. */
    public void put(Position position, Ports ports) {
        Node node = new Node(Objects.requireNonNull(position), Objects.requireNonNull(ports));
        nodes.put(position, node);
        updateNeighbors(position, node);
    }

    /** Removes a position and its direct connections, if present. */
    public void remove(Position position) {
        if (nodes.remove(Objects.requireNonNull(position)) != null) {
            updateNeighbors(position, null);
        }
    }

    /** Returns an immutable list of neighbors that can receive directly from a position. */
    public List<Position> outgoing(Position position) {
        Node node = nodes.get(Objects.requireNonNull(position));
        if (node == null) {
            return List.of();
        }
        List<Position> adjacent = new ArrayList<>(FACES.size());
        for (Node neighbor : node.outgoing) {
            if (neighbor != null) {
                adjacent.add(neighbor.position);
            }
        }
        return List.copyOf(adjacent);
    }

    /** A receiving endpoint and the unrounded loss of its cheapest connected path, in EU. */
    public record Route(Position sink, double rawLoss, List<Position> conductors) {
        /**
         * Captures the exact conductor sequence used to rate this route, in source-to-sink order.
         */
        public Route {
            conductors = List.copyOf(conductors);
        }
    }

    /**
     * Finds every reachable receiver through loaded conductors, retaining the least-loss path to
     * each receiver. Source and sink contacts add 0.002 EU together, including direct connections.
     * A device can terminate a route but cannot carry energy through to another device.
     */
    public List<Route> routes(Position source, ToDoubleFunction<Position> cableLoss) {
        Node start = nodes.get(source);
        if (start == null || start.ports.conductor()) {
            return List.of();
        }
        record Visit(Node node, double loss) {}
        Comparator<Visit> order =
                Comparator.comparingDouble(Visit::loss)
                        .thenComparingInt(v -> v.node.position.x)
                        .thenComparingInt(v -> v.node.position.y)
                        .thenComparingInt(v -> v.node.position.z);
        var frontier = new PriorityQueue<>(order);
        Map<Node, Double> best = new IdentityHashMap<>();
        Map<Node, Node> predecessors = new IdentityHashMap<>();
        List<Route> routes = new ArrayList<>();
        best.put(start, 0.0);
        frontier.add(new Visit(start, 0.0));
        while (!frontier.isEmpty()) {
            Visit current = frontier.remove();
            if (current.loss() > best.get(current.node())) {
                continue;
            }
            if (current.node() != start && !current.node().ports.conductor()) {
                List<Position> path = path(start, current.node(), predecessors);
                routes.add(
                        new Route(
                                current.node().position,
                                current.loss() + 0.002,
                                path.subList(1, path.size() - 1)));
                continue;
            }
            for (Node neighbor : current.node().outgoing) {
                if (neighbor == null || neighbor == start) {
                    continue;
                }
                double segment =
                        neighbor.ports.conductor() ? cableLoss.applyAsDouble(neighbor.position) : 0;
                if (!Double.isFinite(segment) || segment < 0) {
                    throw new IllegalArgumentException(
                            "Invalid cable loss at " + neighbor.position);
                }
                double next = current.loss() + segment;
                if (next < best.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    best.put(neighbor, next);
                    predecessors.put(neighbor, current.node());
                    frontier.add(new Visit(neighbor, next));
                }
            }
        }
        return List.copyOf(routes);
    }

    /**
     * Finds a candidate path between distinct nonconducting endpoints, through conductors. The
     * returned path is immutable and includes both endpoints. Returns empty for invalid or
     * unreachable endpoints.
     */
    public Optional<List<Position>> findPath(Position source, Position sink) {
        Node start = nodes.get(Objects.requireNonNull(source));
        Node end = nodes.get(Objects.requireNonNull(sink));
        if (start == null
                || end == null
                || start == end
                || start.ports.conductor()
                || end.ports.conductor()) {
            return Optional.empty();
        }

        Queue<Node> pending = new ArrayDeque<>();
        Map<Node, Node> predecessors = new IdentityHashMap<>();
        predecessors.put(start, start);
        pending.add(start);
        while (!pending.isEmpty()) {
            Node current = pending.remove();
            for (Node neighbor : current.outgoing) {
                if (neighbor == null || (neighbor != end && !neighbor.ports.conductor())) {
                    continue;
                }
                if (predecessors.putIfAbsent(neighbor, current) != null) {
                    continue;
                }
                if (neighbor == end) {
                    return Optional.of(path(start, end, predecessors));
                }
                pending.add(neighbor);
            }
        }
        return Optional.empty();
    }

    private void updateNeighbors(Position position, Node node) {
        for (Face face : FACES) {
            Node neighbor = nodes.get(position.adjacent(face));
            if (neighbor == null) {
                continue;
            }
            Face opposite = opposite(face);
            if (node != null) {
                node.outgoing[face.ordinal()] =
                        connects(node, face, neighbor, opposite) ? neighbor : null;
            }
            neighbor.outgoing[opposite.ordinal()] =
                    node != null && connects(neighbor, opposite, node, face) ? node : null;
        }
    }

    private static boolean connects(Node from, Face out, Node to, Face in) {
        int sourceColor = from.ports.color();
        int targetColor = to.ports.color();
        return (sourceColor == 0 || targetColor == 0 || sourceColor == targetColor)
                && from.ports.outputs().contains(out)
                && to.ports.inputs().contains(in);
    }

    private static List<Position> path(Node start, Node end, Map<Node, Node> predecessors) {
        List<Position> result = new ArrayList<>();
        for (Node node = end; node != start; node = predecessors.get(node)) {
            result.add(node.position);
        }
        result.add(start.position);
        Collections.reverse(result);
        return List.copyOf(result);
    }

    private static Face opposite(Face face) {
        return switch (face) {
            case DOWN -> Face.UP;
            case UP -> Face.DOWN;
            case NORTH -> Face.SOUTH;
            case SOUTH -> Face.NORTH;
            case WEST -> Face.EAST;
            case EAST -> Face.WEST;
        };
    }

    /** A block position within this graph's world. */
    public record Position(int x, int y, int z) {
        private Position adjacent(Face face) {
            return switch (face) {
                case DOWN -> y == Integer.MIN_VALUE ? null : new Position(x, y - 1, z);
                case UP -> y == Integer.MAX_VALUE ? null : new Position(x, y + 1, z);
                case NORTH -> z == Integer.MIN_VALUE ? null : new Position(x, y, z - 1);
                case SOUTH -> z == Integer.MAX_VALUE ? null : new Position(x, y, z + 1);
                case WEST -> x == Integer.MIN_VALUE ? null : new Position(x - 1, y, z);
                case EAST -> x == Integer.MAX_VALUE ? null : new Position(x + 1, y, z);
            };
        }
    }

    /**
     * Effective energy ports of one block.
     *
     * @param inputs faces that accept energy
     * @param outputs faces that emit energy
     * @param conductor whether paths can pass through this block between its ports
     * @param color conductor dye, or zero for uncolored connections
     */
    public record Ports(Set<Face> inputs, Set<Face> outputs, boolean conductor, int color) {
        /** Keeps undyed conductors and device ports compatible with every dye. */
        public Ports(Set<Face> inputs, Set<Face> outputs, boolean conductor) {
            this(inputs, outputs, conductor, 0);
        }

        /** Copies the face sets so connections change only when the topology is updated. */
        public Ports {
            inputs = Set.copyOf(inputs);
            outputs = Set.copyOf(outputs);
        }
    }

    private static final class Node {
        private final Position position;
        private final Ports ports;
        private final Node[] outgoing = new Node[FACES.size()];

        private Node(Position position, Ports ports) {
            this.position = position;
            this.ports = ports;
        }
    }
}
