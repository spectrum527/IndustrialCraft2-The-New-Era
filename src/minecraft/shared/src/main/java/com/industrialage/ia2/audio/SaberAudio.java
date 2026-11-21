package com.industrialage.ia2.audio;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Server-side one-shot audio for the nano saber. */
public final class SaberAudio {
    private static final int CUE_INTERVAL = 7;
    private static final Map<ServerPlayer, Integer> LAST_CUE = new WeakHashMap<>();

    private SaberAudio() {}

    /** Plays a cue if the player's previous saber sound has had time to finish. */
    public static void play(ServerLevel level, ServerPlayer player, String cue) {
        int now = player.tickCount;
        synchronized (LAST_CUE) {
            Integer last = LAST_CUE.get(player);
            if (last != null && now - last < CUE_INTERVAL) return;
            LAST_CUE.put(player, now);
        }
        IndustrialSounds.play(level, player.blockPosition(), "item.nanosaber." + cue);
    }
}
