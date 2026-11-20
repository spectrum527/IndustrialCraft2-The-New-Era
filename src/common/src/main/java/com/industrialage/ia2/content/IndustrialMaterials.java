package com.industrialage.ia2.content;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Passive industrial materials shared by each loader's item registry and the resource generator.
 */
public final class IndustrialMaterials {
    /**
     * Unqualified IC2 item paths, excluding the foundation items already registered by the hosts.
     */
    public static final List<String> ADDITIONAL =
            rows("industrial-materials.tsv").stream()
                    .map(id -> id.substring("ic2:".length()))
                    .toList();

    private IndustrialMaterials() {}

    /** Reads UTF-8 observation rows packaged with the industrial content, skipping the header. */
    public static List<String> rows(String file) {
        var input =
                Objects.requireNonNull(IndustrialMaterials.class.getResourceAsStream(file), file);
        try (var reader =
                new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines().skip(1).filter(line -> !line.isBlank()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(file, e);
        }
    }
}
