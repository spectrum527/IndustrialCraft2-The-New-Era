package com.industrialage.ia2.logistics;

import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The four material tanks share the same authored fluid display. */
public final class TankMenus {
    private static final DeferredRegister<MenuType<?>> TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, "ic2");
    static final Supplier<MenuType<TankMenu>> TYPE =
            TYPES.register(
                    "material_tank",
                    () ->
                            new MenuType<>(
                                    (id, inv) -> new TankMenu(id, inv, null, false),
                                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<MenuType<TankMenu>> UTILITY =
            TYPES.register(
                    "tank",
                    () ->
                            new MenuType<>(
                                    (id, inv) -> new TankMenu(id, inv, null, true),
                                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    private TankMenus() {}

    static void install(IEventBus bus) {
        TYPES.register(bus);
    }

    /** Register the physical tank interface during client initialization. */
    public static void clients(IEventBus bus) {
        com.industrialage.ia2.client.ClientServices.registerMenuScreen(bus, TYPE, TankScreen::new);
        com.industrialage.ia2.client.ClientServices.registerMenuScreen(
                bus, UTILITY, TankScreen::new);
    }
}
