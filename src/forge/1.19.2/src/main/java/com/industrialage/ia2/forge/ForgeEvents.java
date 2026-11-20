package com.industrialage.ia2.forge;

import com.industrialage.ia2.platform.EventStreams;
import java.util.Objects;
import java.util.function.Function;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

/** Connects Forge events to the typed streams for this mod instance. */
public final class ForgeEvents extends EventStreams {
    private final IEventBus gameBus;

    /** Creates an adapter for the supplied game bus. */
    public ForgeEvents(IEventBus gameBus) {
        this.gameBus = Objects.requireNonNull(gameBus, "gameBus");
    }

    /** Publishes the converted host event synchronously on the delivering thread. */
    public <H extends Event, E> void forward(
            Class<H> hostType, Class<E> type, Function<? super H, ? extends E> convert) {
        Objects.requireNonNull(hostType, "hostType");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(convert, "convert");
        gameBus.addListener(
                EventPriority.NORMAL,
                false,
                hostType,
                event -> publish(type, convert.apply(event)));
    }
}
