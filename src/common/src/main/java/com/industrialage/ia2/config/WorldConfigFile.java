package com.industrialage.ia2.config;

import com.industrialage.ia2.platform.ConfigFacade;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Publishes all active settings from one saved world's file as an immutable snapshot. */
public final class WorldConfigFile implements ConfigFacade {
    /** The host validates its file format and writes defaults from the same schema. */
    public interface Format {
        /** Parses the complete text and returns a lookup over a validated candidate. */
        Function<List<String>, Object> read(Reader text);

        /** Writes a complete default file to the supplied writer. */
        void writeDefaults(Writer writer);
    }

    private final Path file;
    private final Format format;
    private final Publisher publisher =
            new Publisher(
                    ActiveConfig.entries().stream().map(ActiveConfig.Entry::key).toList(),
                    snapshot -> {});

    /** Creates an initially unpublished view scoped to this file. */
    public WorldConfigFile(Path file, Format format) {
        this.file = Objects.requireNonNull(file);
        this.format = Objects.requireNonNull(format);
    }

    /** Returns the world's active configuration path. */
    public Path path() {
        return file;
    }

    @Override
    public Optional<Snapshot> current() {
        return publisher.current();
    }

    /**
     * Reads and validates the entire file before publishing. A failed read retains the snapshot.
     */
    public void load(boolean createIfMissing) throws IOException {
        if (createIfMissing && Files.notExists(file)) {
            createDefaults();
        }
        var candidate = format.read(new StringReader(Files.readString(file)));
        var values = new HashMap<String, Object>();
        for (var entry : ActiveConfig.entries()) {
            values.put(entry.key().name(), entry.resolve(candidate.apply(entry.path())));
        }
        publisher.publish(values);
    }

    private void createDefaults() throws IOException {
        Files.createDirectories(file.getParent());
        var temporary = Files.createTempFile(file.getParent(), "ic2-config-", ".toml");
        try {
            try (var writer = Files.newBufferedWriter(temporary)) {
                format.writeDefaults(writer);
            }
            Files.move(temporary, file);
        } catch (FileAlreadyExistsException concurrentCreation) {
            // The file created by the other writer will be read and validated normally.
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
