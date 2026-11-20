package com.industrialage.ia2.platform;

import java.util.Objects;
import java.util.function.Function;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;

/** Connects NeoForge events to the typed streams for this mod instance. */
public final class NeoForgeEvents extends EventStreams {
    private final IEventBus gameBus;

    /** Creates an adapter for the supplied game bus. */
    public NeoForgeEvents(IEventBus gameBus) {
        this.gameBus = Objects.requireNonNull(gameBus, "gameBus");
    }

    /** Publishes the converted host event synchronously on the delivering thread. */
    public <H extends Event, E> void forward(
            Class<H> hostType, Class<E> type, Function<? super H, ? extends E> convert) {
        Objects.requireNonNull(hostType, "hostType");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(convert, "convert");
        gameBus.addListener(hostType, event -> publish(type, convert.apply(event)));
    }
}
