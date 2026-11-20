package com.industrialage.ia2.industrial;

import java.util.EnumMap;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** A fixed native slot layout for each physical storage box and the industrial workbench. */
final class BoxMenus {
    private static final DeferredRegister<MenuType<?>> TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, "ic2");
    private static final EnumMap<BoxKind, Supplier<? extends MenuType<BoxMenu>>> ALL =
            new EnumMap<>(BoxKind.class);

    private BoxMenus() {}

    static void install(IEventBus bus) {
        for (var kind : BoxKind.values())
            ALL.put(
                    kind,
                    TYPES.register(
                            kind.path,
                            () ->
                                    new MenuType<>(
                                            (id, player) -> new BoxMenu(id, player, kind, null, 0),
                                            net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)));
        TYPES.register(bus);
    }

    static MenuType<BoxMenu> type(BoxKind kind) {
        return ALL.get(kind).get();
    }

    static void clients(IEventBus bus) {
        ALL.values()
                .forEach(
                        type ->
                                com.industrialage.ia2.client.ClientServices.registerMenuScreen(
                                        bus, type, BoxScreen::new));
    }
}
