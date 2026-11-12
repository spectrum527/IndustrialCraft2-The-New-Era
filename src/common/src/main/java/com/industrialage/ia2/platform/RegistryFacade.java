package com.industrialage.ia2.platform;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Loader-independent registration of values during the host registration phase. */
public interface RegistryFacade {
    /**
     * Queues a registration without invoking its factory.
     *
     * <p>Uniqueness is per facade instance and pair of kind name and ID. A duplicate leaves the
     * original registration intact. Different kinds may use the same ID.
     *
     * <p>The adapter invokes the factory once during host registration. An exception or null result
     * leaves the handle permanently failed. The adapter publishes no value for that registration
     * and never retries the factory.
     *
     * @param kind registry kind recognized by the adapter
     * @param id ID of the entry
     * @param factory deferred source of the registered value
     * @param <T> registered value type
     * @return handle for the queued registration
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the kind is unsupported
     * @throws IllegalStateException if the registration is a duplicate or registration is closed
     */
    <T> Handle<T> register(Kind<T> kind, Id id, Supplier<? extends T> factory);

    /**
     * A named registry kind and the Java type supported for its values.
     *
     * @param name kind name recognized by the adapter
     * @param valueType Java type of the registered values
     * @param <T> registered value type
     */
    record Kind<T>(String name, Class<T> valueType) {
        /**
         * Creates a registry kind descriptor.
         *
         * @param name nonempty kind name
         * @param valueType Java type of the registered values
         * @throws NullPointerException if either argument is null
         * @throws IllegalArgumentException if the name is empty
         */
        public Kind {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(valueType, "valueType");
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Registry kind name is empty");
            }
        }
    }

    /**
     * The namespaced ID of an Industrial Age 2 registry entry.
     *
     * @param value complete entry ID
     */
    record Id(String value) {
        private static final Pattern VALID_ID = Pattern.compile("ia2:[a-z0-9/._-]+");

        /**
         * Creates an ID with the {@code ia2:} namespace and a nonempty resource path.
         *
         * @param value full ID matching {@code ia2:[a-z0-9/._-]+}
         * @throws NullPointerException if the value is null
         * @throws IllegalArgumentException if the value does not match the required format
         */
        public Id {
            if (!VALID_ID.matcher(Objects.requireNonNull(value, "value")).matches()) {
                throw new IllegalArgumentException("Invalid IA2 registry ID: " + value);
            }
        }
    }

    /**
     * The result of a queued registration.
     *
     * @param <T> registered value type
     */
    @FunctionalInterface
    interface Handle<T> {
        /**
         * Reads the bound value without invoking the factory.
         *
         * @return the same non-null value on each call after successful binding
         * @throws IllegalStateException if binding has yet to succeed or has failed
         */
        T get();
    }
}
