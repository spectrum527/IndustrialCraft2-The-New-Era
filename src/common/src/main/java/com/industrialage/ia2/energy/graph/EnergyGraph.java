package com.industrialage.ia2.energy.graph;

import com.industrialage.ia2.platform.CapabilityFacade.Face;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

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
        return from.ports.outputs().contains(out) && to.ports.inputs().contains(in);
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
     */
    public record Ports(Set<Face> inputs, Set<Face> outputs, boolean conductor) {
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
