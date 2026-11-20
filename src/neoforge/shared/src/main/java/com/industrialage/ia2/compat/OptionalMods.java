package com.industrialage.ia2.compat;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

/** Registers integrations on the mod bus before the loader delivers inter-mod messages. */
public final class OptionalMods {
    private OptionalMods() {}

    /** Installs lifecycle listeners without linking either optional API. */
    public static void install(IEventBus bus) {
        bus.addListener(
                (InterModEnqueueEvent event) -> {
                    if (ModList.get().isLoaded("theoneprobe")) TopHost.enqueue();
                });
    }
}
