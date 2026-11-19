package com.industrialage.ia2.config;

import com.industrialage.ia2.platform.ConfigFacade.Key;
import com.industrialage.ia2.platform.ConfigFacade.Type;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/** The server configuration declarations and their typed input domains. */
public final class ActiveConfig {
    private static final Type<Boolean> BOOLEAN = Type.scalar(Boolean.class);
    private static final Type<Integer> INTEGER = Type.scalar(Integer.class);
    private static final Type<Float> FLOAT = Type.scalar(Float.class);
    private static final Type<Double> DOUBLE = Type.scalar(Double.class);
    private static final Type<List<String>> STRINGS = Type.list(Type.scalar(String.class));
    private static final Pattern RECIPE_INPUT =
            Pattern.compile("(?:Tag:|Fluid:)?[a-z0-9_.-]+:[a-z0-9/._-]+");
    private static final Map<String, Entry<?>> ENTRIES = load();

    private ActiveConfig() {}

    /** {@return every declared server configuration entry in the specification's order} */
    public static Collection<Entry<?>> entries() {
        return ENTRIES.values();
    }

    /** {@return the stable declaration for a scalar setting of the requested class} */
    @SuppressWarnings("unchecked") // Each scalar declaration has the class of its default.
    public static <T> Key<T> scalarKey(String path, Class<T> type) {
        var entry = entry(path);
        if (entry.key().type() == STRINGS || entry.defaultValue().getClass() != type) {
            throw new IllegalArgumentException("Incompatible scalar type for " + path);
        }
        return (Key<T>) entry.key();
    }

    /** {@return the stable declaration for a list of recipe input strings} */
    @SuppressWarnings("unchecked") // STRINGS is the exact shape shared by the list declarations.
    public static Key<List<String>> stringListKey(String path) {
        var key = entry(path).key();
        if (key.type() != STRINGS) {
            throw new IllegalArgumentException("Expected a string list at " + path);
        }
        return (Key<List<String>>) key;
    }

    private static Entry<?> entry(String path) {
        var entry = ENTRIES.get(path);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown configuration path " + path);
        }
        return entry;
    }

    private static Map<String, Entry<?>> load() {
        Map<String, Entry<?>> entries = new LinkedHashMap<>();
        var source =
                Objects.requireNonNull(ActiveConfig.class.getResourceAsStream("active-keys.tsv"));
        try (var reader =
                new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))) {
            if (!"path\ttype\tdefault_json\tscope\tdomain\tmeaning".equals(reader.readLine())) {
                throw new IllegalStateException("Invalid active configuration catalog header");
            }
            for (String line; (line = reader.readLine()) != null; ) {
                var fields = line.split("\t", -1);
                var entry = parse(fields);
                var path = entry.key().name();
                if (!entry.accepts(entry.defaultValue())
                        || entries.putIfAbsent(path, entry) != null) {
                    throw new IllegalStateException(
                            "Invalid or duplicate configuration default at " + path);
                }
            }
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        if (entries.size() != 58) {
            throw new IllegalStateException("Expected 58 active server configuration entries");
        }
        return Collections.unmodifiableMap(entries);
    }

    private static Entry<?> parse(String[] fields) {
        if (fields.length != 6 || !fields[3].equals("SERVER")) {
            throw new IllegalStateException("Invalid server configuration catalog entry");
        }
        var name = fields[0];
        var literal = fields[2];
        var comment = fields[5];
        return switch (fields[1] + "/" + fields[4]) {
            case "Bool/bool" ->
                    new Entry<>(
                            name,
                            BOOLEAN,
                            parseBoolean(literal),
                            comment,
                            value -> value instanceof Boolean flag ? flag : null);
            case "Int32/int32" ->
                    new Entry<>(
                            name,
                            INTEGER,
                            Integer.valueOf(literal),
                            comment,
                            ActiveConfig::integer);
            case "Float32/finite32" ->
                    new Entry<>(
                            name,
                            FLOAT,
                            Float.valueOf(literal),
                            comment,
                            ActiveConfig::finiteFloat);
            case "Float64/finite64" ->
                    new Entry<>(
                            name,
                            DOUBLE,
                            Double.valueOf(literal),
                            comment,
                            ActiveConfig::finiteDouble);
            case "Float64/0..1" ->
                    new Entry<>(
                            name, DOUBLE, Double.valueOf(literal), comment, ActiveConfig::ratio);
            case "List<String>/recipe_inputs" ->
                    new Entry<>(
                            name,
                            STRINGS,
                            parseStringList(literal),
                            comment,
                            ActiveConfig::recipeInputs);
            default ->
                    throw new IllegalStateException(
                            "Unknown configuration value domain at " + name);
        };
    }

    private static Boolean parseBoolean(String literal) {
        return switch (literal) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalStateException("Invalid boolean default " + literal);
        };
    }

    private static List<String> parseStringList(String literal) {
        if (!literal.startsWith("[") || !literal.endsWith("]")) {
            throw new IllegalStateException("Invalid configuration list default");
        }
        var elements = literal.substring(1, literal.length() - 1).strip();
        if (elements.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(elements.split(",", -1))
                .map(String::strip)
                .map(
                        value -> {
                            if (value.length() < 2
                                    || !value.startsWith("\"")
                                    || !value.endsWith("\"")) {
                                throw new IllegalStateException("Invalid recipe input default");
                            }
                            return value.substring(1, value.length() - 1);
                        })
                .toList();
    }

    private static Integer integer(Object input) {
        if (!(input instanceof Byte
                || input instanceof Short
                || input instanceof Integer
                || input instanceof Long)) {
            return null;
        }
        long value = ((Number) input).longValue();
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE ? (int) value : null;
    }

    private static Float finiteFloat(Object input) {
        return input instanceof Number number && Float.isFinite(number.floatValue())
                ? number.floatValue()
                : null;
    }

    private static Double finiteDouble(Object input) {
        return input instanceof Number number && Double.isFinite(number.doubleValue())
                ? number.doubleValue()
                : null;
    }

    private static Double ratio(Object input) {
        var value = finiteDouble(input);
        return value != null && value >= 0 && value <= 1 ? value : null;
    }

    private static List<String> recipeInputs(Object input) {
        if (!(input instanceof List<?> elements)) {
            return null;
        }
        var result = new ArrayList<String>(elements.size());
        for (var element : elements) {
            if (!(element instanceof String text) || !RECIPE_INPUT.matcher(text).matches()) {
                return null;
            }
            result.add(text);
        }
        return List.copyOf(result);
    }

    /** One declaration with its default, path and validation for raw host values. */
    public static final class Entry<T> {
        private final Key<T> key;
        private final List<String> path;
        private final T defaultValue;
        private final String comment;
        private final Function<Object, T> decode;

        private Entry(
                String name,
                Type<T> type,
                T defaultValue,
                String comment,
                Function<Object, T> decode) {
            key = new Key<>(name, type);
            path = List.of(name.split("/"));
            this.defaultValue = defaultValue;
            this.comment = comment;
            this.decode = decode;
        }

        /** {@return the declaration used to read typed snapshot values} */
        public Key<T> key() {
            return key;
        }

        /** {@return the individual path components for a hierarchical configuration} */
        public List<String> path() {
            return path;
        }

        /** {@return the immutable default for a new configuration file} */
        public T defaultValue() {
            return defaultValue;
        }

        /** {@return the explanation used in the generated configuration file} */
        public String comment() {
            return comment;
        }

        /** {@return whether the raw value satisfies the declared type and domain} */
        public boolean accepts(Object raw) {
            return decode.apply(raw) != null;
        }

        /** {@return the immutable value in the precision and type used by common consumers} */
        public T resolve(Object raw) {
            var value = decode.apply(raw);
            if (value == null) {
                throw new IllegalArgumentException("Invalid configuration value for " + key.name());
            }
            return value;
        }
    }
}
