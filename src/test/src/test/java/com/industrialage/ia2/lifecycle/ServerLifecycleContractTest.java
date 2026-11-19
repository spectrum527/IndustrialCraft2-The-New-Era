package com.industrialage.ia2.lifecycle;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.platform.EventFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class ServerLifecycleContractTest {
    private static final String A = "minecraft:overworld";
    private static final String B = "minecraft:the_nether";
    private final List<Object> notifications = new ArrayList<>();
    private final EventFacade observer =
            new EventFacade() {
                public <E> Subscription subscribe(Class<E> type, Consumer<? super E> callback) {
                    throw new AssertionError("Lifecycle registered an unexpected callback");
                }

                public <E> void publish(Class<E> type, E event) {
                    notifications.add(type.cast(event));
                }
            };
    private final ServerLifecycle lifecycle = new ServerLifecycle(observer);

    @Test
    void sessionsReleaseEachWorldOnceAndRestartAtTickOne() {
        lifecycle.initialize();
        lifecycle.start();
        lifecycle.loadWorld(A);
        lifecycle.loadWorld(B);
        var both = lifecycle.worlds();
        assertEquals(Set.of(A, B), both);
        assertEquals(1, lifecycle.tick());
        assertEquals(2, lifecycle.tick());
        lifecycle.unloadWorld(A);
        assertEquals(Set.of(B), lifecycle.worlds());
        lifecycle.stop();
        assertTrue(lifecycle.worlds().isEmpty());
        assertEquals(Set.of(A, B), both);
        assertEquals(
                List.of(
                        new ServerLifecycle.Initialized(),
                        new ServerLifecycle.Started(),
                        new ServerLifecycle.WorldLoaded(A),
                        new ServerLifecycle.WorldLoaded(B),
                        new ServerLifecycle.Tick(1),
                        new ServerLifecycle.Tick(2),
                        new ServerLifecycle.WorldUnloaded(A),
                        new ServerLifecycle.WorldUnloaded(B),
                        new ServerLifecycle.Stopped()),
                notifications);
        lifecycle.start();
        assertTrue(lifecycle.worlds().isEmpty());
        lifecycle.loadWorld(A);
        assertEquals(1, lifecycle.tick());
        lifecycle.stop();
    }

    @Test
    void rejectedOperationsLeaveWorldsAndNotificationsIntact() {
        assertThrows(IllegalStateException.class, lifecycle::start);
        assertThrows(IllegalStateException.class, lifecycle::tick);
        assertThrows(IllegalStateException.class, lifecycle::stop);
        assertTrue(notifications.isEmpty());
        lifecycle.initialize();
        lifecycle.start();
        lifecycle.loadWorld(A);
        var before = List.copyOf(notifications);
        assertThrows(IllegalStateException.class, lifecycle::initialize);
        assertThrows(IllegalStateException.class, lifecycle::start);
        assertThrows(IllegalStateException.class, () -> lifecycle.loadWorld(A));
        assertThrows(IllegalStateException.class, () -> lifecycle.unloadWorld(B));
        assertThrows(IllegalArgumentException.class, () -> lifecycle.loadWorld(""));
        CompletableFuture.runAsync(
                        () -> {
                            assertThrows(IllegalStateException.class, lifecycle::tick);
                            assertThrows(IllegalStateException.class, () -> lifecycle.loadWorld(B));
                            assertThrows(
                                    IllegalStateException.class, () -> lifecycle.unloadWorld(A));
                            assertThrows(IllegalStateException.class, lifecycle::stop);
                        })
                .join();
        assertEquals(before, notifications);
        assertEquals(Set.of(A), lifecycle.worlds());
        assertEquals(1, lifecycle.tick());
    }
}
