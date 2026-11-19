package com.industrialage.ia2.platform;

import com.industrialage.ia2.network.MessageChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

/** Binds neutral declarations to individual required play payloads during NeoForge negotiation. */
final class NeoForgeNetwork implements MessageChannel.Transport {
    record Wire(CustomPacketPayload.Type<Wire> type, byte[] body) implements CustomPacketPayload {}

    record Binding(
            CustomPacketPayload.Type<Wire> type,
            StreamCodec<RegistryFriendlyByteBuf, Wire> codec,
            IPayloadHandler<Wire> handler) {}

    record Bindings(List<Binding> serverbound, List<Binding> clientbound) {}

    private record Endpoint(NeoForgeNetwork owner, IPayloadContext context)
            implements NetworkFacade.Peer {}

    private final MessageChannel channel = new MessageChannel(this);
    private volatile Map<String, CustomPacketPayload.Type<Wire>> types = Map.of();
    private Bindings bindings;
    private Consumer<CustomPacketPayload> clientSender;

    NeoForgeNetwork(IEventBus modBus, Dist side) {
        NeoForgeVersion.installPayloadRegistration(modBus, side, this::bindings);
    }

    MessageChannel channel() {
        return channel;
    }

    void clientSender(Consumer<CustomPacketPayload> sender) {
        clientSender = Objects.requireNonNull(sender);
    }

    private synchronized Bindings bindings() {
        if (bindings != null) {
            return bindings;
        }
        var serverbound = new ArrayList<Binding>();
        var clientbound = new ArrayList<Binding>();
        var packetTypes = new HashMap<String, CustomPacketPayload.Type<Wire>>();
        for (var message : channel.seal()) {
            CustomPacketPayload.Type<Wire> type = NeoForgeVersion.payloadType(message.id());
            packetTypes.put(message.id(), type);
            StreamCodec<RegistryFriendlyByteBuf, Wire> codec =
                    StreamCodec.of(
                            (buffer, wire) -> buffer.writeBytes(wire.body()),
                            buffer -> {
                                int size = buffer.readableBytes();
                                if (size > message.maxFrameBytes()) {
                                    throw new IllegalArgumentException("Oversized " + message.id());
                                }
                                byte[] bytes = new byte[size];
                                buffer.readBytes(bytes);
                                return new Wire(type, bytes);
                            });
            var destination =
                    message.direction() == NetworkFacade.Direction.CLIENT_TO_SERVER
                            ? serverbound
                            : clientbound;
            destination.add(
                    new Binding(
                            type, codec, (wire, source) -> receive(message.id(), wire, source)));
        }
        types = Map.copyOf(packetTypes);
        bindings = new Bindings(List.copyOf(serverbound), List.copyOf(clientbound));
        return bindings;
    }

    private void receive(String id, Wire packet, IPayloadContext context) {
        if (!context.connection().isConnected()) {
            return;
        }
        var direction =
                context.flow() == PacketFlow.SERVERBOUND
                        ? NetworkFacade.Direction.CLIENT_TO_SERVER
                        : NetworkFacade.Direction.SERVER_TO_CLIENT;
        channel.receive(id, direction, packet.body(), new Endpoint(this, context));
    }

    private Wire wire(String id, byte[] body) {
        var type = types.get(id);
        if (type == null) {
            throw new IllegalStateException("Host has not registered " + id);
        }
        return new Wire(type, body);
    }

    @Override
    public void toServer(String id, byte[] body) {
        if (clientSender == null) {
            throw new IllegalStateException("No client connection on a dedicated server");
        }
        clientSender.accept(wire(id, body));
    }

    @Override
    public void toClient(NetworkFacade.Peer target, String id, byte[] body) {
        if (!(target instanceof Endpoint peer)
                || peer.owner() != this
                || peer.context().flow() != PacketFlow.SERVERBOUND) {
            throw new IllegalArgumentException("Peer does not belong to this server channel");
        }
        if (!peer.context().connection().isConnected()) {
            throw new IllegalStateException("Peer disconnected");
        }
        PacketDistributor.sendToPlayer((ServerPlayer) peer.context().player(), wire(id, body));
    }
}
