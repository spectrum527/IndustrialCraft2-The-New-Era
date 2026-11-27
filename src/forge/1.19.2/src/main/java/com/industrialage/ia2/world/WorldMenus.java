package com.industrialage.ia2.world;

import java.util.EnumMap;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

final class WorldMenus {
    private static final DeferredRegister<MenuType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, "ic2");
    private static final EnumMap<WorldMachineBlock.Kind, Supplier<? extends MenuType<WorldMenu>>>
            MENUS = new EnumMap<>(WorldMachineBlock.Kind.class);
    private static final Supplier<? extends MenuType<AnalyzerMenu>> ANALYZER =
            TYPES.register(
                    "cropnalyzer",
                    () ->
                            net.minecraftforge.common.extensions.IForgeMenuType.create(
                                    (id, inventory, buffer) ->
                                            new AnalyzerMenu(
                                                    analyzer(), id, inventory, null, null, null)));
    private static final Supplier<? extends MenuType<UpgradeMenu>> UPGRADE =
            TYPES.register(
                    "advanced_upgrade",
                    () ->
                            net.minecraftforge.common.extensions.IForgeMenuType.create(
                                    (id, inventory, buffer) ->
                                            new UpgradeMenu(
                                                    upgrade(), id, inventory, null, null, null)));
    private static final Supplier<? extends MenuType<ScannerMenu>> SCANNER =
            TYPES.register(
                    "scanner",
                    () ->
                            net.minecraftforge.common.extensions.IForgeMenuType.create(
                                    (id, inventory, buffer) ->
                                            new ScannerMenu(scanner(), id, inventory, null)));

    private WorldMenus() {}

    static void install(IEventBus bus) {
        for (var kind : WorldMachineBlock.Kind.values())
            MENUS.put(
                    kind,
                    TYPES.register(
                            kind.path,
                            () ->
                                    net.minecraftforge.common.extensions.IForgeMenuType.create(
                                            (id, inventory, buffer) ->
                                                    new WorldMenu(
                                                            type(kind),
                                                            id,
                                                            inventory,
                                                            kind,
                                                            null))));
        TYPES.register(bus);
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist
                == net.minecraftforge.api.distmarker.Dist.CLIENT) clients(bus);
    }

    static MenuType<UpgradeMenu> upgrade() {
        return UPGRADE.get();
    }

    static MenuType<ScannerMenu> scanner() {
        return SCANNER.get();
    }

    static MenuType<AnalyzerMenu> analyzer() {
        return ANALYZER.get();
    }

    static MenuType<WorldMenu> type(WorldMachineBlock.Kind kind) {
        return MENUS.get(kind).get();
    }

    private static void clients(IEventBus bus) {
        MENUS.values()
                .forEach(
                        type ->
                                com.industrialage.ia2.forge.client.ForgeClient.registerMenuScreen(
                                        bus, type, WorldScreen::new));
        com.industrialage.ia2.forge.client.ForgeClient.registerMenuScreen(
                bus, ANALYZER, AnalyzerScreen::new);
        com.industrialage.ia2.forge.client.ForgeClient.registerMenuScreen(
                bus, SCANNER, ScannerScreen::new);
        com.industrialage.ia2.forge.client.ForgeClient.registerMenuScreen(
                bus, UPGRADE, UpgradeScreen::new);
    }
}
