package com.industrialage.ia2.industrial;

import java.util.EnumMap;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ToolMenus {
    private static final DeferredRegister<MenuType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, "ic2");
    private static final EnumMap<WorkshopToolBox.Kind, Supplier<? extends MenuType<ToolBoxMenu>>>
            MENUS = new EnumMap<>(WorkshopToolBox.Kind.class);

    private ToolMenus() {}

    public static void install(IEventBus bus) {
        for (var kind : WorkshopToolBox.Kind.values())
            MENUS.put(
                    kind,
                    TYPES.register(
                            kind.path,
                            () ->
                                    net.minecraftforge.common.extensions.IForgeMenuType.create(
                                            (id, player, buffer) ->
                                                    new ToolBoxMenu(
                                                            type(kind),
                                                            id,
                                                            player,
                                                            kind,
                                                            null,
                                                            null))));
        TYPES.register(bus);
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist
                == net.minecraftforge.api.distmarker.Dist.CLIENT) clients(bus);
    }

    static MenuType<ToolBoxMenu> type(WorkshopToolBox.Kind kind) {
        return MENUS.get(kind).get();
    }

    private static void clients(IEventBus bus) {
        MENUS.values()
                .forEach(
                        type ->
                                com.industrialage.ia2.forge.client.ForgeClient.registerMenuScreen(
                                        bus, type, ToolBoxScreen::new));
    }
}
