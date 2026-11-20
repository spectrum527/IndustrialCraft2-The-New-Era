package com.industrialage.ia2.industrial;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

/** Registers and tracks the two IC2 primed game entities. */
public final class ChargeTypes {
    private static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(
                    net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES, "ic2");
    static final Supplier<EntityType<ChargeNative.Charge>> INDUSTRIAL =
            TYPES.register("itnt", () -> ChargeNative.type(false));
    static final Supplier<EntityType<ChargeNative.Charge>> NUCLEAR =
            TYPES.register("nuke", () -> ChargeNative.type(true));

    private ChargeTypes() {}

    static void install(IEventBus bus) {
        TYPES.register(bus);
    }

    /** Called exclusively from the client setup path. */
    public static void installRenderer(IEventBus bus) {
        ChargeNative.Client.install(bus);
        PowerMenus.clients(bus);
    }
}
