package com.industrialage.ia2.compat;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

/** Registers integrations on the mod bus before the loader delivers inter-mod messages. */
public final class OptionalMods {
    private OptionalMods() {}

    /** Installs lifecycle listeners without linking either optional API. */
    public static void install(IEventBus bus) {
        bus.addListener(
                (InterModEnqueueEvent event) -> {
                    if (ModList.get().isLoaded("theoneprobe")) TopHost.enqueue();
                    CuriosBack.enqueueSlot();
                });
    }
}
