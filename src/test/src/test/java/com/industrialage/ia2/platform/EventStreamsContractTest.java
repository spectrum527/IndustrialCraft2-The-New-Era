package com.industrialage.ia2.platform;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EventStreamsContractTest {
    private final EventStreams events = new EventStreams();

    @Test
    void aClosedListenerIsSkippedAndOnlyNestedDispatchSeesNewSubscriptions() {
        var seen = new ArrayList<String>();
        var second = new AtomicReference<EventFacade.Subscription>();
        events.subscribe(
                Integer.class,
                number -> {
                    seen.add("a" + number);
                    if (number == 1) {
                        second.get().close();
                        second.get().close();
                        events.subscribe(Integer.class, inner -> seen.add("c" + inner));
                        events.publish(Integer.class, 2);
                    }
                });
        second.set(events.subscribe(Integer.class, number -> seen.add("b" + number)));
        events.subscribe(Number.class, number -> fail("Received a subclass event"));
        events.publish(Integer.class, 1);
        assertEquals(List.of("a1", "a2", "c2"), seen);
    }

    @Test
    void callbackFailureRetainsSubscriptionsAndSessionCleanupReleasesThem() {
        var calls = new AtomicInteger();
        var cause = new IllegalArgumentException("callback");
        var failing =
                events.subscribe(
                        String.class,
                        value -> {
                            calls.incrementAndGet();
                            throw cause;
                        });
        events.subscribe(String.class, value -> calls.addAndGet(10));
        for (int i = 0; i < 2; i++) {
            assertSame(
                    cause,
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> events.publish(String.class, "test")));
        }
        assertEquals(2, calls.get());
        CompletableFuture.runAsync(
                        () -> {
                            assertThrows(IllegalStateException.class, failing::close);
                            assertThrows(
                                    IllegalStateException.class,
                                    () -> events.publish(String.class, "cross-thread"));
                            assertThrows(
                                    IllegalStateException.class,
                                    () -> events.subscribe(String.class, value -> {}));
                        })
                .join();
        failing.close();
        events.publish(String.class, "same-thread");
        assertEquals(12, calls.get());
        events.endServerSession();
        CompletableFuture.runAsync(() -> events.publish(String.class, "new-session")).join();
        assertEquals(12, calls.get());
    }
}
