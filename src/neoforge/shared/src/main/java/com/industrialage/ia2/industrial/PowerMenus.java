package com.industrialage.ia2.industrial;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Each native menu type carries its physical layout to both sides of the connection. */
final class PowerMenus {
    private static final DeferredRegister<MenuType<?>> TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, "ic2");
    private static final Map<String, Supplier<? extends MenuType<PowerMenu>>> MENUS =
            new HashMap<>();

    private PowerMenus() {}

    static void install(IEventBus bus) {
        for (PowerKind kind : PowerKind.values()) {
            if ((kind.vesselPart() && kind != PowerKind.REACTOR_FLUID_PORT)
                    || kind == PowerKind.ITNT
                    || kind == PowerKind.REACTOR_CHAMBER
                    || kind == PowerKind.CLASSIC_NUKE) continue;
            register(kind, kind.path);
            if (kind.trader()) register(kind, kind.path + "_guest");
            if (kind == PowerKind.NUCLEAR_REACTOR) register(kind, kind.path + "_fluid");
        }
        TYPES.register(bus);
    }

    private static void register(PowerKind kind, String key) {
        MENUS.put(
                key,
                TYPES.register(
                        key,
                        () ->
                                new MenuType<>(
                                        (id, inventory) ->
                                                new PowerMenu(key, id, inventory, kind, null),
                                        net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)));
    }

    static MenuType<PowerMenu> type(String key) {
        return MENUS.get(key).get();
    }

    static void clients(IEventBus bus) {
        MENUS.values()
                .forEach(
                        type ->
                                com.industrialage.ia2.client.ClientServices.registerMenuScreen(
                                        bus, type, PowerScreen::new));
    }
}
