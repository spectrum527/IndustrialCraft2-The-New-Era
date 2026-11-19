package com.industrialage.ia2.platform;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * Registration and sending of explicitly encoded messages.
 *
 * <p>The adapter derives direction and source from the connection. It checks registration,
 * direction, and frame length before decoding, and validates untrusted lengths before allocating.
 * The adapter requires valid fields and a final buffer position equal to the original frame length.
 * Valid messages reach their handler on the receiving logical side's host thread. The protocol
 * governs whether to discard invalid frames or disconnect.
 *
 * <p>Arguments are non-null. Sending rejects unregistered declarations, direction mismatches,
 * invalid encodings, and oversized frames. A normal return means local acceptance for sending.
 */
public interface NetworkFacade {
    /**
     * Registers a message declaration for this facade.
     *
     * @param type message declaration
     * @throws IllegalArgumentException if the ID is already registered
     */
    void register(MessageType<?> type);

    /**
     * Binds the handler for a registered declaration on its receiving logical side.
     *
     * @param type registered message declaration
     * @param handler callback receiving the decoded value and the connection-derived source
     * @param <M> message type
     * @throws IllegalArgumentException if the declaration is unregistered
     */
    <M> void bindReceiver(MessageType<M> type, BiConsumer<? super M, ? super Peer> handler);

    /**
     * Submits a client-to-server message to the client's current server connection.
     *
     * @param type registered message declaration
     * @param message message to encode
     * @param <M> message type
     * @throws IllegalArgumentException if the message fails local validation
     * @throws IllegalStateException if the connection is unavailable
     */
    <M> void sendToServer(MessageType<M> type, M message);

    /**
     * Submits a server-to-client message to a client on this facade.
     *
     * @param target receiving client
     * @param type registered message declaration
     * @param message message to encode
     * @param <M> message type
     * @throws IllegalArgumentException if the target is foreign or local validation fails
     * @throws IllegalStateException if the connection is unavailable
     */
    <M> void sendToClient(Peer target, MessageType<M> type, M message);

    /**
     * The declaration for one kind of message.
     *
     * @param id full ID matching {@code ic2:[a-z0-9/._-]+}
     * @param codec explicit codec for this message
     * @param maxFrameBytes maximum encoded frame length in bytes, including zero
     * @param direction fixed logical direction
     * @param <M> message type
     */
    record MessageType<M>(String id, Codec<M> codec, int maxFrameBytes, Direction direction) {
        private static final Pattern VALID_ID = Pattern.compile("ic2:[a-z0-9/._-]+");

        /**
         * Validates the message declaration.
         *
         * @throws NullPointerException if any reference component is null
         * @throws IllegalArgumentException if the ID is invalid or the frame limit is negative
         */
        public MessageType {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(codec, "codec");
            Objects.requireNonNull(direction, "direction");
            if (!VALID_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid IC2 message ID: " + id);
            }
            if (maxFrameBytes < 0) {
                throw new IllegalArgumentException("Negative frame byte limit: " + maxFrameBytes);
            }
        }
    }

    /**
     * Explicit field encoding for the type selected by the registered ID, without changing game
     * state.
     *
     * @param <M> message type
     */
    interface Codec<M> {
        /**
         * Encodes one complete frame.
         *
         * @param message message to encode
         * @return non-null frame bytes
         */
        byte[] encode(M message);

        /**
         * Reads and validates a message, advancing the position to the original frame length. Check
         * field lengths before allocating from them.
         *
         * @param frame bounded frame with position zero and capacity and limit equal to its length
         * @return decoded non-null message
         * @throws IllegalArgumentException if message fields are invalid
         */
        M decode(ByteBuffer frame);
    }

    /** Fixed logical direction of a message. */
    enum Direction {
        /** Client to server. */
        CLIENT_TO_SERVER,

        /** Server to client. */
        SERVER_TO_CLIENT
    }

    /** An adapter-owned remote endpoint scoped to one facade instance. */
    interface Peer {}
}
