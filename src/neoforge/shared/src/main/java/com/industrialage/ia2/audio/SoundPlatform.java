package com.industrialage.ia2.audio;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Binds the sound registry to the loader and limits playback code to the physical client. */
public final class SoundPlatform {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, "ic2");
    private static final Map<String, Supplier<SoundEvent>> EVENTS = new HashMap<>();

    private SoundPlatform() {}

    /** Registers the measured IC2 sound IDs backed by the project's audio. */
    public static void install(IEventBus bus, boolean client) {
        var source =
                Objects.requireNonNull(
                        SoundPlatform.class.getResourceAsStream(
                                "/com/industrialage/ia2/audio/sounds.txt"));
        try (var paths =
                new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))) {
            paths.lines()
                    .forEach(
                            path ->
                                    EVENTS.put(
                                            path,
                                            SOUNDS.register(
                                                    path, SoundEvent::createVariableRangeEvent)));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        SOUNDS.register(bus);
        if (client) {
            ClientMachineAudio.install();
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                    net.neoforged.neoforge.event.level.LevelEvent.Unload.class,
                    e -> {
                        if (e.getLevel() instanceof net.minecraft.world.level.Level level)
                            ClientMachineAudio.unload(level);
                    });
        }
    }

    static SoundEvent event(String path) {
        return Objects.requireNonNull(EVENTS.get(path), path).get();
    }

    static String blockPath(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
    }
}
