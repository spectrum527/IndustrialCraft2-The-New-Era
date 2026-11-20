package com.industrialage.ia2.industrial;

import java.util.EnumMap;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Menu registration lives apart from workshop processor screens. */
public final class ToolMenus {
    private static final DeferredRegister<MenuType<?>> TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, "ic2");
    private static final EnumMap<WorkshopToolBox.Kind, Supplier<? extends MenuType<ToolBoxMenu>>>
            MENUS = new EnumMap<>(WorkshopToolBox.Kind.class);

    private ToolMenus() {}

    public static void install(IEventBus bus, boolean client) {
        for (var kind : WorkshopToolBox.Kind.values())
            MENUS.put(
                    kind,
                    TYPES.register(
                            kind.path,
                            () ->
                                    new MenuType<>(
                                            (id, player) ->
                                                    new ToolBoxMenu(
                                                            type(kind),
                                                            id,
                                                            player,
                                                            kind,
                                                            null,
                                                            null),
                                            net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)));
        TYPES.register(bus);
        if (client) clients(bus);
    }

    static MenuType<ToolBoxMenu> type(WorkshopToolBox.Kind kind) {
        return MENUS.get(kind).get();
    }

    private static void clients(IEventBus bus) {
        MENUS.values()
                .forEach(
                        type ->
                                com.industrialage.ia2.client.ClientServices.registerMenuScreen(
                                        bus, type, ToolBoxScreen::new));
    }
}
