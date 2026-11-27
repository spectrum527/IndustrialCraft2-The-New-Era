package com.industrialage.ia2.network;

import static com.industrialage.ia2.platform.NetworkFacade.Direction.CLIENT_TO_SERVER;
import static com.industrialage.ia2.platform.NetworkFacade.Direction.SERVER_TO_CLIENT;
import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.platform.NetworkFacade.Codec;
import com.industrialage.ia2.platform.NetworkFacade.MessageType;
import com.industrialage.ia2.platform.NetworkFacade.Peer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class MessageChannelContractTest {
    private static final String REQUEST = "ic2:echo_request";
    private static final String REPLY = "ic2:echo_reply";

    private record Transmission(String id, byte[] body, Peer recipient) {}

    private final List<Transmission> sent = new ArrayList<>();
    private boolean connected = true;
    private final MessageChannel channel =
            new MessageChannel(
                    new MessageChannel.Transport() {
                        public void toServer(String id, byte[] frame) {
                            recordTransmission(id, frame, null);
                        }

                        public void toClient(Peer recipient, String id, byte[] frame) {
                            recordTransmission(id, frame, recipient);
                        }
                    });
    private final Peer first = new Peer() {};
    private final Peer second = new Peer() {};
    private final EchoProtocol echo = new EchoProtocol(channel);

    private void recordTransmission(String id, byte[] body, Peer recipient) {
        if (!connected) {
            throw new IllegalStateException("Test transport is disconnected");
        }
        sent.add(new Transmission(id, body, recipient));
    }

    @Test
    void unavailableTransportCannotAcknowledgeALocalSend() {
        connected = false;
        assertThrows(IllegalStateException.class, () -> echo.request(14));
        assertThrows(
                IllegalStateException.class,
                () -> channel.receive(REQUEST, CLIENT_TO_SERVER, new byte[8], first));
        assertTrue(sent.isEmpty());
        assertTrue(echo.lastReply().isEmpty());
    }

    @Test
    void echoesEverySequenceToItsSourceWithoutProducingAnotherRequest() {
        assertEquals("1", EchoProtocol.VERSION);
        channel.seal();
        assertTrue(echo.lastReply().isEmpty());
        for (long sequence :
                new long[] {0x0102030405060708L, -53, 0, Long.MIN_VALUE, Long.MAX_VALUE}) {
            var peer = sequence < 0 ? first : second;
            echo.request(sequence);
            var request = sent.remove(0);
            assertEquals(REQUEST, request.id());
            assertNull(request.recipient());
            assertArrayEquals(ByteBuffer.allocate(8).putLong(sequence).array(), request.body());
            channel.receive(REQUEST, CLIENT_TO_SERVER, request.body(), peer);
            var reply = sent.remove(0);
            assertEquals(REPLY, reply.id());
            assertSame(peer, reply.recipient());
            assertArrayEquals(request.body(), reply.body());
            channel.receive(REPLY, SERVER_TO_CLIENT, reply.body(), peer);
            assertEquals(OptionalLong.of(sequence), echo.lastReply());
            assertTrue(sent.isEmpty());
        }
        echo.disconnected();
        assertTrue(echo.lastReply().isEmpty());
    }

    @Test
    void invalidInputCannotSendAReplyOrChangeClientState() {
        for (int length : List.of(0, 7, 9)) {
            var body = new byte[length];
            assertThrows(
                    IllegalArgumentException.class,
                    () -> channel.receive(REQUEST, CLIENT_TO_SERVER, body, first));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> channel.receive(REPLY, SERVER_TO_CLIENT, body, first));
        }
        var body = new byte[8];
        assertThrows(
                IllegalArgumentException.class,
                () -> channel.receive(REQUEST, SERVER_TO_CLIENT, body, first));
        assertThrows(
                IllegalArgumentException.class,
                () -> channel.receive(REPLY, CLIENT_TO_SERVER, body, first));
        assertThrows(
                IllegalArgumentException.class,
                () -> channel.receive("ic2:absent", CLIENT_TO_SERVER, body, first));
        assertTrue(sent.isEmpty());
        assertTrue(echo.lastReply().isEmpty());
    }

    @Test
    void rejectsConflictsForeignDeclarationsAndUnreadBytesBeforeDelivery() {
        Codec<Byte> codec =
                new Codec<>() {
                    public byte[] encode(Byte value) {
                        return new byte[] {value};
                    }

                    public Byte decode(ByteBuffer frame) {
                        assertEquals(0, frame.position());
                        assertEquals(frame.capacity(), frame.limit());
                        return frame.get();
                    }
                };
        var type = new MessageType<>("ic2:fixture", codec, 2, CLIENT_TO_SERVER);
        var copy = new MessageType<>(type.id(), codec, 2, CLIENT_TO_SERVER);
        var received = new ArrayList<Byte>();
        assertThrows(IllegalArgumentException.class, () -> channel.sendToServer(type, (byte) 4));
        channel.register(type);
        channel.bindReceiver(type, (v, source) -> received.add(v));
        assertThrows(IllegalArgumentException.class, () -> channel.register(copy));
        assertThrows(
                IllegalArgumentException.class,
                () -> channel.bindReceiver(type, (v, source) -> fail()));
        assertThrows(IllegalArgumentException.class, () -> channel.sendToServer(copy, (byte) 4));
        assertThrows(
                IllegalArgumentException.class, () -> channel.sendToClient(first, type, (byte) 4));
        channel.seal();
        assertThrows(IllegalStateException.class, () -> channel.register(copy));
        assertThrows(
                IllegalArgumentException.class,
                () -> channel.receive(type.id(), CLIENT_TO_SERVER, new byte[] {4, 9}, first));
        assertTrue(received.isEmpty());
        assertTrue(sent.isEmpty());
        channel.receive(type.id(), CLIENT_TO_SERVER, new byte[] {4}, first);
        assertEquals(List.of((byte) 4), received);
    }
}
