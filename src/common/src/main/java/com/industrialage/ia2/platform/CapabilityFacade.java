package com.industrialage.ia2.platform;

import java.util.Objects;
import java.util.Optional;

/** Typed capability queries against adapter-owned targets. */
public interface CapabilityFacade {
    /**
     * Queries a capability without loading a chunk or creating a target. A returned capability may
     * become invalid when the target unloads or the world changes.
     *
     * @param key capability key
     * @param target target from this facade instance
     * @param face face to query, or empty for an unspecified face
     * @param <T> capability value type
     * @return capability value, or empty if the capability is unregistered, the provider is
     *     missing, the target is unloaded, or the capability is unavailable on that face
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the target is foreign or a declared key has a different
     *     value type
     * @throws IllegalStateException if the provider returns a value of the wrong type
     */
    <T> Optional<T> find(Key<T> key, Target target, Optional<Face> face);

    /**
     * A named capability and its declared Java type.
     *
     * @param name nonempty capability name
     * @param valueType type of the capability value
     * @param <T> capability value type
     */
    record Key<T>(String name, Class<T> valueType) {
        /**
         * Validates the key name and value type.
         *
         * @throws NullPointerException if either component is null
         * @throws IllegalArgumentException if the name is empty
         */
        public Key {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(valueType, "valueType");
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Capability name is empty");
            }
        }
    }

    /** An adapter-owned capability target scoped to one facade instance. */
    interface Target {}

    /** Face from which a capability is queried. */
    enum Face {
        /** Bottom face. */
        DOWN,

        /** Top face. */
        UP,

        /** North face. */
        NORTH,

        /** South face. */
        SOUTH,

        /** West face. */
        WEST,

        /** East face. */
        EAST
    }
}
