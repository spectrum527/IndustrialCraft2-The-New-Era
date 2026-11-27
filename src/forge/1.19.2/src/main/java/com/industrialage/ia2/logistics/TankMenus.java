package com.industrialage.ia2.logistics;

import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

/** The four material tanks share the same authored fluid display. */
public final class TankMenus {
    private static final DeferredRegister<MenuType<?>> TYPES =
            DeferredRegister.create(
                    net.minecraftforge.registries.ForgeRegistries.MENU_TYPES, "ic2");
    static final Supplier<MenuType<TankMenu>> TYPE =
            TYPES.register(
                    "material_tank",
                    () ->
                            net.minecraftforge.common.extensions.IForgeMenuType.create(
                                    (id, inv, buffer) -> new TankMenu(id, inv, null, false)));
    static final Supplier<MenuType<TankMenu>> UTILITY =
            TYPES.register(
                    "tank",
                    () ->
                            net.minecraftforge.common.extensions.IForgeMenuType.create(
                                    (id, inv, buffer) -> new TankMenu(id, inv, null, true)));

    private TankMenus() {}

    static void install(IEventBus bus) {
        TYPES.register(bus);
    }

    /** Register the physical tank interface during client initialization. */
    public static void clients(IEventBus bus) {
        com.industrialage.ia2.forge.client.ForgeClient.registerMenuScreen(
                bus, TYPE, TankScreen::new);
        com.industrialage.ia2.forge.client.ForgeClient.registerMenuScreen(
                bus, UTILITY, TankScreen::new);
    }
}
