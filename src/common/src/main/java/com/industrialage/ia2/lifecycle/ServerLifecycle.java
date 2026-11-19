package com.industrialage.ia2.lifecycle;

import com.industrialage.ia2.platform.EventFacade;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Tracks logical server sessions for one mod instance and delivers their typed notifications. */
public final class ServerLifecycle {
    /** Platform initialization completed. */
    public record Initialized() {}

    /** A fresh server session can accept world loads. */
    public record Started() {}

    /** A dimension joined this session. */
    public record WorldLoaded(String id) {}

    /** A dimension left this session. */
    public record WorldUnloaded(String id) {}

    /** A complete logical server tick, numbered from one within each session. */
    public record Tick(long number) {}

    /** All worlds of this session have been released. */
    public record Stopped() {}

    private final EventFacade events;
    private final Set<String> loaded = new LinkedHashSet<>();
    private boolean initialized;
    private volatile Thread owner;
    private long ticks;

    /** Delivers notifications through the supplied instance's event facade. */
    public ServerLifecycle(EventFacade events) {
        this.events = Objects.requireNonNull(events);
    }

    /** Completes common initialization once per platform instance. */
    public synchronized void initialize() {
        if (initialized) {
            throw new IllegalStateException("Platform already initialized");
        }
        initialized = true;
        events.publish(Initialized.class, new Initialized());
    }

    /** Opens a session on the calling server thread, before dimensions are loaded. */
    public synchronized void start() {
        if (!initialized || owner != null) {
            throw new IllegalStateException("Platform is not ready to start a server");
        }
        owner = Thread.currentThread();
        ticks = 0;
        events.publish(Started.class, new Started());
    }

    /** Records a loaded dimension before notifying consumers. */
    public void loadWorld(String id) {
        requireServerThread();
        if (Objects.requireNonNull(id).isBlank()) {
            throw new IllegalArgumentException("Missing dimension ID");
        }
        if (!loaded.add(id)) {
            throw new IllegalStateException("Dimension already loaded: " + id);
        }
        events.publish(WorldLoaded.class, new WorldLoaded(id));
    }

    /** Releases a loaded dimension before notifying consumers. */
    public void unloadWorld(String id) {
        requireServerThread();
        if (Objects.requireNonNull(id).isBlank()) {
            throw new IllegalArgumentException("Missing dimension ID");
        }
        if (!loaded.remove(id)) {
            throw new IllegalStateException("Dimension was not loaded: " + id);
        }
        events.publish(WorldUnloaded.class, new WorldUnloaded(id));
    }

    /** Returns an immutable view of the active dimension IDs at the time of this call. */
    public Set<String> worlds() {
        if (owner == null) {
            return Set.of();
        }
        requireServerThread();
        return Set.copyOf(loaded);
    }

    /** Completes one server tick and returns its sequence number. */
    public long tick() {
        requireServerThread();
        long number = ++ticks;
        events.publish(Tick.class, new Tick(number));
        return number;
    }

    /** Closes a session and releases even dimensions that did not receive an explicit unload. */
    public void stop() {
        requireServerThread();
        try {
            for (String id : List.copyOf(loaded)) {
                unloadWorld(id);
            }
            events.publish(Stopped.class, new Stopped());
        } finally {
            loaded.clear();
            owner = null;
        }
    }

    private void requireServerThread() {
        if (owner != Thread.currentThread()) {
            throw new IllegalStateException("No server session on this thread");
        }
    }
}
