package com.industrialage.ia2.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;

/** Synchronous typed streams and explicit mappings for NeoForge game events. */
public final class NeoForgeEvents implements EventFacade {
    private final IEventBus gameBus;
    private final ConcurrentHashMap<Class<?>, Stream> streams = new ConcurrentHashMap<>();

    /** Creates streams whose first use records the owning host thread for each event type. */
    public NeoForgeEvents(IEventBus gameBus) {
        this.gameBus = Objects.requireNonNull(gameBus, "gameBus");
    }

    /** Connects a host event to a neutral stream on the delivering thread. */
    public <H extends Event, E> void forward(
            Class<H> hostType, Class<E> type, Function<? super H, ? extends E> convert) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(convert, "convert");
        gameBus.addListener(hostType, event -> publish(type, convert.apply(event)));
    }

    /** Releases streams owned by the calling logical server thread when its session ends. */
    public void endServerSession() {
        var serverThread = Thread.currentThread();
        streams.values()
                .removeIf(
                        stream -> {
                            if (stream.owner != serverThread) {
                                return false;
                            }
                            stream.listeners.forEach(listener -> listener.active = false);
                            stream.listeners.clear();
                            return true;
                        });
    }

    @Override
    public <E> Subscription subscribe(Class<E> type, Consumer<? super E> callback) {
        Objects.requireNonNull(callback, "callback");
        var stream = stream(type);
        var listener = new Listener(stream, event -> callback.accept(type.cast(event)));
        stream.listeners.add(listener);
        return listener;
    }

    @Override
    public <E> void publish(Class<E> type, E event) {
        Objects.requireNonNull(event, "event");
        if (!type.isInstance(event)) {
            throw new IllegalArgumentException("Incompatible event: " + type.getName());
        }
        for (var listener : List.copyOf(stream(type).listeners)) {
            if (listener.active) {
                listener.callback.accept(event);
            }
        }
    }

    private Stream stream(Class<?> type) {
        var stream = streams.computeIfAbsent(type, ignored -> new Stream());
        stream.checkThread();
        return stream;
    }

    private static final class Stream {
        private final Thread owner = Thread.currentThread();
        private final List<Listener> listeners = new ArrayList<>();

        private void checkThread() {
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException("Event stream used outside its host thread");
            }
        }
    }

    private static final class Listener implements Subscription {
        private final Stream stream;
        private final Consumer<Object> callback;
        private boolean active = true;

        private Listener(Stream stream, Consumer<Object> callback) {
            this.stream = stream;
            this.callback = callback;
        }

        @Override
        public void close() {
            stream.checkThread();
            if (active) {
                active = false;
                stream.listeners.remove(this);
            }
        }
    }
}
