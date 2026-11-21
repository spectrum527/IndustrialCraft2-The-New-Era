package com.industrialage.ia2.forge.client;

import com.industrialage.ia2.content.PortableEquipment;
import com.industrialage.ia2.forge.ForgeNetwork;
import com.industrialage.ia2.forge.content.ForgeContent;
import com.industrialage.ia2.forge.equipment.ForgeGear;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import com.industrialage.ia2.industrial.WorkshopScreen;
import com.industrialage.ia2.network.EchoProtocol;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Hooks the physical client into Forge's input, resource and connection lifecycles. */
public final class ForgeClient {
    private final EchoProtocol echo;
    private final StatusResources resources = new StatusResources();
    private Connection playConnection;
    private KeyMapping statusKey;
    private InputConstants.Key screenKey = InputConstants.UNKNOWN;

    private ForgeClient(EchoProtocol echo) {
        this.echo = echo;
    }

    /** Installs client listeners once after the mod has selected the physical client. */
    public static void register(
            IEventBus modBus, IEventBus gameBus, EchoProtocol echo, ForgeNetwork network) {
        var client = new ForgeClient(echo);
        network.clientConnection(() -> client.playConnection);
        modBus.addListener(client::registerKey);
        modBus.addListener(
                (RegisterClientReloadListenersEvent event) ->
                        event.registerReloadListener(client.resources));
        gameBus.addListener(client::rememberScreenKey);
        gameBus.addListener(client::onKey);
        gameBus.addListener(client::onMouseButton);
        gameBus.addListener(
                (TickEvent.ClientTickEvent event) -> {
                    if (event.phase == TickEvent.Phase.END) {
                        client.screenKey = InputConstants.UNKNOWN;
                    }
                });
        gameBus.addListener(
                (ClientPlayerNetworkEvent.LoggingIn event) -> {
                    echo.disconnected();
                    client.playConnection = event.getConnection();
                });
        gameBus.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> {
                    client.playConnection = null;
                    echo.disconnected();
                });
    }

    /** Wires machine menus and electrically powered item models to the physical client. */
    public static void registerContent(IEventBus modBus) {
        registerMenuScreen(modBus, ForgeContent::machineMenu, MachineScreen::new);
        registerMenuScreen(modBus, WorkshopPlatform::menuType, WorkshopScreen::new);
        modBus.addListener(
                (FMLClientSetupEvent event) ->
                        event.enqueueWork(
                                () -> {
                                    ItemProperties.register(
                                            ForgeContent.battery(),
                                            ResourceLocation.fromNamespaceAndPath("ic2", "charge"),
                                            (stack, world, holder, seed) ->
                                                    ForgeContent.batteryCharge(stack));
                                    var charged =
                                            ResourceLocation.fromNamespaceAndPath(
                                                    "ic2", "quantum_charged");
                                    for (var piece : PortableEquipment.CATALOG) {
                                        if (!piece.path.startsWith("quantum_")) continue;
                                        var item =
                                                ForgeRegistries.ITEMS.getValue(
                                                        ResourceLocation.fromNamespaceAndPath(
                                                                "ic2", piece.path));
                                        ItemProperties.register(
                                                item,
                                                charged,
                                                (stack, world, holder, seed) ->
                                                        ForgeGear.stored(stack) > 0 ? 1 : 0);
                                    }
                                }));
    }

    /** Registers a screen factory on the client thread after its menu type is available. */
    public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>>
            void registerMenuScreen(
                    IEventBus modBus,
                    Supplier<? extends MenuType<M>> menuType,
                    MenuScreens.ScreenConstructor<M, S> factory) {
        modBus.addListener(
                (FMLClientSetupEvent event) ->
                        event.enqueueWork(() -> MenuScreens.register(menuType.get(), factory)));
    }

    private void registerKey(RegisterKeyMappingsEvent event) {
        statusKey =
                new KeyMapping(
                        "key.ic2.open_status",
                        KeyConflictContext.IN_GAME,
                        InputConstants.UNKNOWN,
                        KeyMapping.CATEGORY_MISC);
        event.register(statusKey);
    }

    private void rememberScreenKey(ScreenEvent.KeyPressed.Pre event) {
        // A screen can close itself before Forge posts the corresponding global key event.
        screenKey = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
    }

    private void onKey(InputEvent.Key event) {
        var key = InputConstants.getKey(event.getKey(), event.getScanCode());
        boolean fromScreen = key.equals(screenKey);
        screenKey = InputConstants.UNKNOWN;
        if (!fromScreen && event.getAction() == InputConstants.PRESS) {
            openStatus(key);
        }
    }

    private void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == InputConstants.PRESS
                && openStatus(InputConstants.Type.MOUSE.getOrCreate(event.getButton()))) {
            event.setCanceled(true);
        }
    }

    private boolean openStatus(InputConstants.Key key) {
        var client = Minecraft.getInstance();
        if (client.level == null
                || client.screen != null
                || client.getOverlay() != null
                || statusKey == null
                || !statusKey.isActiveAndMatches(key)
                || !connected()) {
            return false;
        }
        var screen = new StatusScreen(resources, echo, this::connected);
        client.setScreen(screen);
        return client.screen == screen;
    }

    private boolean connected() {
        return playConnection != null && playConnection.isConnected();
    }
}
