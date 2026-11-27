package com.industrialage.ia2.forge;

import com.industrialage.ia2.network.EchoProtocol;
import com.industrialage.ia2.network.MessageChannel;
import com.industrialage.ia2.platform.NetworkFacade;
import io.netty.handler.codec.DecoderException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Binds each common message ID to a required, versioned Forge play channel. */
public final class ForgeNetwork implements MessageChannel.Transport {
    private record Endpoint(ForgeNetwork owner, Connection connection, NetworkDirection direction)
            implements NetworkFacade.Peer {}

    private final MessageChannel declarations = new MessageChannel(this);
    private volatile Map<String, SimpleChannel> routes = Map.of();
    private Supplier<Connection> clientConnection = () -> null;

    /** Registers the host channels after consumers have declared their messages. */
    public ForgeNetwork(IEventBus modBus) {
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(this::register));
    }

    /** Returns this mod instance's loader-independent message service. */
    public MessageChannel channel() {
        return declarations;
    }

    /**
     * Installs the physical client's lookup of its current play connection during initialization.
     */
    public void clientConnection(Supplier<Connection> lookup) {
        clientConnection = Objects.requireNonNull(lookup);
    }

    private void register() {
        var registered = new HashMap<String, SimpleChannel>();
        for (var type : declarations.seal()) {
            var host =
                    NetworkRegistry.newSimpleChannel(
                            ResourceLocation.parse(type.id()),
                            () -> EchoProtocol.VERSION,
                            EchoProtocol.VERSION::equals,
                            EchoProtocol.VERSION::equals);
            var direction =
                    type.direction() == NetworkFacade.Direction.CLIENT_TO_SERVER
                            ? NetworkDirection.PLAY_TO_SERVER
                            : NetworkDirection.PLAY_TO_CLIENT;
            host.messageBuilder(byte[].class, 0, direction)
                    .encoder((body, buffer) -> buffer.writeBytes(body))
                    .decoder(buffer -> readBody(buffer, type))
                    .consumerMainThread((body, context) -> receive(type.id(), body, context.get()))
                    .add();
            registered.put(type.id(), host);
        }
        routes = Map.copyOf(registered);
    }

    private static byte[] readBody(FriendlyByteBuf buffer, NetworkFacade.MessageType<?> type) {
        int size = buffer.readableBytes();
        if (size > type.maxFrameBytes()) {
            throw new DecoderException("Oversized body for " + type.id());
        }
        byte[] body = new byte[size];
        buffer.readBytes(body);
        return body;
    }

    private void receive(String id, byte[] body, NetworkEvent.Context context) {
        var connection = context.getNetworkManager();
        var direction = context.getDirection();
        boolean serverbound = direction == NetworkDirection.PLAY_TO_SERVER;
        if (negotiated(id, connection) == null
                || (serverbound
                        ? context.getSender() == null
                        : connection != clientConnection.get())) {
            return;
        }
        var flow =
                serverbound
                        ? NetworkFacade.Direction.CLIENT_TO_SERVER
                        : NetworkFacade.Direction.SERVER_TO_CLIENT;
        try {
            declarations.receive(id, flow, body, new Endpoint(this, connection, direction));
        } catch (IllegalArgumentException invalidBody) {
            connection.disconnect(Component.literal("Invalid IC2 play message: " + id));
        }
    }

    @Override
    public void toServer(String id, byte[] body) {
        send(id, body, clientConnection.get(), NetworkDirection.PLAY_TO_SERVER);
    }

    @Override
    public void toClient(NetworkFacade.Peer target, String id, byte[] body) {
        if (!(target instanceof Endpoint peer)
                || peer.owner() != this
                || peer.direction() != NetworkDirection.PLAY_TO_SERVER) {
            throw new IllegalArgumentException("Peer does not belong to this server channel");
        }
        send(id, body, peer.connection(), NetworkDirection.PLAY_TO_CLIENT);
    }

    private void send(String id, byte[] body, Connection connection, NetworkDirection direction) {
        var host = negotiated(id, connection);
        if (host == null) {
            throw new IllegalStateException("No negotiated play connection for " + id);
        }
        host.sendTo(body, connection, direction);
    }

    private SimpleChannel negotiated(String id, Connection connection) {
        var host = routes.get(id);
        if (host == null
                || connection == null
                || !connection.isConnected()
                || !host.isRemotePresent(connection)) {
            return null;
        }
        return host;
    }
}
