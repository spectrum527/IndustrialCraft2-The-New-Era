package com.industrialage.ia2.client;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StatusResources extends SimplePreparableReloadListener<Optional<String>> {
    static final String ID = "ic2:status/client.json";
    private static final Logger LOG = LoggerFactory.getLogger(StatusResources.class);

    @FunctionalInterface
    interface OpenResource {
        BufferedReader open(ResourceManager manager) throws IOException;
    }

    private final OpenResource openResource;
    private volatile Optional<String> message = Optional.empty();

    StatusResources(OpenResource openResource) {
        this.openResource = openResource;
    }

    Optional<String> message() {
        return message;
    }

    @Override
    protected Optional<String> prepare(ResourceManager manager, ProfilerFiller profiler) {
        try (var json = new JsonReader(openResource.open(manager))) {
            String value = null;
            json.beginObject();
            while (json.hasNext()) {
                if (!json.nextName().equals("message")) {
                    json.skipValue();
                    continue;
                }
                if (value != null || json.peek() != JsonToken.STRING) {
                    throw new IOException("Expected one string field named message");
                }
                value = json.nextString();
            }
            json.endObject();
            if (json.peek() != JsonToken.END_DOCUMENT
                    || value == null
                    || value.isBlank()
                    || value.codePoints().anyMatch(Character::isISOControl)) {
                throw new IOException("Expected a nonempty status message on one line");
            }
            return Optional.of(value);
        } catch (IOException | IllegalStateException failure) {
            LOG.warn("Failed to load IC2 client status resource {}", ID, failure);
            return Optional.empty();
        }
    }

    @Override
    protected void apply(
            Optional<String> prepared, ResourceManager manager, ProfilerFiller profiler) {
        if (prepared.isPresent()) {
            message = prepared;
            LOG.info("Applied IC2 client status resource {} with message {}", ID, prepared.get());
        }
    }
}
