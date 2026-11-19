package com.industrialage.ia2.platform;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/** Read-only view of the configuration for one platform instance. */
@FunctionalInterface
public interface ConfigFacade {
    /** {@return the most recent valid snapshot, or empty before the first publication} */
    Optional<Snapshot> current();

    /**
     * Checks and freezes a resolved value at publication.
     *
     * @param <T> immutable value type
     */
    final class Type<T> {
        private static final Set<Class<?>> SCALARS =
                Set.of(
                        String.class,
                        Boolean.class,
                        Character.class,
                        Byte.class,
                        Short.class,
                        Integer.class,
                        Long.class,
                        Float.class,
                        Double.class);
        private final Function<Object, T> freeze;

        private Type(Function<Object, T> freeze) {
            this.freeze = freeze;
        }

        /**
         * {@return a shape requiring exactly the given scalar type}
         *
         * @param <T> scalar type
         * @param valueType string or boxed primitive class
         */
        public static <T> Type<T> scalar(Class<T> valueType) {
            if (!SCALARS.contains(Objects.requireNonNull(valueType, "valueType"))) {
                throw new IllegalArgumentException("Unsupported scalar type: " + valueType);
            }
            return new Type<>(
                    value -> {
                        if (!valueType.isInstance(value)) {
                            throw new IllegalArgumentException("Expected " + valueType.getName());
                        }
                        return valueType.cast(value);
                    });
        }

        /**
         * {@return a shape that recursively checks and freezes each list element}
         *
         * @param <E> element type
         * @param elementType required shape of each element
         */
        public static <E> Type<List<E>> list(Type<E> elementType) {
            Objects.requireNonNull(elementType, "elementType");
            return new Type<>(
                    value -> {
                        if (!(value instanceof List<?> elements)) {
                            throw new IllegalArgumentException("Expected a list");
                        }
                        return elements.stream().map(elementType.freeze).toList();
                    });
        }
    }

    /**
     * A key identified by its declaration object. Consumers retain the declared key for reads.
     *
     * @param <T> configuration value type
     */
    final class Key<T> {
        private final String name;
        private final Type<T> type;

        /**
         * Creates a declaration for use by the adapter and its consumers.
         *
         * @param name nonempty configuration key name
         * @param type value shape
         */
        public Key(String name, Type<T> type) {
            this.name = Objects.requireNonNull(name, "name");
            this.type = Objects.requireNonNull(type, "type");
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Configuration key name is empty");
            }
        }

        /** {@return the configuration key name} */
        public String name() {
            return name;
        }

        /** {@return the declared value shape} */
        public Type<T> type() {
            return type;
        }
    }

    /** Immutable values from one successful validation of the complete configuration. */
    final class Snapshot {
        private final Map<Key<?>, Object> values;

        private Snapshot(Map<Key<?>, Object> values) {
            this.values = Map.copyOf(values);
        }

        /**
         * {@return the value frozen for this declared key}
         *
         * @param <T> immutable result type
         * @param key declaration originally supplied to the publisher
         * @throws IllegalArgumentException if the key was not declared
         */
        @SuppressWarnings("unchecked") // Publication freezes each entry with its own typed key.
        public <T> T get(Key<T> key) {
            Object value = values.get(Objects.requireNonNull(key, "key"));
            if (value == null) {
                throw new IllegalArgumentException("Undeclared configuration key: " + key.name());
            }
            return (T) value;
        }
    }

    /** Adapter-owned publisher of complete, resolved configurations for one running environment. */
    final class Publisher implements ConfigFacade {
        private final Map<String, Key<?>> declarations;
        private final Consumer<? super Snapshot> validator;
        private volatile Snapshot current;

        /**
         * Fixes the declared keys and whole-snapshot validator for this instance.
         *
         * @param keys resolved keys to include in every complete snapshot
         * @param validator adapter validation which throws if the candidate is invalid
         */
        public Publisher(Collection<? extends Key<?>> keys, Consumer<? super Snapshot> validator) {
            this.validator = Objects.requireNonNull(validator, "validator");
            Map<String, Key<?>> declared = new HashMap<>();
            for (Key<?> key : keys) {
                if (declared.putIfAbsent(key.name(), key) != null) {
                    throw new IllegalArgumentException(
                            "Duplicate configuration key: " + key.name());
                }
            }
            declarations = Map.copyOf(declared);
        }

        @Override
        public Optional<Snapshot> current() {
            return Optional.ofNullable(current);
        }

        /**
         * Freezes and validates the complete candidate before replacing the current snapshot. The
         * adapter resolves defaults and supplies stable inputs for the duration of this call.
         *
         * @param values resolved values by key name
         * @throws IllegalArgumentException if the candidate has missing, unknown or mistyped keys
         */
        public void publish(Map<String, ?> values) {
            if (!declarations.keySet().equals(values.keySet())) {
                throw new IllegalArgumentException(
                        "Candidate must contain exactly the declared keys");
            }
            Map<Key<?>, Object> frozen = new HashMap<>();
            declarations.forEach(
                    (name, key) -> frozen.put(key, key.type().freeze.apply(values.get(name))));
            Snapshot candidate = new Snapshot(frozen);
            validator.accept(candidate);
            current = candidate;
        }
    }
}
