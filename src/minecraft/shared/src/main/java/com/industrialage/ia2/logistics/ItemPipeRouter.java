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
 * <p>Uses a two-tier sink rule: when any pipe in the connected network has an {@link
 * TransportNode.Nozzle#INJECTION} nozzle or an {@link TransportBlock#OUT}-aligned non-pipe
 * container (even if currently full), only those explicit outlets act as sinks, preserving decoy
 * isolation and backpressure. Only when the connected network has no explicit outlets or injection
 * nozzles do terminal ({@code linkedPipes <= 1}) pipes auto-route to eligible adjacent containers.
 * Intermediate connected pipes forward cargo toward the nearest ready sink across bends and
 * vertical climbs.
 */
final class ItemPipeRouter {
    private static final int MAX_NETWORK_NODES = 4096;

    private ItemPipeRouter() {}

    record Edge<N>(N pipe, Direction side) {}

    /**
     * Routing decision distinguishing an active blocked route, a full-sink backpressured network,
     * and a truly sinkless directed network.
     */
    record RouteResult(Direction hop, boolean hasSink, boolean hasConfiguredOutlet) {
        static final RouteResult NO_SINK = new RouteResult(null, false, false);
        static final RouteResult BACKPRESSURED = new RouteResult(null, false, true);
        static final RouteResult BLOCKED = new RouteResult(null, true, true);

        /** Whether the caller may fall back to a sinkless directed push along {@code OUT}. */
        boolean allowDirectedFallback() {
            return !hasSink && !hasConfiguredOutlet;
        }
    }

    interface Graph<N> {
        List<Edge<N>> links(N node);

        boolean isReadySink(N node);

        boolean isCompatible(N node);

        boolean canReceive(N node, Direction entryFace);

        /**
         * Whether {@code node} has any explicitly configured outlet (an injection nozzle or an
         * {@code OUT}-aligned non-pipe container, even if currently full).
         */
        default boolean hasExplicitOutlet(N node) {
            return false;
        }

        /**
         * Whether {@code node} can serve as an implicit auto-routed sink when no pipe in the
         * connected network has an explicitly configured outlet.
         */
        default boolean isReadyImplicitSink(N node) {
            return false;
        }

        /**
         * Whether {@code node} has any eligible implicit auto-routed outlet (even if currently
         * full), used to hold backpressure instead of falling back to cyclic directed pushes.
         */
        default boolean hasImplicitOutlet(N node) {
            return isReadyImplicitSink(node);
        }
    }

    /**
     * Finds the next connected pipe face from {@code source} along a shortest clear path to a
     * downstream exit that can currently accept {@code cargo}, or {@code null} if no ready sink
     * exists in the loaded connected pipe network.
     */
    static Direction nextHop(ServerLevel world, TransportNode source, ItemStack cargo, long time) {
        return decideHop(world, source, cargo, time).hop();
    }

    /**
     * Computes both the next hop and whether any ready downstream sink exists in the connected pipe
     * network so callers do not fall back to directed pushes during transient backpressure.
     */
    static RouteResult decideHop(
            ServerLevel world, TransportNode source, ItemStack cargo, long time) {
        if (cargo.isEmpty() || source.spec.kind() != Kind.ITEM_PIPE) return RouteResult.NO_SINK;
        return routeDecision(source, worldGraph(world, source, cargo), time);
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

    /**
     * Whether any loaded item pipe in the connected network containing {@code start} has an
     * injection nozzle or an {@code OUT}-aligned non-pipe container.
     */
    static boolean networkHasExplicitOutlet(ServerLevel world, TransportNode start) {
        if (start.spec.kind() != Kind.ITEM_PIPE) return false;
        var nodes = new ArrayList<TransportNode>();
        var seen = new java.util.HashSet<TransportNode>();
        nodes.add(start);
        seen.add(start);
        for (int index = 0; index < nodes.size(); index++) {
            var pipe = nodes.get(index);
            if (pipe.injectionNozzles != 0 || pipe.hasConfiguredExplicitOutlet(world)) {
                return true;
            }
            for (var side : TransportNode.SIDES) {
                var pos = pipe.getBlockPos().relative(side);
                if (!world.hasChunkAt(pos)) continue;
                var other = world.getBlockEntity(pos);
                if (TransportNode.isPipe(other)) {
                    var next = (TransportNode) other;
                    if (pipe.linksTo(side, next)
                            && nodes.size() < MAX_NETWORK_NODES
                            && seen.add(next)) {
                        nodes.add(next);
                    }
                }
            }
        }
        return false;
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
                return pipe.hasReadyExplicitOutlet(world, cargo);
            }

            @Override
            public boolean hasExplicitOutlet(TransportNode pipe) {
                return pipe.injectionNozzles != 0 || pipe.hasConfiguredExplicitOutlet(world);
            }

            @Override
            public boolean isReadyImplicitSink(TransportNode pipe) {
                return pipe.canUseImplicitOutlet(world)
                        && pipe.hasReadyImplicitOutlet(world, cargo);
            }

            @Override
            public boolean hasImplicitOutlet(TransportNode pipe) {
                return pipe.canUseImplicitOutlet(world) && pipe.hasConfiguredImplicitOutlet(world);
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
        return routeDecision(source, graph, time).hop();
    }

    /**
     * Computes the next hop together with whether the connected graph currently contains a ready
     * sink, distinguishing transient blockage from a sinkless directed network.
     */
    static <N> RouteResult routeDecision(N source, Graph<N> graph, long time) {
        return routeInternal(source, graph, time, true);
    }

    /**
     * Computes the shortest-path preview direction on an abstract graph, falling back to topology
     * distance when the immediate neighbor is transiently full or occupied by another item.
     */
    static <N> Direction routePreview(N source, Graph<N> graph, long time) {
        return routeInternal(source, graph, time, false).hop();
    }

    private static <N> RouteResult routeInternal(
            N source, Graph<N> graph, long time, boolean requireImmediateReceive) {
        var nodes = new ArrayList<N>();
        var visited = new HashMap<N, List<Edge<N>>>();
        var explicitSinks = new ArrayList<N>();
        var implicitSinks = new ArrayList<N>();
        boolean anyExplicitOutlet = false;
        boolean anyImplicitOutlet = false;

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
            if (graph.hasExplicitOutlet(pipe)) {
                anyExplicitOutlet = true;
            } else if (!anyExplicitOutlet && graph.hasImplicitOutlet(pipe)) {
                anyImplicitOutlet = true;
            }
            if (!pipe.equals(source)) {
                if (graph.isReadySink(pipe)) {
                    explicitSinks.add(pipe);
                } else if (!anyExplicitOutlet && graph.isReadyImplicitSink(pipe)) {
                    implicitSinks.add(pipe);
                }
            }
        }

        List<N> sinks =
                !explicitSinks.isEmpty()
                        ? explicitSinks
                        : (anyExplicitOutlet ? List.of() : implicitSinks);
        if (sinks.isEmpty()) {
            return (anyExplicitOutlet || anyImplicitOutlet)
                    ? RouteResult.BACKPRESSURED
                    : RouteResult.NO_SINK;
        }

        Direction clearHop = routePass(source, graph, visited, sinks, time, true, true);
        if (clearHop != null) return new RouteResult(clearHop, true, true);
        Direction queuedHop = routePass(source, graph, visited, sinks, time, false, true);
        if (queuedHop != null) return new RouteResult(queuedHop, true, true);
        if (requireImmediateReceive) return RouteResult.BLOCKED;
        return new RouteResult(
                routePass(source, graph, visited, sinks, time, false, false), true, true);
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

        var bestLinks = new ArrayList<Direction>();
        for (var link : visited.get(source)) {
            int dist = distance.getOrDefault(link.pipe(), Integer.MAX_VALUE);
            if (dist < bestSourceDist
                    && (!requireImmediateReceive
                            || graph.canReceive(link.pipe(), link.side().getOpposite()))) {
                bestLinks.add(link.side());
            }
        }
        return bestLinks.isEmpty() ? null : bestLinks.get(Math.floorMod(time, bestLinks.size()));
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
     * (checking the opposite face first in each category), and falling back to a non-extraction
     * face (preferring the opposite face).
     */
    static Direction redirectedOutput(
            Direction extractionFace,
            java.util.function.Predicate<Direction> linksToPipe,
            java.util.function.Predicate<Direction> linksToTarget) {
        return redirectedOutput(
                extractionFace, side -> side == extractionFace, linksToPipe, linksToTarget);
    }

    /**
     * Picks an output face excluding all currently installed extraction nozzles, preferring a
     * linked neighboring item pipe, then an adjacent non-pipe target container, then the opposite
     * face if free of an extraction nozzle, then any remaining non-extraction face.
     */
    static Direction redirectedOutput(
            Direction extractionFace,
            java.util.function.Predicate<Direction> isExtraction,
            java.util.function.Predicate<Direction> linksToPipe,
            java.util.function.Predicate<Direction> linksToTarget) {
        Direction pipe = preferredTarget(isExtraction, linksToPipe);
        if (pipe != null) return pipe;
        Direction target = preferredTarget(isExtraction, linksToTarget);
        if (target != null) return target;
        Direction opposite = extractionFace.getOpposite();
        if (!isExtraction.test(opposite)) return opposite;
        for (var candidate : Direction.values()) {
            if (!isExtraction.test(candidate)) return candidate;
        }
        return opposite;
    }

    /**
     * Resolves whether installing an injection nozzle on {@code injectionFace} should align {@code
     * OUT} to {@code injectionFace} to prevent backward directed pushes into a linked upstream pipe
     * or loopback delivery into the opposite source container.
     */
    static Direction resolvedInjectionOutput(
            Direction injectionFace,
            Direction currentOut,
            boolean outLinksToPipe,
            boolean outIsOppositeTarget) {
        if (currentOut != injectionFace
                && (outLinksToPipe
                        || (currentOut == injectionFace.getOpposite() && outIsOppositeTarget))) {
            return injectionFace;
        }
        return currentOut;
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
