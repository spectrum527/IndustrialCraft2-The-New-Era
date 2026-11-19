package com.industrialage.ia2.platform;

import java.util.Objects;

/**
 * Services assembled by a version adapter and injected into consumers for one running environment.
 *
 * @param registry registry service
 * @param events event service
 * @param network network service
 * @param capabilities capability service
 * @param client physical-side client initialization service
 * @param config configuration service
 */
public record Platform(
        RegistryFacade registry,
        EventFacade events,
        NetworkFacade network,
        CapabilityFacade capabilities,
        ClientFacade client,
        ConfigFacade config) {
    /**
     * Binds the supplied services for the lifetime of this platform.
     *
     * @throws NullPointerException if any service is null
     */
    public Platform {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(network, "network");
        Objects.requireNonNull(capabilities, "capabilities");
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(config, "config");
    }
}
