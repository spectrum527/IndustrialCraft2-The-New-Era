package com.industrialage.ia2.industrial;

import java.util.EnumMap;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Each native menu type carries its physical layout to both sides of the connection. */
final class PowerMenus {
    private static final DeferredRegister<MenuType<?>> TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, "ic2");
    private static final EnumMap<PowerKind, Supplier<? extends MenuType<PowerMenu>>> MENUS =
            new EnumMap<>(PowerKind.class);

    private PowerMenus() {}

    static void install(IEventBus bus) {
        for (PowerKind kind : PowerKind.values()) {
            if (kind.vesselPart()
                    || kind == PowerKind.REACTOR_CHAMBER
                    || kind == PowerKind.ITNT
                    || kind == PowerKind.CLASSIC_NUKE) continue;
            MENUS.put(
                    kind,
                    TYPES.register(
                            kind.path,
                            () ->
                                    new MenuType<>(
                                            (id, inventory) ->
                                                    new PowerMenu(
                                                            type(kind), id, inventory, kind, null),
                                            net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)));
        }
        TYPES.register(bus);
    }

    static MenuType<PowerMenu> type(PowerKind kind) {
        return MENUS.get(kind).get();
    }

    static void clients(IEventBus bus) {
        MENUS.values()
                .forEach(
                        type ->
                                com.industrialage.ia2.client.ClientServices.registerMenuScreen(
                                        bus, type, PowerScreen::new));
    }
}
