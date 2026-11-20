package com.industrialage.ia2.audio;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Owns each looping sound for its client level and block position until activity or loading ends.
 */
final class ClientMachineAudio {
    private static final Map<BlockPos, Loop> PLAYING = new HashMap<>();
    private static Level current;

    private ClientMachineAudio() {}

    static void install() {
        IndustrialSounds.install(ClientMachineAudio::update);
    }

    static void unload(Level level) {
        if (current == level) {
            PLAYING.values().forEach(Loop::finish);
            PLAYING.clear();
            current = null;
        }
    }

    private static boolean active(BlockState state) {
        var p = state.getBlock().getStateDefinition().getProperty("active");
        return p instanceof BooleanProperty b && state.getValue(b);
    }

    private static void update(Level level, BlockPos pos, BlockState state) {
        if (level != current && current != null) unload(current);
        if (!active(state) || PLAYING.containsKey(pos)) return;
        var player = Minecraft.getInstance().player;
        if (player == null
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                        > 32 * 32) return;
        String path = IndustrialSounds.loop(SoundPlatform.blockPath(state));
        if (path != null) {
            current = level;
            var loop = new Loop(level, pos.immutable(), state, path);
            PLAYING.put(pos.immutable(), loop);
            Minecraft.getInstance().getSoundManager().play(loop);
        }
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        final Level world;
        final BlockPos pos;
        final BlockState initial;

        Loop(Level world, BlockPos pos, BlockState state, String path) {
            super(
                    SoundPlatform.event(path),
                    SoundSource.BLOCKS,
                    SoundInstance.createUnseededRandom());
            this.world = world;
            this.pos = pos;
            this.initial = state;
            x = pos.getX() + 0.5;
            y = pos.getY() + 0.5;
            z = pos.getZ() + 0.5;
            volume = .44F;
            looping = true;
        }

        void finish() {
            stop();
        }

        @Override
        public void tick() {
            var client = Minecraft.getInstance();
            if (client.level != world
                    || client.player == null
                    || client.player.distanceToSqr(x, y, z) > 36 * 36
                    || !world.hasChunkAt(pos)) {
                finish();
                PLAYING.remove(pos, this);
                if (PLAYING.isEmpty()) current = null;
                return;
            }
            var now = world.getBlockState(pos);
            if (!now.is(initial.getBlock()) || !active(now)) {
                finish();
                PLAYING.remove(pos, this);
                if (PLAYING.isEmpty()) current = null;
            }
        }
    }
}
