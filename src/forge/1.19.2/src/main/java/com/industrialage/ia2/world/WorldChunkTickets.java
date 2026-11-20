package com.industrialage.ia2.world;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

final class WorldChunkTickets {
    private static final TicketType<ChunkPos> LEASE =
            TicketType.create("ic2_world_loader", Comparator.comparingLong(ChunkPos::toLong));
    private static final Map<ServerLevel, Map<ChunkPos, Integer>> HOLDERS = new WeakHashMap<>();

    private WorldChunkTickets() {}

    static void hold(ServerLevel level, ChunkPos chunk) {
        int count =
                HOLDERS.computeIfAbsent(level, ignored -> new HashMap<>())
                        .merge(chunk, 1, Integer::sum);
        if (count == 1) level.getChunkSource().addRegionTicket(LEASE, chunk, 2, chunk);
    }

    static void release(ServerLevel level, ChunkPos chunk) {
        var leases = HOLDERS.get(level);
        if (leases == null) return;
        var count = leases.get(chunk);
        if (count == null) return;
        if (count > 1) {
            leases.put(chunk, count - 1);
        } else {
            leases.remove(chunk);
            level.getChunkSource().removeRegionTicket(LEASE, chunk, 2, chunk);
        }
        if (leases.isEmpty()) HOLDERS.remove(level);
    }
}
