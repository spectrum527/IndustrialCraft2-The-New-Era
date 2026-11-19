package com.industrialage.ia2.gametest;

import com.industrialage.ia2.platform.CapabilityFacade.Face;
import com.industrialage.ia2.platform.CapabilityFacade.Key;
import com.industrialage.ia2.platform.NeoForgeCapabilities;
import com.industrialage.ia2.platform.NeoForgePlatform;
import com.industrialage.ia2.platform.RegistryFacade.Handle;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.IBlockCapabilityProvider;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

final class PlatformWorldServices {
    private static final Key<String> PORT = new Key<>("fixture.face", String.class);
    private static final Key<Integer> BAD = new Key<>("fixture.bad", Integer.class);
    private final NeoForgePlatform host;
    private final List<Direction> sides = new ArrayList<>();

    PlatformWorldServices(NeoForgePlatform host, Handle<? extends Block> block) {
        this.host = host;
        var port = BlockCapability.createSided(ResourceLocation.parse("ic2:qa_side"), String.class);
        host.blockCapabilities()
                .registerBlock(
                        PORT,
                        port,
                        (level, pos, state, entity, side) -> {
                            sides.add(side);
                            return side == Direction.DOWN
                                    ? null
                                    : side == null ? "unsided" : side.name();
                        },
                        block);
        var bad =
                BlockCapability.createSided(
                        ResourceLocation.parse("ic2:qa_bad_side"), Integer.class);
        host.blockCapabilities().registerBlock(BAD, bad, wrongTypeProvider(), block);
    }

    // Simulates a misbehaving external provider while the production adapter handles the result.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static IBlockCapabilityProvider<Integer, Direction> wrongTypeProvider() {
        IBlockCapabilityProvider<Object, Direction> misconfigured = (l, p, s, e, side) -> "wrong";
        return (IBlockCapabilityProvider) misconfigured;
    }

    void checkBeforeRemoval(ServerLevel level, BlockPos pos, BiConsumer<String, Boolean> check) {
        var caps = host.services().capabilities();
        var target = host.blockCapabilities().target(level, pos);
        sides.clear();
        for (var face : Face.values()) {
            var value = caps.find(PORT, target, Optional.of(face));
            var expected = face == Face.DOWN ? Optional.empty() : Optional.of(face.name());
            check.accept(
                    "side-" + face,
                    value.equals(expected) && sides.getLast() == Direction.valueOf(face.name()));
        }
        check.accept(
                "no-side",
                caps.find(PORT, target, Optional.empty()).equals(Optional.of("unsided"))
                        && sides.getLast() == null
                        && sides.size() == 7);
        var absentKey = new Key<>("fixture.absent", String.class);
        check.accept("absent-key", caps.find(absentKey, target, Optional.empty()).isEmpty());
        PlatformRegistrationFailures.expect(
                IllegalArgumentException.class,
                () -> caps.find(new Key<>(PORT.name(), Integer.class), target, Optional.empty()));
        PlatformRegistrationFailures.expect(
                IllegalStateException.class, () -> caps.find(BAD, target, Optional.empty()));
        var separate =
                new NeoForgeCapabilities(
                        BusBuilder.builder().build(), BusBuilder.builder().build());
        PlatformRegistrationFailures.expect(
                IllegalArgumentException.class,
                () -> caps.find(PORT, separate.target(level, pos), Optional.empty()));
        check.accept("invalid-capability-inputs-rejected", true);

        var remote = new BlockPos(760000, 90, -760000);
        check.accept("chunk-unloaded-before", !level.hasChunkAt(remote));
        var unloaded = host.blockCapabilities().target(level, remote);
        check.accept("unloaded-target", caps.find(PORT, unloaded, Optional.empty()).isEmpty());
        check.accept("chunk-unloaded-after", !level.hasChunkAt(remote));

        var calls = new ArrayList<Integer>();
        host.hostEvents()
                .forward(
                        HostProbe.class,
                        NeutralProbe.class,
                        event -> new NeutralProbe(event.value));
        var thread = Thread.currentThread();
        try (var subscription =
                host.services()
                        .events()
                        .subscribe(
                                NeutralProbe.class,
                                probe -> {
                                    check.accept(
                                            "event-thread",
                                            Thread.currentThread() == thread
                                                    && level.getServer().isSameThread());
                                    calls.add(probe.value());
                                })) {
            NeoForge.EVENT_BUS.post(new HostProbe(29));
        }
        check.accept("event-forwarded-once", calls.equals(List.of(29)));
    }

    void checkAfterRemoval(ServerLevel level, BlockPos pos, BiConsumer<String, Boolean> check) {
        var target = host.blockCapabilities().target(level, pos);
        check.accept(
                "removed-provider",
                host.services().capabilities().find(PORT, target, Optional.empty()).isEmpty());
    }

    void watchWorldUnload(ServerLevel level, BlockPos survivor, String phase) {
        var target = host.blockCapabilities().target(level, survivor);
        var caps = host.services().capabilities();
        var side = Optional.of(Face.EAST);
        if (!caps.find(PORT, target, side).equals(Optional.of("EAST"))) {
            throw new AssertionError("Surviving provider was missing before world unload");
        }
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                LevelEvent.Unload.class,
                event -> {
                    if (event.getLevel() == level) {
                        if (caps.find(PORT, target, side).isPresent()) {
                            throw new AssertionError(
                                    "Unloaded world still exposes the surviving provider");
                        }
                        try {
                            Files.writeString(
                                    Path.of("capability-" + phase + ".txt"),
                                    "before world unload = EAST\nafter world unload = absent\n");
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                });
    }

    private record NeutralProbe(int value) {}

    static final class HostProbe extends Event {
        private final int value;

        HostProbe(int value) {
            this.value = value;
        }
    }
}
