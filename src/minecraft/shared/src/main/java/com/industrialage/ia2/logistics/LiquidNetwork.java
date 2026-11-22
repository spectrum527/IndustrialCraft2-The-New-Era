package com.industrialage.ia2.logistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * Routes one tick of a connected set of loaded fluid pipes. All reservations are world-thread
 * local.
 */
final class LiquidNetwork {
    private LiquidNetwork() {}

    private record Link(TransportNode pipe, Direction side) {}

    private record End(BlockPos pos, Direction side, TransportNode tank) {}

    private record Ports(List<Link> links, List<End> ends) {}

    static void tick(ServerLevel world, TransportNode first) {
        var nodes = new ArrayList<TransportNode>();
        var ports = new HashMap<TransportNode, Ports>();
        var kinds = new LinkedHashSet<FluidKey>();
        long tick = world.getGameTime();
        nodes.add(first);
        first.lastFluidTick = tick;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            TransportNode node = nodes.get(nodeIndex);
            var links = new ArrayList<Link>();
            var ends = new ArrayList<End>();
            int extractionBudget = node.spec.rate();
            for (int i = 0; i < TransportNode.SIDES.length; i++) {
                int index =
                        node.hasNozzles() ? Math.floorMod(tick + i, TransportNode.SIDES.length) : i;
                Direction side = TransportNode.SIDES[index];
                if (!node.open(side)) continue;
                var pos = node.getBlockPos().relative(side);
                if (!world.hasChunkAt(pos)) continue;
                var face = side.getOpposite();
                var other = world.getBlockEntity(pos);
                if (TransportNode.isPipe(other)) {
                    var next = (TransportNode) other;
                    if (!node.linksTo(side, next)) continue;
                    if (next.lastFluidTick != tick) {
                        nodes.add(next);
                        next.lastFluidTick = tick;
                    }
                    links.add(new Link(next, side));
                    continue;
                }
                var tank = other instanceof TransportNode n ? n : null;
                int legacyRate = node.pump(side);
                var nozzle = node.nozzle(side);
                if (legacyRate > 0 || nozzle == TransportNode.Nozzle.EXTRACTION) {
                    if (!TransportNode.machineOutput(other, face)) continue;
                    int rate = legacyRate > 0 ? legacyRate : extractionBudget;
                    int drawn = pump(world, node, pos, face, tank, rate);
                    if (legacyRate == 0) extractionBudget -= drawn;
                } else if (TransportNode.machineInput(other, face)
                        && (tank != null || TransportPlatform.hasFluidPort(world, pos, face))) {
                    ends.add(new End(pos, face, tank));
                }
            }
            if (node.liquid != null) kinds.add(node.liquid);
            ports.put(node, new Ports(links, ends));
        }
        for (FluidKey key : kinds) route(world, nodes, ports, key, tick);
    }

    private static int pump(
            ServerLevel world,
            TransportNode node,
            BlockPos pos,
            Direction face,
            TransportNode tank,
            int rate) {
        int room = Math.min(rate, node.spec.capacity() - node.amount);
        if (room <= 0) return 0;
        if (tank != null) {
            FluidKey key = tank.liquid;
            if (key == null) return 0;
            int amount = node.fill(null, key, room, true);
            return node.fill(null, key, tank.drain(face, key, amount, false), false);
        }
        var liquid = TransportPlatform.takeFluid(world, pos, face, node.liquid, room);
        return liquid == null ? 0 : node.fill(null, liquid.key(), liquid.amount(), false);
    }

    record Batch(FluidKey key, int amount) {}

    private static int deliver(
            ServerLevel world, End end, FluidKey key, int amount, boolean simulate) {
        return end.tank != null
                ? end.tank.fill(end.side, key, amount, simulate)
                : TransportPlatform.offerFluid(world, end.pos, end.side, key, amount, simulate);
    }

    private static boolean compatible(TransportNode pipe, FluidKey key) {
        return pipe.liquid == null || pipe.liquid.equals(key);
    }

    private static void route(
            ServerLevel world,
            List<TransportNode> nodes,
            Map<TransportNode, Ports> ports,
            FluidKey key,
            long tick) {
        Map<TransportNode, Integer> distance = new HashMap<>();
        var queue = new ArrayDeque<TransportNode>();
        for (var pipe : nodes) {
            if (!compatible(pipe, key)) continue;
            for (var end : ports.get(pipe).ends) {
                if (deliver(world, end, key, 1, true) > 0) {
                    queue.add(pipe);
                    distance.put(pipe, 0);
                    break;
                }
            }
        }
        while (!queue.isEmpty()) {
            var pipe = queue.remove();
            int next = distance.get(pipe) + 1;
            for (var link : ports.get(pipe).links) {
                if (compatible(link.pipe, key) && distance.putIfAbsent(link.pipe, next) == null)
                    queue.add(link.pipe);
            }
        }
        var order = new ArrayList<>(nodes);
        order.sort(Comparator.comparingInt(p -> distance.getOrDefault(p, Integer.MAX_VALUE)));
        for (var pipe : order) {
            if (!key.equals(pipe.liquid)) continue;
            var edges = ports.get(pipe);
            int budget = Math.min(pipe.amount, pipe.spec.rate());
            int ownDistance = distance.getOrDefault(pipe, Integer.MAX_VALUE);
            if (ownDistance == 0) {
                for (int i = 0; i < edges.ends.size() && budget > 0; i++) {
                    int index = Math.floorMod(tick + i, edges.ends.size());
                    int sent = deliver(world, edges.ends.get(index), key, budget, false);
                    budget -= sent;
                    pipe.debit(sent);
                }
            }
            for (int i = 0; i < edges.links.size() && budget > 0; i++) {
                var link = edges.links.get(Math.floorMod(tick + i, edges.links.size()));
                int amount = budget;
                if (ownDistance == Integer.MAX_VALUE) {
                    // Equalize filled fractions while every available outlet is blocked.
                    long difference =
                            (long) pipe.amount * link.pipe.spec.capacity()
                                    - (long) link.pipe.amount * pipe.spec.capacity();
                    amount =
                            (int)
                                    Math.min(
                                            budget,
                                            Math.max(
                                                    0L,
                                                    difference
                                                            / (pipe.spec.capacity()
                                                                    + link.pipe.spec.capacity())));
                } else if (distance.getOrDefault(link.pipe, Integer.MAX_VALUE) >= ownDistance) {
                    continue;
                }
                int sent = link.pipe.fill(link.side.getOpposite(), key, amount, false);
                budget -= sent;
                pipe.debit(sent);
            }
        }
    }
}
