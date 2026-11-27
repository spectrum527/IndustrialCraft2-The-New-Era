package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.lifecycle.ServerLifecycle;
import com.industrialage.ia2.platform.EventFacade;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;

final class PlatformLifecycleTrace {
    private final List<String> observations = new ArrayList<>();
    private final Set<String> worlds = new LinkedHashSet<>();
    private long ticks;

    PlatformLifecycleTrace(String phase, EventFacade events) {
        MinecraftForge.EVENT_BUS.addListener(
                EventPriority.HIGHEST,
                false,
                ServerAboutToStartEvent.class,
                start -> {
                    events.subscribe(
                            ServerLifecycle.Started.class, signal -> observations.add("start"));
                    events.subscribe(
                            ServerLifecycle.WorldLoaded.class,
                            world -> {
                                if (observations.isEmpty() || !worlds.add(world.id())) {
                                    throw new AssertionError(
                                            "Unordered or duplicate world load " + world.id());
                                }
                                observations.add("load " + world.id());
                            });
                    events.subscribe(
                            ServerLifecycle.Tick.class,
                            tick -> {
                                if (++ticks != tick.number()) {
                                    throw new AssertionError(
                                            "Unexpected server tick " + tick.number());
                                }
                                if (ticks == 1) {
                                    observations.add("tick 1");
                                }
                            });
                    events.subscribe(
                            ServerLifecycle.WorldUnloaded.class,
                            world -> {
                                if (!worlds.remove(world.id())) {
                                    throw new AssertionError(
                                            "Unloaded unknown world " + world.id());
                                }
                                observations.add("unload " + world.id());
                            });
                    events.subscribe(
                            ServerLifecycle.Stopped.class,
                            signal -> {
                                observations.add("stop");
                                var primary =
                                        observations.stream()
                                                .filter(
                                                        value ->
                                                                !value.startsWith("load ")
                                                                                && !value
                                                                                        .startsWith(
                                                                                                "unload"
                                                                                                        + " ")
                                                                        || value.endsWith(
                                                                                " minecraft:overworld"))
                                                .toList();
                                if (!worlds.isEmpty()
                                        || !primary.equals(
                                                List.of(
                                                        "start",
                                                        "load minecraft:overworld",
                                                        "tick 1",
                                                        "unload minecraft:overworld",
                                                        "stop"))) {
                                    throw new AssertionError(
                                            "Incomplete lifecycle " + observations);
                                }
                                try {
                                    Files.write(
                                            Path.of("lifecycle-" + phase + ".txt"), observations);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                });
    }
}
