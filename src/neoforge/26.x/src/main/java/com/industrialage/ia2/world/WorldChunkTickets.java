package com.industrialage.ia2.world;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

final class WorldChunkTickets {
    private static final DeferredRegister<TicketType> TYPES =
            DeferredRegister.create(Registries.TICKET_TYPE, "ic2");
    private static final java.util.function.Supplier<TicketType> LEASE =
            TYPES.register(
                    "world_loader",
                    () ->
                            new TicketType(
                                    0L,
                                    TicketType.FLAG_LOADING
                                            | TicketType.FLAG_SIMULATION
                                            | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));

    static void install(IEventBus bus) {
        TYPES.register(bus);
    }

    private static final Map<ServerLevel, Map<ChunkPos, Integer>> HOLDERS = new WeakHashMap<>();

    private WorldChunkTickets() {}

    static void hold(ServerLevel level, ChunkPos chunk) {
        int count =
                HOLDERS.computeIfAbsent(level, ignored -> new HashMap<>())
                        .merge(chunk, 1, Integer::sum);
        if (count == 1) {
            level.getChunkSource().addTicketWithRadius(LEASE.get(), chunk, 2);
            level.setChunkForced(chunk.x(), chunk.z(), true);
        }
    }

    static void release(ServerLevel level, ChunkPos chunk) {
        release(level, chunk, true);
    }

    static void release(ServerLevel level, ChunkPos chunk, boolean unforce) {
        var leases = HOLDERS.get(level);
        if (leases == null) return;
        var count = leases.get(chunk);
        if (count == null) return;
        if (count > 1) {
            leases.put(chunk, count - 1);
        } else {
            leases.remove(chunk);
            level.getChunkSource().removeTicketWithRadius(LEASE.get(), chunk, 2);
            if (unforce) level.setChunkForced(chunk.x(), chunk.z(), false);
        }
        if (leases.isEmpty()) HOLDERS.remove(level);
    }
}
