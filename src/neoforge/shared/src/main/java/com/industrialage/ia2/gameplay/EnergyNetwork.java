package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.content.PowerGrid;
import com.industrialage.ia2.energy.graph.EnergyGraph;
import com.industrialage.ia2.energy.graph.EnergyGraph.Ports;
import com.industrialage.ia2.energy.graph.EnergyGraph.Position;
import com.industrialage.ia2.energy.graph.EnergyGraph.Route;
import com.industrialage.ia2.equipment.MeterSampling;
import com.industrialage.ia2.industrial.CableShock;
import com.industrialage.ia2.machine.FuelGenerator;
import com.industrialage.ia2.platform.CapabilityFacade.Face;
import com.industrialage.ia2.platform.ConfigFacade;
import com.industrialage.ia2.power.WorldPowerPort;
import com.industrialage.ia2.world.CableColors;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Settles EU once per world tick using loaded machines and their connected conductors. */
public final class EnergyNetwork {
    private static final Set<Face> ALL_FACES = Set.of(Face.values());
    private static final List<Ports> CABLE_PORTS =
            java.util.stream.IntStream.rangeClosed(0, 15)
                    .mapToObj(color -> new Ports(ALL_FACES, ALL_FACES, true, color))
                    .toList();
    private static final Ports SOURCE_PORTS = new Ports(Set.of(), ALL_FACES, false);
    private static final Ports SINK_PORTS = new Ports(ALL_FACES, Set.of(), false);
    private static final ConfigFacade.Key<Boolean> ROUND_LOSS =
            ActiveConfig.scalarKey("misc/roundEnetLoss", Boolean.class);
    private static final ConfigFacade.Key<Boolean> CABLE_MELTDOWN =
            ActiveConfig.scalarKey("misc/enableEnetCableMeltdown", Boolean.class);
    private static final ConfigFacade.Key<Boolean> MACHINE_OVERLOAD =
            ActiveConfig.scalarKey("misc/enableEnetExplosions", Boolean.class);
    private static final ConfigFacade.Key<Float> GENERATOR_RATE =
            ActiveConfig.scalarKey("balance/energy/generator/generator", Float.class);
    private static final Map<ServerLevel, EnergyNetwork> WORLDS = new HashMap<>();
    private static ConfigFacade config;

    private final Map<Position, WorldPowerPort> machines = new LinkedHashMap<>();
    private final Map<Position, List<Route>> routes = new HashMap<>();
    private final Set<Position> energizedDetectors = new HashSet<>();
    private boolean dirty = true;

    static void install(IEventBus gameBus, ConfigFacade settings) {
        config = settings;
        gameBus.addListener(
                LevelEvent.Unload.class,
                event -> {
                    if (event.getLevel() instanceof ServerLevel level) {
                        WORLDS.remove(level);
                    }
                });
        gameBus.addListener(
                ChunkEvent.Load.class,
                event -> {
                    if (event.getLevel() instanceof ServerLevel level) {
                        invalidate(level);
                    }
                });
        gameBus.addListener(
                ChunkEvent.Unload.class,
                event -> {
                    if (event.getLevel() instanceof ServerLevel level) {
                        invalidate(level);
                    }
                });
        gameBus.addListener(
                LevelTickEvent.Post.class,
                event -> {
                    if (event.getLevel() instanceof ServerLevel level) {
                        var network = WORLDS.get(level);
                        if (network != null) {
                            network.settle(level);
                        }
                        MeterSampling.finishTick(level);
                    }
                });
    }

    static int generatorRate() {
        var rate = config.current().map(values -> values.get(GENERATOR_RATE)).orElse(1F);
        return Math.clamp(
                Math.round(FuelGenerator.DEFAULT_ENERGY_PER_TICK * rate),
                0,
                FuelGenerator.CAPACITY);
    }

    /** Attaches or refreshes a native electricity endpoint in a loaded level. */
    public static void load(ServerLevel level, WorldPowerPort machine) {
        if (machine.energyPorts().inputs().isEmpty() && machine.energyPorts().outputs().isEmpty()) {
            return;
        }
        var network = WORLDS.computeIfAbsent(level, ignored -> new EnergyNetwork());
        network.machines.put(position(machine.getBlockPos()), machine);
        network.dirty = true;
    }

    /** Withdraws a native endpoint, matching its instance to avoid removing a replacement. */
    public static void unload(ServerLevel level, WorldPowerPort machine) {
        var network = WORLDS.get(level);
        if (network != null && network.machines.remove(position(machine.getBlockPos()), machine)) {
            network.dirty = true;
            if (network.machines.isEmpty()) {
                WORLDS.remove(level);
            }
        }
    }

    static void invalidate(ServerLevel level) {
        var network = WORLDS.get(level);
        if (network != null) {
            network.dirty = true;
        }
    }

    static boolean wasCarryingPower(ServerLevel level, BlockPos pos) {
        var network = WORLDS.get(level);
        return network != null && network.energizedDetectors.contains(position(pos));
    }

    /** Checks whether this loaded position belongs to a conductor or electricity endpoint. */
    public static boolean metered(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        if (level.getBlockState(pos).getBlock() instanceof CableBlock) return true;
        if (!(level.getBlockEntity(pos) instanceof WorldPowerPort port) || port.isRemoved())
            return false;
        var ports = port.energyPorts();
        return !ports.inputs().isEmpty() || !ports.outputs().isEmpty();
    }

    private void settle(ServerLevel level) {
        energizedDetectors.clear();
        var sources = new ArrayList<Source>();
        for (var entry : machines.entrySet()) {
            var machine = entry.getValue();
            if (machine.source() && machine.ranThisTick(level)) {
                double offer = machine.offer();
                if (offer > 0) {
                    sources.add(
                            new Source(entry.getKey(), machine, offer, machine.packetsPerTick()));
                }
            }
        }
        if (sources.isEmpty()) {
            return;
        }
        var active = config.current();
        boolean roundLoss = active.map(values -> values.get(ROUND_LOSS)).orElse(true);
        boolean meltdown = active.map(values -> values.get(CABLE_MELTDOWN)).orElse(true);
        boolean overload =
                meltdown && active.map(values -> values.get(MACHINE_OVERLOAD)).orElse(true);
        var round = new Round(roundLoss, meltdown, overload, new CableShock());
        int offset = level.getRandom().nextInt(sources.size());
        for (int i = 0; i < sources.size(); i++) {
            var source = sources.get((offset + i) % sources.size());
            for (int packet = 0; packet < source.packets(); packet++) {
                if (!send(level, source, round)) break;
            }
        }
        round.shocks.apply(level);
    }

    private boolean send(ServerLevel level, Source source, Round round) {
        if (dirty) {
            rebuild(level);
        }
        var destinations = routes.get(source.position());
        if (destinations == null || destinations.isEmpty() || source.machine().isRemoved()) {
            return false;
        }
        double budget = Math.min(source.offer(), source.machine().offer());
        double initialBudget = budget;
        int offset = level.getRandom().nextInt(destinations.size());
        for (int i = 0; i < destinations.size() && budget > 0; i++) {
            var route = destinations.get((i + offset) % destinations.size());
            var sink = machines.get(route.sink());
            if (sink != null && sink.ranThisTick(level)) {
                double debit =
                        PowerGrid.transfer(budget, route.rawLoss(), round.roundLoss, sink::receive);
                if (debit > 0) {
                    source.machine().draw(debit);
                    budget -= debit;
                    double loss = round.roundLoss ? Math.floor(route.rawLoss()) : route.rawLoss();
                    record(level, source, route, sink, debit, debit - loss);
                    afterTransfer(level, route, sink, debit, loss, round);
                    if (dirty) {
                        return true;
                    }
                }
            }
        }
        return budget < initialBudget;
    }

    private static void record(
            ServerLevel level,
            Source source,
            Route route,
            WorldPowerPort sink,
            double sent,
            double received) {
        if (!MeterSampling.watching(level)) return;
        MeterSampling.transfer(
                level,
                source.machine().getBlockPos(),
                sink.getBlockPos(),
                sent,
                received,
                source.offer());
        for (var cable : route.conductors()) {
            MeterSampling.conductor(level, blockPos(cable), received, source.offer());
        }
    }

    private void afterTransfer(
            ServerLevel level,
            Route route,
            WorldPowerPort sink,
            double debit,
            double loss,
            Round round) {
        for (var conductor : route.conductors()) {
            BlockPos pos = blockPos(conductor);
            if (level.getBlockState(pos).getBlock() instanceof CableBlock cable) {
                if (cable.exposed) round.shocks.record(pos, debit - loss);
                if (cable.detector()) {
                    energizedDetectors.add(conductor);
                }
                if (round.meltdown && debit > cable.packetLimit() + 1) {
                    level.destroyBlock(pos, false);
                    dirty = true;
                }
            }
        }
        if (round.overload && loss == 0 && debit > sink.packetLimit() && !sink.isRemoved()) {
            var pos = sink.getBlockPos();
            level.destroyBlock(pos, false);
            level.explode(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    2.5F,
                    Level.ExplosionInteraction.BLOCK);
            dirty = true;
        }
    }

    private void rebuild(ServerLevel level) {
        var graph = new EnergyGraph();
        var pending = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        var losses = new HashMap<Position, Double>();
        for (var entry : machines.entrySet()) {
            var machine = entry.getValue();
            graph.put(entry.getKey(), machine.energyPorts());
            addNeighbors(machine.getBlockPos(), pending, visited);
        }
        while (!pending.isEmpty()) {
            var pos = pending.removeFirst();
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof CableBlock cable && cable.conducts(state)) {
                var node = position(pos);
                graph.put(node, CABLE_PORTS.get(CableColors.color(state)));
                losses.put(node, PowerGrid.cableLoss(cable.path));
                addNeighbors(pos, pending, visited);
            }
        }
        routes.clear();
        for (var entry : machines.entrySet()) {
            if (entry.getValue().source()) {
                routes.put(entry.getKey(), graph.routes(entry.getKey(), losses::get));
            }
        }
        dirty = false;
    }

    private static void addNeighbors(
            BlockPos pos, ArrayDeque<BlockPos> pending, Set<BlockPos> visited) {
        for (var direction : Direction.values()) {
            var neighbor = pos.relative(direction);
            if (visited.add(neighbor)) {
                pending.addLast(neighbor);
            }
        }
    }

    static Ports ports(IndustrialMachine machine) {
        if (machine.kind() == MachineKind.IRON_FURNACE) return new Ports(Set.of(), Set.of(), false);
        if (machine.kind() == MachineKind.GENERATOR) {
            return SOURCE_PORTS;
        }
        if (!machine.source()) {
            return SINK_PORTS;
        }
        var side = MachineBlock.facing(machine.getBlockState());
        var face = Face.valueOf(side.name());
        return new Ports(EnumSet.complementOf(EnumSet.of(face)), Set.of(face), false);
    }

    private static Position position(BlockPos pos) {
        return new Position(pos.getX(), pos.getY(), pos.getZ());
    }

    private static BlockPos blockPos(Position pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }

    private record Round(
            boolean roundLoss, boolean meltdown, boolean overload, CableShock shocks) {}

    private record Source(Position position, WorldPowerPort machine, double offer, int packets) {}
}
