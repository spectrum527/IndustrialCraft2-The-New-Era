package com.industrialage.ia2.platform;

import com.industrialage.ia2.network.EchoProtocol;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

/** Bindings to host APIs that differ across the supported NeoForge versions. */
public final class NeoForgeVersion {
    private NeoForgeVersion() {}

    static boolean canReload(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String fullId) {
        return new CustomPacketPayload.Type<>(ResourceLocation.parse(fullId));
    }

    /** {@return the namespaced identifier of the level's dimension} */
    public static String dimension(ServerLevel level) {
        return level.dimension().location().toString();
    }

    static void installPayloadRegistration(
            IEventBus modBus, Dist side, Supplier<NeoForgeNetwork.Bindings> bindings) {
        modBus.addListener(
                RegisterPayloadHandlersEvent.class,
                event -> {
                    var registrar =
                            event.registrar(EchoProtocol.VERSION).executesOn(HandlerThread.MAIN);
                    var packets = bindings.get();
                    for (var packet : packets.serverbound()) {
                        registrar.playToServer(packet.type(), packet.codec(), packet.handler());
                    }
                    for (var packet : packets.clientbound()) {
                        registrar.playToClient(packet.type(), packet.codec(), packet.handler());
                    }
                });
    }

    /** Host bindings used by the physical client after side selection. */
    public static final class Client {
        private Client() {}

        /** {@return an unbound key mapping active in gameplay} */
        public static KeyMapping gameplayKey(String translationKey) {
            return new KeyMapping(
                    translationKey,
                    KeyConflictContext.IN_GAME,
                    InputConstants.UNKNOWN,
                    KeyMapping.CATEGORY_MISC);
        }

        /** Opens the named resource from the currently selected client resource packs. */
        public static BufferedReader openResource(ResourceManager manager, String resourceId)
                throws IOException {
            return manager.openAsReader(ResourceLocation.parse(resourceId));
        }

        /** Registers a reload listener in this version's client setup window. */
        public static void registerReload(
                IEventBus modBus, String listenerId, PreparableReloadListener listener) {
            modBus.addListener(
                    RegisterClientReloadListenersEvent.class,
                    event -> event.registerReloadListener(listener));
        }
    }
}
