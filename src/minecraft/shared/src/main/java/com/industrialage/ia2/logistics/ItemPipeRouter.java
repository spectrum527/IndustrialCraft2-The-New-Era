package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec.Kind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Shortest-path router across a connected network of loaded item pipes.
 *
 * <p>Preserves explicit outlet rules: only faces configured as {@link
 * TransportNode.Nozzle#INJECTION} or unmodified {@link TransportBlock#OUT} faces facing a non-pipe
 * container that can accept the cargo act as sinks. Intermediate connected pipes forward cargo
 * toward the nearest ready sink even across bends or unrotated segments.
 */
final class ItemPipeRouter {
    private static final int MAX_NETWORK_NODES = 4096;

    private ItemPipeRouter() {}

    record Edge<N>(N pipe, Direction side) {}

    interface Graph<N> {
        List<Edge<N>> links(N node);

        boolean isReadySink(N node);

        boolean isCompatible(N node);

        boolean canReceive(N node, Direction entryFace);
    }

    /**
     * Finds the next connected pipe face from {@code source} along a shortest clear path to a
     * downstream exit that can currently accept {@code cargo}, or {@code null} if no ready sink
     * exists in the loaded connected pipe network.
     */
    static Direction nextHop(ServerLevel world, TransportNode source, ItemStack cargo, long time) {
        if (cargo.isEmpty() || source.spec.kind() != Kind.ITEM_PIPE) return null;
        return route(source, worldGraph(world, source, cargo), time);
    }

    /**
     * Finds the visual travel direction from {@code source} toward the nearest ready downstream
     * sink even while the immediately adjacent pipe is momentarily full or carrying another item.
     */
    static Direction previewHop(
            ServerLevel world, TransportNode source, ItemStack cargo, long time) {
        if (cargo.isEmpty() || source.spec.kind() != Kind.ITEM_PIPE) return null;
        return routePreview(source, worldGraph(world, source, cargo), time);
    }

    private static Graph<TransportNode> worldGraph(
            ServerLevel world, TransportNode source, ItemStack cargo) {
        return new Graph<>() {
            @Override
            public List<Edge<TransportNode>> links(TransportNode pipe) {
                var result = new ArrayList<Edge<TransportNode>>();
                for (var side : TransportNode.SIDES) {
                    var pos = pipe.getBlockPos().relative(side);
                    if (!world.hasChunkAt(pos)) continue;
                    var other = world.getBlockEntity(pos);
                    if (TransportNode.isPipe(other)) {
                        var next = (TransportNode) other;
                        if (pipe.linksTo(side, next)) {
                            result.add(new Edge<>(next, side));
                        }
                    }
                }
                return result;
            }

            @Override
            public boolean isReadySink(TransportNode pipe) {
                var output = pipe.getBlockState().getValue(TransportBlock.OUT);
                for (var side : TransportNode.SIDES) {
                    var pos = pipe.getBlockPos().relative(side);
                    if (!world.hasChunkAt(pos)) continue;
                    var other = world.getBlockEntity(pos);
                    if (TransportNode.isPipe(other)) continue;
                    var face = side.getOpposite();
                    var mode = pipe.nozzle(side);
                    if ((mode == TransportNode.Nozzle.INJECTION
                                    || (mode == TransportNode.Nozzle.NONE && side == output))
                            && TransportNode.machineInput(other, face)
                            && TransportPlatform.canAcceptItem(world, pos, face, cargo)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean isCompatible(TransportNode candidate) {
                return compatible(source, candidate, cargo);
            }

            @Override
            public boolean canReceive(TransportNode candidate, Direction entryFace) {
                return candidate.canAcceptRoutedPipeItem(entryFace, cargo);
            }
        };
    }

    /** Computes the next hop on an abstract connected pipe graph for unit testing and runtime. */
    static <N> Direction route(N source, Graph<N> graph, long time) {
        return routeInternal(source, graph, time, true);
    }

    /**
     * Computes the shortest-path preview direction on an abstract graph, falling back to topology
     * distance when the immediate neighbor is transiently full or occupied by another item.
     */
    static <N> Direction routePreview(N source, Graph<N> graph, long time) {
        return routeInternal(source, graph, time, false);
    }

    private static <N> Direction routeInternal(
            N source, Graph<N> graph, long time, boolean requireImmediateReceive) {
        var nodes = new ArrayList<N>();
        var visited = new HashMap<N, List<Edge<N>>>();
        var sinks = new ArrayList<N>();

        nodes.add(source);
        visited.put(source, new ArrayList<>());

        for (int index = 0; index < nodes.size(); index++) {
            var pipe = nodes.get(index);
            var links = visited.get(pipe);
            for (var edge : graph.links(pipe)) {
                var next = edge.pipe();
                if (!visited.containsKey(next)) {
                    if (nodes.size() >= MAX_NETWORK_NODES) continue;
                    visited.put(next, new ArrayList<>());
                    nodes.add(next);
                }
                links.add(edge);
            }
            if (!pipe.equals(source) && graph.isReadySink(pipe)) {
                sinks.add(pipe);
            }
        }

        if (sinks.isEmpty()) return null;

        Direction clearHop = routePass(source, graph, visited, sinks, time, true, true);
        if (clearHop != null) return clearHop;
        Direction queuedHop = routePass(source, graph, visited, sinks, time, false, true);
        if (queuedHop != null || requireImmediateReceive) return queuedHop;
        return routePass(source, graph, visited, sinks, time, false, false);
    }

    private static <N> Direction routePass(
            N source,
            Graph<N> graph,
            Map<N, List<Edge<N>>> visited,
            List<N> sinks,
            long time,
            boolean requireCompatibleIntermediates,
            boolean requireImmediateReceive) {
        Map<N, Integer> distance = new HashMap<>();
        var queue = new ArrayDeque<N>();
        for (var sink : sinks) {
            if ((!requireCompatibleIntermediates || graph.isCompatible(sink))
                    && distance.putIfAbsent(sink, 0) == null) {
                queue.add(sink);
            }
        }
        int bestSourceDist = Integer.MAX_VALUE;
        while (!queue.isEmpty()) {
            var current = queue.remove();
            int nextDist = distance.get(current) + 1;
            for (var link : visited.get(current)) {
                var neighbor = link.pipe();
                if (neighbor.equals(source)) {
                    if (!requireImmediateReceive || graph.canReceive(current, link.side())) {
                        bestSourceDist = Math.min(bestSourceDist, nextDist);
                    }
                    continue;
                }
                if ((!requireCompatibleIntermediates || graph.isCompatible(neighbor))
                        && distance.putIfAbsent(neighbor, nextDist) == null) {
                    queue.add(neighbor);
                }
            }
        }

        if (bestSourceDist == Integer.MAX_VALUE) return null;

        var sourceLinks = visited.get(source);
        int count = sourceLinks.size();
        for (int i = 0; i < count; i++) {
            var link = sourceLinks.get(Math.floorMod(time + i, count));
            int dist = distance.getOrDefault(link.pipe(), Integer.MAX_VALUE);
            if (dist < bestSourceDist
                    && (!requireImmediateReceive
                            || graph.canReceive(link.pipe(), link.side().getOpposite()))) {
                return link.side();
            }
        }
        return null;
    }

    /**
     * Picks a non-conflicting output face when an extraction nozzle is installed on {@code OUT}.
     */
    static Direction redirectedOutput(
            Direction extractionFace, java.util.function.Predicate<Direction> linksToPipe) {
        return redirectedOutput(extractionFace, linksToPipe, side -> false);
    }

    /**
     * Picks an output face when an extraction nozzle is installed on {@code extractionFace},
     * preferring a linked neighboring item pipe, then an adjacent non-pipe target container
     * (checking the opposite face first in each category), and falling back to the opposite face.
     */
    static Direction redirectedOutput(
            Direction extractionFace,
            java.util.function.Predicate<Direction> linksToPipe,
            java.util.function.Predicate<Direction> linksToTarget) {
        Direction pipe = preferredTarget(side -> side == extractionFace, linksToPipe);
        if (pipe != null) return pipe;
        Direction target = preferredTarget(side -> side == extractionFace, linksToTarget);
        if (target != null) return target;
        return extractionFace.getOpposite();
    }

    /**
     * Picks an adjacent candidate face for an item pipe with extraction nozzle(s), preferring faces
     * directly opposite an extraction nozzle before perpendicular faces, or {@code null} if none.
     */
    static Direction preferredTarget(
            java.util.function.Predicate<Direction> isExtraction,
            java.util.function.Predicate<Direction> matchesTarget) {
        for (var candidate : Direction.values()) {
            if (isExtraction.test(candidate)) {
                Direction opposite = candidate.getOpposite();
                if (!isExtraction.test(opposite) && matchesTarget.test(opposite)) {
                    return opposite;
                }
            }
        }
        for (var candidate : Direction.values()) {
            if (!isExtraction.test(candidate) && matchesTarget.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean compatible(
            TransportNode source, TransportNode candidate, ItemStack cargo) {
        return candidate == source
                || candidate.cargo.isEmpty()
                || TransportPlatform.sameItem(candidate.cargo, cargo);
    }
}
