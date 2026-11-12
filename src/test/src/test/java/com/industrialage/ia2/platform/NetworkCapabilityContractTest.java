package com.industrialage.ia2.platform;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.platform.CapabilityFacade.Face;
import com.industrialage.ia2.platform.CapabilityFacade.Key;
import com.industrialage.ia2.platform.NetworkFacade.Codec;
import com.industrialage.ia2.platform.NetworkFacade.Direction;
import com.industrialage.ia2.platform.NetworkFacade.MessageType;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NetworkCapabilityContractTest {
    private static final Codec<String> CODEC =
            new Codec<>() {
                public byte[] encode(String message) {
                    return new byte[0];
                }

                public String decode(ByteBuffer frame) {
                    return "";
                }
            };

    @Test
    void messageTypeAcceptsValidIdsAndZeroLimit() {
        for (String id : List.of("ia2:a", "ia2:0", "ia2:machines/ore._-9")) {
            var type = new MessageType<>(id, CODEC, 0, Direction.CLIENT_TO_SERVER);
            assertEquals(id, type.id());
            assertSame(CODEC, type.codec());
            assertEquals(0, type.maxFrameBytes());
            assertEquals(Direction.CLIENT_TO_SERVER, type.direction());
            assertEquals(type, new MessageType<>(id, CODEC, 0, Direction.CLIENT_TO_SERVER));
        }
        assertEquals(
                Integer.MAX_VALUE,
                new MessageType<>("ia2:a", CODEC, Integer.MAX_VALUE, Direction.SERVER_TO_CLIENT)
                        .maxFrameBytes());
    }

    @Test
    void messageTypeRejectsInvalidIds() {
        for (String id :
                List.of(
                        "", "ia2:", "other:a", "IA2:a", "ia2:A", "ia2:a b", "ia2:a:b", "ia2:é",
                        "ia2:a\n")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MessageType<>(id, CODEC, 1, Direction.CLIENT_TO_SERVER),
                    id);
        }
    }

    @Test
    void messageTypeRejectsNullComponentsAndNegativeLimit() {
        var direction = Direction.CLIENT_TO_SERVER;
        assertThrows(
                NullPointerException.class, () -> new MessageType<>(null, CODEC, 1, direction));
        assertThrows(
                NullPointerException.class, () -> new MessageType<>("ia2:a", null, 1, direction));
        assertThrows(NullPointerException.class, () -> new MessageType<>("ia2:a", CODEC, 1, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageType<>("ia2:a", CODEC, -1, direction));
    }

    @Test
    void keyChecksComponentsAndPreservesValueSemantics() {
        var key = new Key<>("energy", Number.class);
        assertEquals("energy", key.name());
        assertSame(Number.class, key.valueType());
        assertEquals(key, new Key<>("energy", Number.class));
        assertEquals(key.hashCode(), new Key<>("energy", Number.class).hashCode());
        assertNotEquals(key, new Key<>("other", Number.class));
        assertNotEquals(key, new Key<>("energy", Integer.class));
        assertEquals(" ", new Key<>(" ", Number.class).name());
        assertThrows(IllegalArgumentException.class, () -> new Key<>("", Number.class));
        assertThrows(NullPointerException.class, () -> new Key<>(null, Number.class));
        assertThrows(NullPointerException.class, () -> new Key<>("energy", null));
    }

    @Test
    void declaresBothDirectionsAndSixFaces() {
        assertEquals(
                Set.of(Direction.CLIENT_TO_SERVER, Direction.SERVER_TO_CLIENT),
                Set.of(Direction.values()));
        assertEquals(
                Set.of(Face.UP, Face.DOWN, Face.NORTH, Face.SOUTH, Face.WEST, Face.EAST),
                Set.of(Face.values()));
    }

    // This method checks generic signatures at compile time; no adapter behavior is asserted.
    @SuppressWarnings("unused")
    private static void typeContracts(
            NetworkFacade network,
            CapabilityFacade capabilities,
            NetworkFacade.Peer peer,
            CapabilityFacade.Target target) {
        MessageType<String> type =
                new MessageType<>("ia2:text", CODEC, 10, Direction.CLIENT_TO_SERVER);
        network.register(type);
        network.bindReceiver(type, (Object message, Object source) -> {});
        network.sendToServer(type, "message");
        network.sendToClient(peer, type, "message");
        Optional<Number> found =
                capabilities.find(new Key<>("energy", Number.class), target, Optional.empty());
        Optional<Number> sided =
                capabilities.find(new Key<>("energy", Number.class), target, Optional.of(Face.UP));
    }
}
