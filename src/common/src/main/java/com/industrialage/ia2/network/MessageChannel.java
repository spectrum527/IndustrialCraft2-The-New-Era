package com.industrialage.ia2.network;

import com.industrialage.ia2.platform.NetworkFacade;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Instance-owned IC2 message declarations. The host supplies the connection and dispatch thread.
 */
public final class MessageChannel implements NetworkFacade {
    /** Sends a complete, already bounded body over the currently available host connection. */
    public interface Transport {
        /** Sends from the physical client to its server. */
        void toServer(String id, byte[] frame);

        /** Sends from the server to a peer of this channel. */
        void toClient(Peer recipient, String id, byte[] frame);
    }

    private final Map<String, Binding<?>> bindings = new LinkedHashMap<>();
    private final Transport transport;
    private boolean sealed;

    /** Creates an empty channel for one host. */
    public MessageChannel(Transport transport) {
        this.transport = Objects.requireNonNull(transport);
    }

    @Override
    public void register(MessageType<?> type) {
        if (sealed) {
            throw new IllegalStateException("Message registration has closed");
        }
        if (bindings.putIfAbsent(type.id(), new Binding<>(type)) != null) {
            throw new IllegalArgumentException("Duplicate message " + type.id());
        }
    }

    /** Closes registration and returns the declarations for the host's negotiation event. */
    public List<MessageType<?>> seal() {
        sealed = true;
        List<MessageType<?>> types = new ArrayList<>(bindings.size());
        bindings.values().forEach(binding -> types.add(binding.type));
        return List.copyOf(types);
    }

    @Override
    public <M> void bindReceiver(MessageType<M> type, BiConsumer<? super M, ? super Peer> handler) {
        if (sealed) {
            throw new IllegalStateException("Receiver registration has closed");
        }
        var binding = binding(type);
        if (binding.receiver != null) {
            throw new IllegalArgumentException("Duplicate receiver " + type.id());
        }
        binding.receiver = Objects.requireNonNull(handler);
    }

    @Override
    public <M> void sendToServer(MessageType<M> type, M message) {
        transport.toServer(type.id(), encode(type, message, Direction.CLIENT_TO_SERVER));
    }

    @Override
    public <M> void sendToClient(Peer target, MessageType<M> type, M message) {
        Objects.requireNonNull(target);
        transport.toClient(target, type.id(), encode(type, message, Direction.SERVER_TO_CLIENT));
    }

    private <M> byte[] encode(MessageType<M> type, M value, Direction actual) {
        binding(type);
        if (type.direction() != actual) {
            throw new IllegalArgumentException("Wrong sending direction for " + type.id());
        }
        byte[] bytes = Objects.requireNonNull(type.codec().encode(Objects.requireNonNull(value)));
        if (bytes.length > type.maxFrameBytes()) {
            throw new IllegalArgumentException("Body exceeds limit for " + type.id());
        }
        return bytes;
    }

    @SuppressWarnings(
            "unchecked") // The declaration reference proves the generic type of this binding.
    private <M> Binding<M> binding(MessageType<M> type) {
        var found = bindings.get(type.id());
        if (found == null || found.type != type) {
            throw new IllegalArgumentException("Undeclared message " + type.id());
        }
        return (Binding<M>) found;
    }

    /** Delivers a host-identified message on its logical thread after complete frame validation. */
    public void receive(String id, Direction actual, byte[] frame, Peer source) {
        var binding = bindings.get(Objects.requireNonNull(id));
        if (binding == null || binding.type.direction() != actual) {
            throw new IllegalArgumentException("Unknown or wrong-direction message " + id);
        }
        binding.deliver(Objects.requireNonNull(frame), Objects.requireNonNull(source));
    }

    private static final class Binding<M> {
        final MessageType<M> type;
        BiConsumer<? super M, ? super Peer> receiver;

        Binding(MessageType<M> type) {
            this.type = type;
        }

        void deliver(byte[] frame, Peer source) {
            if (frame.length > type.maxFrameBytes() || receiver == null) {
                throw new IllegalArgumentException("Cannot receive " + type.id());
            }
            var buffer = ByteBuffer.wrap(frame).asReadOnlyBuffer();
            M value = Objects.requireNonNull(type.codec().decode(buffer));
            if (buffer.position() != frame.length) {
                throw new IllegalArgumentException("Unconsumed body for " + type.id());
            }
            receiver.accept(value, source);
        }
    }
}
