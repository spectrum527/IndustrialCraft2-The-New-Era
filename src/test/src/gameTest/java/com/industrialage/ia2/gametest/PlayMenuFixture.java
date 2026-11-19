package com.industrialage.ia2.gametest;

import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.platform.NeoForgePlatform;
import com.mojang.brigadier.context.CommandContext;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Owns a test-only server menu and command for the two-process client acceptance run. */
@Mod("ic2_gametest")
public final class PlayMenuFixture implements IExtensionPoint {
    private Supplier<MenuType<SampleMenu>> type;

    /** Installs this fixture only in the explicitly selected development profile. */
    public PlayMenuFixture(IEventBus bus, ModContainer container) {
        if (!Boolean.getBoolean("ic2.qa.clientAcceptance")) {
            return;
        }
        var menus = DeferredRegister.create(Registries.MENU, "ic2_gametest");
        type =
                menus.register(
                        "session",
                        () ->
                                new MenuType<>(
                                        (id, inventory) -> new SampleMenu(type.get(), id),
                                        FeatureFlags.DEFAULT_FLAGS));
        menus.register(bus);
        container.registerExtensionPoint(PlayMenuFixture.class, this);
        NeoForge.EVENT_BUS.addListener(
                RegisterCommandsEvent.class,
                event ->
                        event.getDispatcher()
                                .register(
                                        Commands.literal("ic2accept")
                                                .requires(source -> source.hasPermission(2))
                                                .then(
                                                        Commands.literal("menu")
                                                                .executes(
                                                                        context -> {
                                                                            var player =
                                                                                    context.getSource()
                                                                                            .getPlayerOrException();
                                                                            player.openMenu(
                                                                                    new SimpleMenuProvider(
                                                                                            (id,
                                                                                                    inventory,
                                                                                                    recipient) ->
                                                                                                    new SampleMenu(
                                                                                                            type
                                                                                                                    .get(),
                                                                                                            id),
                                                                                            Component
                                                                                                    .literal(
                                                                                                            "IC2 acceptance"
                                                                                                                    + " menu")));
                                                                            return 1;
                                                                        }))
                                                .then(
                                                        Commands.literal("values")
                                                                .executes(
                                                                        PlayMenuFixture::values))));
    }

    static MenuType<SampleMenu> menuType() {
        return ModList.get()
                .getModContainerById("ic2_gametest")
                .orElseThrow()
                .getCustomExtension(PlayMenuFixture.class)
                .orElseThrow()
                .type
                .get();
    }

    private static int values(CommandContext<CommandSourceStack> context) {
        var platform =
                ModList.get()
                        .getModContainerById("ic2")
                        .orElseThrow()
                        .getCustomExtension(NeoForgePlatform.class)
                        .orElseThrow()
                        .services();
        var snapshot = platform.config().current().orElseThrow();
        double ratio =
                snapshot.get(
                        ActiveConfig.scalarKey(
                                "balance/energyRetainedInStorageBlockDrops", Double.class));
        int heat =
                snapshot.get(ActiveConfig.scalarKey("balance/fermenter/hU_per_run", Integer.class));
        context.getSource()
                .sendSuccess(
                        () -> Component.literal("IC2 checked values " + ratio + "/" + heat), false);
        return 1;
    }

    static final class SampleMenu extends AbstractContainerMenu {
        SampleMenu(MenuType<?> type, int id) {
            super(type, id);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
