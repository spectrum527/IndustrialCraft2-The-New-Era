package com.industrialage.ia2.forge.content;

import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.content.PowerGrid;
import com.industrialage.ia2.energy.graph.EnergyGraph;
import com.industrialage.ia2.energy.graph.EnergyGraph.Position;
import com.industrialage.ia2.equipment.MeterSampling;
import com.industrialage.ia2.platform.CapabilityFacade.Face;
import com.industrialage.ia2.platform.ConfigFacade;
import com.industrialage.ia2.power.WorldPowerPort;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ForgePower {
    private static final Map<ServerLevel, ForgePower> WORLDS = new IdentityHashMap<>();
    private static ConfigFacade config;

    private final EnergyGraph graph = new EnergyGraph();
    private final Map<Position, Node> nodes = new LinkedHashMap<>();
    private final Map<Position, List<EnergyGraph.Route>> routes = new HashMap<>();

    static void register(IEventBus bus, ConfigFacade settings) {
        config = settings;
        bus.addListener(
                (TickEvent.LevelTickEvent event) -> {
                    if (event.phase == TickEvent.Phase.END
                            && event.level instanceof ServerLevel level) {
                        var grid = WORLDS.get(level);
                        if (grid != null) {
                            grid.settle(level);
                        }
                        MeterSampling.finishTick(level);
                    }
                });
        bus.addListener(
                (LevelEvent.Unload event) -> {
                    if (event.getLevel() instanceof ServerLevel level) {
                        WORLDS.remove(level);
                    }
                });
    }

    static <T> T setting(String path, Class<T> type, T fallback) {
        return config.current()
                .map(value -> value.get(ActiveConfig.scalarKey(path, type)))
                .orElse(fallback);
    }

    /** Adds a loaded machine or refreshes its oriented ports after a mode change. */
    public static void attach(BlockEntity entity) {
        Endpoint endpoint = endpoint(entity);
        if (entity.getLevel() instanceof ServerLevel level && endpoint != null) {
            var grid = WORLDS.computeIfAbsent(level, unused -> new ForgePower());
            var pos = position(entity.getBlockPos());
            grid.nodes.put(pos, new Node(entity, endpoint));
            var input = EnumSet.noneOf(Face.class);
            var output = EnumSet.noneOf(Face.class);
            for (Direction side : Direction.values()) {
                var face = Face.valueOf(side.name());
                if (endpoint.accepts(side)) {
                    input.add(face);
                }
                if (endpoint.emits(side)) {
                    output.add(face);
                }
            }
            int color = com.industrialage.ia2.world.CableColors.color(entity.getBlockState());
            grid.graph.put(pos, new EnergyGraph.Ports(input, output, endpoint.conducts(), color));
            grid.routes.clear();
        }
    }

    private static Endpoint endpoint(BlockEntity entity) {
        return entity instanceof Endpoint port
                ? port
                : entity instanceof WorldPowerPort extra ? new AdditionalPort(extra) : null;
    }

    static void refresh(BlockEntity entity) {
        if (entity.getLevel() instanceof ServerLevel level) {
            var grid = WORLDS.get(level);
            if (grid != null && grid.owns(entity)) {
                attach(entity);
            }
        }
    }

    /** Removes this exact block entity from its loaded level. */
    public static void detach(BlockEntity entity) {
        if (entity.getLevel() instanceof ServerLevel level) {
            var grid = WORLDS.get(level);
            if (grid != null && grid.owns(entity)) {
                var pos = position(entity.getBlockPos());
                grid.nodes.remove(pos);
                grid.graph.remove(pos);
                grid.routes.clear();
                if (grid.nodes.isEmpty()) {
                    WORLDS.remove(level);
                }
            }
        }
    }

    private boolean owns(BlockEntity entity) {
        var node = nodes.get(position(entity.getBlockPos()));
        return node != null && node.entity == entity;
    }

    /** Checks whether the loaded position belongs to the native EU graph. */
    public static boolean metered(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var entity = level.getBlockEntity(pos);
        if (entity == null || entity.isRemoved()) return false;
        var endpoint = endpoint(entity);
        if (endpoint == null) return false;
        if (endpoint.conducts()) return true;
        for (var side : Direction.values()) {
            if (endpoint.accepts(side) || endpoint.emits(side)) return true;
        }
        return false;
    }

    private void settle(ServerLevel level) {
        var offers = new ArrayList<Offer>();
        nodes.forEach(
                (pos, node) -> {
                    double budget = node.endpoint.offers();
                    if (budget > 0 && node.ticking(level)) {
                        offers.add(new Offer(pos, node, budget, node.endpoint.packetsPerTick()));
                    }
                });
        if (offers.isEmpty()) {
            return;
        }
        var round =
                new Round(
                        setting("misc/roundEnetLoss", Boolean.class, true),
                        setting("misc/enableEnetCableMeltdown", Boolean.class, true),
                        setting("misc/enableEnetExplosions", Boolean.class, true),
                        new LinkedHashSet<>(),
                        new LinkedHashSet<>());
        int first = level.getRandom().nextInt(offers.size());
        for (int offset = 0; offset < offers.size(); offset++) {
            var offer = offers.get((first + offset) % offers.size());
            for (int packet = 0; packet < offer.packets; packet++) {
                if (!deliver(level, offer, round)) break;
            }
        }
        for (var cable : round.burntCables) {
            if (owns(cable)) {
                level.destroyBlock(cable.getBlockPos(), false);
            }
        }
        for (var machine : round.overloadedMachines) {
            if (owns(machine)) {
                var pos = machine.getBlockPos();
                level.removeBlock(pos, false);
                level.explode(
                        null,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        2.5F,
                        Explosion.BlockInteraction.DESTROY);
            }
        }
    }

    private boolean deliver(ServerLevel level, Offer offer, Round round) {
        var source = offer.node.endpoint;
        double budget = Math.min(offer.eu, source.offers());
        double start = budget;
        var destinations =
                routes.computeIfAbsent(
                        offer.pos,
                        pos -> graph.routes(pos, cable -> nodes.get(cable).endpoint.loss()));
        if (destinations.isEmpty()) return false;
        int first = level.getRandom().nextInt(destinations.size());
        for (int index = 0; index < destinations.size() && budget > 0; index++) {
            var route = destinations.get((first + index) % destinations.size());
            var sink = nodes.get(route.sink());
            if (sink == null
                    || !sink.ticking(level)
                    || round.overloadedMachines.contains(sink.entity)) {
                continue;
            }
            boolean lossless = (round.rounded ? Math.floor(route.rawLoss()) : route.rawLoss()) == 0;
            double debit =
                    PowerGrid.transfer(
                            budget,
                            route.rawLoss(),
                            round.rounded,
                            eu -> {
                                double accepted = sink.endpoint.receive(eu);
                                if (round.meltdown
                                        && round.explosions
                                        && lossless
                                        && accepted > sink.endpoint.inputRating()) {
                                    round.overloadedMachines.add(sink.entity);
                                }
                                return accepted;
                            });
            double drawn = source.draw(debit);
            budget -= drawn;
            if (drawn > 0) {
                boolean watched = MeterSampling.watching(level);
                double received =
                        drawn - (round.rounded ? Math.floor(route.rawLoss()) : route.rawLoss());
                if (watched) {
                    MeterSampling.transfer(
                            level,
                            offer.node.entity.getBlockPos(),
                            sink.entity.getBlockPos(),
                            drawn,
                            received,
                            offer.eu);
                }
                for (var pos : route.conductors()) {
                    var node = nodes.get(pos);
                    if (node != null && node.entity instanceof CableBlock.Conductor conductor) {
                        if (watched)
                            MeterSampling.conductor(
                                    level, conductor.getBlockPos(), received, offer.eu);
                        conductor.markTransfer();
                        if (round.meltdown && conductor.overCurrent(drawn)) {
                            round.burntCables.add(conductor);
                        }
                    }
                }
            }
        }
        return budget < start;
    }

    private static Position position(BlockPos pos) {
        return new Position(pos.getX(), pos.getY(), pos.getZ());
    }

    private record AdditionalPort(WorldPowerPort machine) implements Endpoint {
        @Override
        public boolean accepts(Direction side) {
            return machine.energyPorts().inputs().contains(Face.valueOf(side.name()));
        }

        @Override
        public boolean emits(Direction side) {
            return machine.energyPorts().outputs().contains(Face.valueOf(side.name()));
        }

        @Override
        public double offers() {
            return machine.offer();
        }

        @Override
        public int packetsPerTick() {
            return machine.packetsPerTick();
        }

        @Override
        public double draw(double requested) {
            return machine.draw(requested);
        }

        @Override
        public double receive(double offered) {
            return machine.receive(offered);
        }

        @Override
        public double inputRating() {
            return machine.packetLimit();
        }
    }

    interface Endpoint {
        default int packetsPerTick() {
            return 1;
        }

        default boolean accepts(Direction face) {
            return false;
        }

        default boolean emits(Direction face) {
            return false;
        }

        default boolean conducts() {
            return false;
        }

        default double loss() {
            return 0;
        }

        default double offers() {
            return 0;
        }

        default double draw(double eu) {
            return 0;
        }

        default double inputRating() {
            return Double.POSITIVE_INFINITY;
        }

        default double receive(double eu) {
            return 0;
        }
    }

    private record Node(BlockEntity entity, Endpoint endpoint) {
        boolean ticking(ServerLevel level) {
            return !entity.isRemoved()
                    && level.isPositionEntityTicking(entity.getBlockPos())
                    && (!(endpoint instanceof AdditionalPort extra)
                            || extra.machine.ranThisTick(level));
        }
    }

    private record Offer(Position pos, Node node, double eu, int packets) {}

    private record Round(
            boolean rounded,
            boolean meltdown,
            boolean explosions,
            Set<BlockEntity> burntCables,
            Set<BlockEntity> overloadedMachines) {}
}
