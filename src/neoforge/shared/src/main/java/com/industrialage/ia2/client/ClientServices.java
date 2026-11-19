package com.industrialage.ia2.client;

import com.industrialage.ia2.network.EchoProtocol;
import com.industrialage.ia2.platform.NeoForgeVersion.Client;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Hooks IC2 into the physical client's input, connection and resource lifecycles. */
public final class ClientServices {
    private static final String STATUS_KEY = "key.ic2.open_status";
    private static final String STATUS_LISTENER = "ic2:client_status";

    private final EchoProtocol echo;
    private final StatusResources resources =
            new StatusResources(manager -> Client.openResource(manager, StatusResources.ID));
    private final KeyMapping binding = Client.gameplayKey(STATUS_KEY);
    private InputConstants.Key screenKey = InputConstants.UNKNOWN;

    private ClientServices(EchoProtocol echo) {
        this.echo = echo;
    }

    /** Installs one client's bindings and supplies the negotiated play transport to the runtime. */
    public static void install(
            IEventBus modBus,
            IEventBus gameBus,
            EchoProtocol echo,
            Consumer<Consumer<CustomPacketPayload>> installSender) {
        var services = new ClientServices(echo);
        installSender.accept(ClientServices::send);
        modBus.addListener(
                RegisterKeyMappingsEvent.class, event -> event.register(services.binding));
        Client.registerReload(modBus, STATUS_LISTENER, services.resources);
        gameBus.addListener(ScreenEvent.KeyPressed.Pre.class, services::rememberScreenInput);
        gameBus.addListener(InputEvent.Key.class, services::onKey);
        gameBus.addListener(InputEvent.MouseButton.Pre.class, services::onMouseButton);
        gameBus.addListener(
                ClientTickEvent.Post.class, event -> services.screenKey = InputConstants.UNKNOWN);
        gameBus.addListener(
                ClientPlayerNetworkEvent.LoggingOut.class, event -> echo.disconnected());
    }

    /** Registers a container screen factory when its menu type has been bound by the host. */
    public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>>
            void registerMenuScreen(
                    IEventBus modBus,
                    Supplier<? extends MenuType<M>> menuType,
                    MenuScreens.ScreenConstructor<M, S> screen) {
        modBus.addListener(
                RegisterMenuScreensEvent.class, event -> event.register(menuType.get(), screen));
    }

    private void rememberScreenInput(ScreenEvent.KeyPressed.Pre event) {
        // A screen can close itself before the global input event reaches us.
        screenKey = keyboardKey(event.getKeyCode(), event.getScanCode());
    }

    private void onKey(InputEvent.Key event) {
        var key = keyboardKey(event.getKey(), event.getScanCode());
        boolean fromScreen = screenKey.equals(key);
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

    private static InputConstants.Key keyboardKey(int code, int scanCode) {
        return code == InputConstants.UNKNOWN.getValue()
                ? InputConstants.Type.SCANCODE.getOrCreate(scanCode)
                : InputConstants.Type.KEYSYM.getOrCreate(code);
    }

    private boolean openStatus(InputConstants.Key key) {
        var client = Minecraft.getInstance();
        if (client.screen != null
                || client.getOverlay() != null
                || client.level == null
                || !binding.isActiveAndMatches(key)
                || !connected(client.getConnection())) {
            return false;
        }
        var screen = new StatusScreen(resources, echo);
        client.setScreen(screen);
        return client.screen == screen;
    }

    static boolean connected(ClientPacketListener listener) {
        return listener != null && listener.isAcceptingMessages();
    }

    private static void send(CustomPacketPayload payload) {
        var listener = Minecraft.getInstance().getConnection();
        if (!connected(listener) || !listener.hasChannel(payload)) {
            throw new IllegalStateException("IC2 play channel is unavailable");
        }
        listener.send(payload);
    }
}
