package com.industrialage.ia2.platform;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.platform.ConfigFacade.Key;
import com.industrialage.ia2.platform.ConfigFacade.Publisher;
import com.industrialage.ia2.platform.ConfigFacade.Snapshot;
import com.industrialage.ia2.platform.ConfigFacade.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigSnapshotContractTest {
    private static final Key<Integer> COUNT = new Key<>("count", Type.scalar(Integer.class));
    private static final Key<List<List<String>>> GROUPS =
            new Key<>("groups", Type.list(Type.list(Type.scalar(String.class))));
    private final List<Key<?>> declarations = new ArrayList<>(List.of(COUNT, GROUPS));
    private final Publisher config = new Publisher(declarations, this::checkGroupCount);

    private void checkGroupCount(Snapshot candidate) {
        assertNotSame(
                candidate, config.current().orElse(null), "Candidate visible during validation");
        if (candidate.get(COUNT) < candidate.get(GROUPS).size()) {
            throw new IllegalArgumentException("Count below group count");
        }
    }

    @Test
    void declarationsRejectMutableScalarsAndDuplicateNames() {
        assertThrows(IllegalArgumentException.class, () -> Type.scalar(StringBuilder.class));
        var duplicate = new Key<>("count", Type.scalar(String.class));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Publisher(List.of(COUNT, duplicate), s -> {}));
    }

    @Test
    void publishedValuesOutliveInputsAndLaterReplacements() {
        declarations.clear();
        var inner = new ArrayList<>(List.of("oak"));
        var outer = new ArrayList<>(List.of(inner));
        var input = new HashMap<String, Object>(Map.of("count", 3, "groups", outer));
        config.publish(input);
        var old = config.current().orElseThrow();
        input.clear();
        inner.clear();
        outer.clear();
        assertThrows(UnsupportedOperationException.class, () -> old.get(GROUPS).clear());
        assertThrows(UnsupportedOperationException.class, () -> old.get(GROUPS).get(0).clear());
        var copied = new Key<>("count", COUNT.type());
        var absent = new Key<>("absent", COUNT.type());
        var wrongType = new Key<>("count", Type.scalar(String.class));
        for (var key : List.of(copied, absent, wrongType)) {
            assertThrows(IllegalArgumentException.class, () -> old.get(key));
        }
        config.publish(Map.of("count", 2, "groups", List.of()));
        var current = config.current().orElseThrow();
        assertEquals(2, current.get(COUNT));
        assertTrue(current.get(GROUPS).isEmpty());
        assertEquals(3, old.get(COUNT));
        assertEquals(List.of(List.of("oak")), old.get(GROUPS));
    }

    @Test
    void missingValuesAndInvalidReplacementsPreserveTheLastSuccessfulPublication() {
        assertTrue(config.current().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> config.publish(Map.of("count", 1)));
        assertTrue(config.current().isEmpty());
        var groups = List.of(List.of("birch"));
        config.publish(Map.of("count", 2, "groups", groups));
        var saved = config.current().orElseThrow();
        for (var rejected :
                List.of(
                        Map.of("count", 0, "groups", groups),
                        Map.of("count", "7", "groups", groups),
                        Map.of("count", 5, "groups", List.of(List.of(17))),
                        Map.of("count", 5, "groups", "unwrapped"),
                        Map.of("groups", groups),
                        Map.of("count", 5, "groups", groups, "extra", true))) {
            assertThrows(IllegalArgumentException.class, () -> config.publish(rejected));
            var current = config.current().orElseThrow();
            assertEquals(2, current.get(COUNT));
            assertEquals(groups, current.get(GROUPS));
            assertEquals(2, saved.get(COUNT));
            assertEquals(groups, saved.get(GROUPS));
        }
    }
}
