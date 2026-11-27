package com.industrialage.ia2.forge.client;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Retains the most recent successful status message across client resource reloads. */
final class StatusResources extends SimplePreparableReloadListener<Optional<String>> {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("ic2", "status/client.json");
    private static final Logger LOG = LoggerFactory.getLogger(StatusResources.class);
    private Optional<String> current = Optional.empty();

    Optional<String> message() {
        return current;
    }

    @Override
    protected Optional<String> prepare(ResourceManager manager, ProfilerFiller profiler) {
        try (var json = new JsonReader(manager.openAsReader(ID))) {
            json.beginObject();
            String message = null;
            while (json.hasNext()) {
                if (!"message".equals(json.nextName())) {
                    json.skipValue();
                } else {
                    if (message != null || json.peek() != JsonToken.STRING) {
                        throw new IOException("Expected a single string message");
                    }
                    message = json.nextString();
                }
            }
            json.endObject();
            if (json.peek() != JsonToken.END_DOCUMENT
                    || message == null
                    || message.isBlank()
                    || message.codePoints().anyMatch(Character::isISOControl)) {
                throw new IOException("Expected one nonempty line of status text");
            }
            return Optional.of(message);
        } catch (IOException | IllegalStateException failure) {
            LOG.warn("Failed to load IC2 client status resource {}", ID, failure);
            return Optional.empty();
        }
    }

    @Override
    protected void apply(
            Optional<String> candidate, ResourceManager manager, ProfilerFiller profiler) {
        candidate.ifPresent(
                message -> {
                    current = candidate;
                    LOG.info("Applied IC2 client status resource {} with message {}", ID, message);
                });
    }
}
