package com.industrialage.ia2.machine;

import java.util.IdentityHashMap;
import java.util.List;

/** Dispatches a machine's lifecycle on its host's logical server thread. */
public final class BlockEntityBase {
    /** A machine part driven by the lifecycle dispatcher. */
    public interface Component {
        /** Acquires resources for the loaded lifetime and cleans up its own incomplete loads. */
        default void onLoad() {}

        /** Advances the component by one loaded server tick and reports if saved state changed. */
        boolean serverTick();

        /** Releases resources acquired during the loaded lifetime. */
        default void onUnload() {}
    }

    private enum State {
        UNLOADED,
        LOADING,
        LOADED,
        TICKING,
        UNLOAD_REQUESTED,
        UNLOADING
    }

    private final List<Component> components;
    private State state = State.UNLOADED;

    /** Fixes the callback order. Each component instance may appear only once. */
    public BlockEntityBase(Component... components) {
        this.components = List.of(components);
        var seen = new IdentityHashMap<Component, Boolean>();
        for (Component component : this.components) {
            if (seen.put(component, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("Duplicate machine component");
            }
        }
    }

    /** Loads in construction order and unwinds completed loads on failure or removal. */
    public void onLoad() {
        if (state != State.UNLOADED) {
            return;
        }
        state = State.LOADING;
        int completedLoads = 0;
        try {
            while (state == State.LOADING && completedLoads < components.size()) {
                components.get(completedLoads).onLoad();
                completedLoads++;
            }
        } catch (RuntimeException | Error failure) {
            unloadComponents(completedLoads, failure);
            throw failure;
        }
        if (state == State.UNLOAD_REQUESTED) {
            unloadComponents(completedLoads, null);
        } else {
            state = State.LOADED;
        }
    }

    /** Ticks loaded components in construction order and reports if any saved state changed. */
    public boolean serverTick() {
        if (state != State.LOADED) {
            return false;
        }
        state = State.TICKING;
        Throwable callbackFailure = null;
        try {
            boolean changed = false;
            for (Component component : components) {
                changed |= component.serverTick();
                if (state == State.UNLOAD_REQUESTED) {
                    break;
                }
            }
            return changed;
        } catch (RuntimeException | Error failure) {
            callbackFailure = failure;
            throw failure;
        } finally {
            if (state == State.UNLOAD_REQUESTED) {
                unloadComponents(components.size(), callbackFailure);
            } else {
                state = State.LOADED;
            }
        }
    }

    /**
     * Unloads in reverse construction order. If a component callback is in progress, unloading
     * starts after that callback and prevents dispatch to the remaining components.
     */
    public void onUnload() {
        switch (state) {
            case LOADING, TICKING -> state = State.UNLOAD_REQUESTED;
            case LOADED -> unloadComponents(components.size(), null);
            case UNLOADED, UNLOAD_REQUESTED, UNLOADING -> {}
        }
    }

    /** Reports whether loading completed and no unload has been requested. */
    public boolean isLoaded() {
        return state == State.LOADED || state == State.TICKING;
    }

    private void unloadComponents(int count, Throwable callbackFailure) {
        state = State.UNLOADING;
        Throwable firstFailure = callbackFailure;
        try {
            for (int i = count - 1; i >= 0; i--) {
                try {
                    components.get(i).onUnload();
                } catch (RuntimeException | Error failure) {
                    if (firstFailure == null) {
                        firstFailure = failure;
                    } else if (firstFailure != failure) {
                        firstFailure.addSuppressed(failure);
                    }
                }
            }
        } finally {
            state = State.UNLOADED;
        }
        if (callbackFailure != null) {
            return;
        }
        if (firstFailure instanceof RuntimeException exception) {
            throw exception;
        }
        if (firstFailure instanceof Error error) {
            throw error;
        }
    }
}
