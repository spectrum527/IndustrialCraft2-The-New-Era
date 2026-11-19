package com.industrialage.ia2.network;

import com.industrialage.ia2.platform.NetworkFacade;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.ObjLongConsumer;

/**
 * Registers the two required play messages and remembers a receipt only for the current connection.
 */
public final class EchoProtocol {
    /** Version negotiated by the host before the play connection is established. */
    public static final String VERSION = "1";

    private static final NetworkFacade.Codec<Long> SEQUENCE =
            new NetworkFacade.Codec<>() {
                @Override
                public byte[] encode(Long value) {
                    return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
                }

                @Override
                public Long decode(ByteBuffer frame) {
                    if (frame.remaining() != Long.BYTES) {
                        throw new IllegalArgumentException(
                                "Echo requires one 64-bit sequence number");
                    }
                    return frame.getLong();
                }
            };

    private static final NetworkFacade.MessageType<Long> REQUEST =
            new NetworkFacade.MessageType<>(
                    "ic2:echo_request",
                    SEQUENCE,
                    Long.BYTES,
                    NetworkFacade.Direction.CLIENT_TO_SERVER);
    private static final NetworkFacade.MessageType<Long> REPLY =
            new NetworkFacade.MessageType<>(
                    "ic2:echo_reply",
                    SEQUENCE,
                    Long.BYTES,
                    NetworkFacade.Direction.SERVER_TO_CLIENT);
    private final NetworkFacade channel;
    private OptionalLong lastReply = OptionalLong.empty();

    /**
     * Installs both declarations and replies solely to the connection that supplied the request.
     */
    public EchoProtocol(NetworkFacade channel) {
        this(channel, (direction, sequence) -> {});
    }

    /** Reports completed requests and receipts to a host observer on the receiving thread. */
    public EchoProtocol(NetworkFacade channel, ObjLongConsumer<NetworkFacade.Direction> received) {
        this.channel = Objects.requireNonNull(channel);
        Objects.requireNonNull(received);
        channel.register(REQUEST);
        channel.register(REPLY);
        channel.bindReceiver(
                REQUEST,
                (value, source) -> {
                    channel.sendToClient(source, REPLY, value);
                    received.accept(REQUEST.direction(), value);
                });
        channel.bindReceiver(
                REPLY,
                (value, source) -> {
                    lastReply = OptionalLong.of(value);
                    received.accept(REPLY.direction(), value);
                });
    }

    /** Sends a sequence number over the active client connection. */
    public void request(long sequence) {
        channel.sendToServer(REQUEST, sequence);
    }

    /** Returns the last sequence received on this play connection. */
    public OptionalLong lastReply() {
        return lastReply;
    }

    /** Forgets the receipt when leaving a server or integrated world. */
    public void disconnected() {
        lastReply = OptionalLong.empty();
    }
}
