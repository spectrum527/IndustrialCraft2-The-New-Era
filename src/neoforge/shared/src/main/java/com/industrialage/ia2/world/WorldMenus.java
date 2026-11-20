package com.industrialage.ia2.world;

import java.util.EnumMap;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

final class WorldMenus {
    private static final DeferredRegister<MenuType<?>> TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, "ic2");
    private static final EnumMap<WorldMachineBlock.Kind, Supplier<? extends MenuType<WorldMenu>>>
            MENUS = new EnumMap<>(WorldMachineBlock.Kind.class);
    private static final Supplier<? extends MenuType<AnalyzerMenu>> ANALYZER =
            TYPES.register(
                    "cropnalyzer",
                    () ->
                            new MenuType<>(
                                    (id, inventory) ->
                                            new AnalyzerMenu(
                                                    analyzer(), id, inventory, null, null, null),
                                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    private static final Supplier<? extends MenuType<UpgradeMenu>> UPGRADE =
            TYPES.register(
                    "advanced_upgrade",
                    () ->
                            new MenuType<>(
                                    (id, inventory) ->
                                            new UpgradeMenu(
                                                    upgrade(), id, inventory, null, null, null),
                                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    private static final Supplier<? extends MenuType<ScannerMenu>> SCANNER =
            TYPES.register(
                    "scanner",
                    () ->
                            new MenuType<>(
                                    (id, inventory) ->
                                            new ScannerMenu(scanner(), id, inventory, null),
                                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    private WorldMenus() {}

    static void install(IEventBus bus, boolean client) {
        for (var kind : WorldMachineBlock.Kind.values())
            MENUS.put(
                    kind,
                    TYPES.register(
                            kind.path,
                            () ->
                                    new MenuType<>(
                                            (id, inventory) ->
                                                    new WorldMenu(
                                                            type(kind), id, inventory, kind, null),
                                            net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)));
        TYPES.register(bus);
        if (client) clients(bus);
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
                                com.industrialage.ia2.client.ClientServices.registerMenuScreen(
                                        bus, type, WorldScreen::new));
        com.industrialage.ia2.client.ClientServices.registerMenuScreen(
                bus, ANALYZER, AnalyzerScreen::new);
        com.industrialage.ia2.client.ClientServices.registerMenuScreen(
                bus, SCANNER, ScannerScreen::new);
        com.industrialage.ia2.client.ClientServices.registerMenuScreen(
                bus, UPGRADE, UpgradeScreen::new);
    }
}
