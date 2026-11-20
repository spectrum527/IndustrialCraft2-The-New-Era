package com.industrialage.ia2.machine;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.machine.BlockEntityBase.Component;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlockEntityBaseContractTest {
    private static final class Recording implements Component {
        private final String id;
        private final List<String> calls;
        private Runnable loadAction = () -> {};
        private Runnable tickAction = () -> {};
        private Runnable unloadAction = () -> {};
        private boolean dirty;

        private Recording(String id, List<String> calls) {
            this.id = id;
            this.calls = calls;
            this.dirty = id.equals("a");
        }

        public void onLoad() {
            calls.add("load " + id);
            loadAction.run();
        }

        public boolean serverTick() {
            calls.add("tick " + id);
            tickAction.run();
            return dirty;
        }

        public void onUnload() {
            calls.add("unload " + id);
            unloadAction.run();
        }
    }

    @Test
    void componentsFollowLoadedLifetimeInOrderAndReloadWithoutCatchUp() {
        var calls = new ArrayList<String>();
        var a = new Recording("a", calls);
        var b = new Recording("b", calls);
        var block = new BlockEntityBase(a, b);
        var otherCalls = new ArrayList<String>();
        var other =
                new BlockEntityBase(new Recording("c", otherCalls), new Recording("d", otherCalls));
        other.onLoad();
        assertFalse(other.serverTick());
        var otherSnapshot = List.copyOf(otherCalls);
        assertFalse(block.serverTick());
        block.onUnload();
        assertFalse(block.isLoaded());
        assertTrue(calls.isEmpty());
        block.onLoad();
        block.onLoad();
        assertTrue(block.isLoaded());
        assertTrue(block.serverTick());
        block.onUnload();
        block.onUnload();
        for (int i = 0; i < 100; i++) {
            assertFalse(block.serverTick());
        }
        assertFalse(block.isLoaded());
        block.onLoad();
        block.serverTick();
        block.onUnload();
        assertEquals(
                List.of(
                        "load a",
                        "load b",
                        "tick a",
                        "tick b",
                        "unload b",
                        "unload a",
                        "load a",
                        "load b",
                        "tick a",
                        "tick b",
                        "unload b",
                        "unload a"),
                calls);
        assertThrows(IllegalArgumentException.class, () -> new BlockEntityBase(a, a));
        assertEquals(otherSnapshot, otherCalls);
        other.onUnload();
        other.onLoad();
        assertFalse(other.serverTick());
        other.onUnload();
        assertEquals(
                List.of(
                        "load c",
                        "load d",
                        "tick c",
                        "tick d",
                        "unload d",
                        "unload c",
                        "load c",
                        "load d",
                        "tick c",
                        "tick d",
                        "unload d",
                        "unload c"),
                otherCalls);
    }

    @Test
    void loadFailureRollsBackOnlyCompletedComponentsAndAllowsReload() {
        var calls = new ArrayList<String>();
        var a = new Recording("a", calls);
        var b = new Recording("b", calls);
        var c = new Recording("c", calls);
        var d = new Recording("d", calls);
        var failure = new IllegalStateException("load failed");
        c.loadAction =
                () -> {
                    throw failure;
                };
        var block = new BlockEntityBase(a, b, c, d);
        assertSame(failure, assertThrows(IllegalStateException.class, block::onLoad));
        assertFalse(block.isLoaded());
        assertEquals(List.of("load a", "load b", "load c", "unload b", "unload a"), calls);
        block.onUnload();
        assertEquals(List.of("load a", "load b", "load c", "unload b", "unload a"), calls);
        calls.clear();
        c.loadAction = () -> {};
        block.onLoad();
        assertTrue(block.isLoaded());
        block.onUnload();
        assertEquals(
                List.of(
                        "load a",
                        "load b",
                        "load c",
                        "load d",
                        "unload d",
                        "unload c",
                        "unload b",
                        "unload a"),
                calls);
    }

    @Test
    void unloadFailureStillReleasesOthersOnceAndAllowsReload() {
        var calls = new ArrayList<String>();
        var a = new Recording("a", calls);
        var b = new Recording("b", calls);
        var c = new Recording("c", calls);
        var failure = new IllegalStateException("unload failed");
        b.unloadAction =
                () -> {
                    throw failure;
                };
        var block = new BlockEntityBase(a, b, c);
        block.onLoad();
        assertSame(failure, assertThrows(IllegalStateException.class, block::onUnload));
        assertFalse(block.isLoaded());
        var expected = List.of("load a", "load b", "load c", "unload c", "unload b", "unload a");
        assertEquals(expected, calls);
        block.onUnload();
        assertEquals(expected, calls);
        calls.clear();
        b.unloadAction = () -> {};
        block.onLoad();
        block.onUnload();
        assertEquals(expected, calls);
    }

    @Test
    void reentrantUnloadStopsCallbacksAndCleansUpOnceDuringLoadOrTick() {
        for (boolean duringLoad : new boolean[] {true, false}) {
            var calls = new ArrayList<String>();
            var a = new Recording("a", calls);
            var b = new Recording("b", calls);
            var c = new Recording("c", calls);
            var block = new BlockEntityBase(a, b, c);
            Runnable remove =
                    () -> {
                        block.onUnload();
                        assertFalse(block.isLoaded());
                    };
            if (duringLoad) {
                b.loadAction = remove;
                block.onLoad();
                assertFalse(block.isLoaded());
                assertEquals(List.of("load a", "load b", "unload b", "unload a"), calls);
            } else {
                a.dirty = false;
                b.dirty = true;
                b.tickAction = remove;
                block.onLoad();
                assertTrue(block.serverTick());
                assertFalse(block.isLoaded());
                assertEquals(
                        List.of(
                                "load a",
                                "load b",
                                "load c",
                                "tick a",
                                "tick b",
                                "unload c",
                                "unload b",
                                "unload a"),
                        calls);
            }
            var before = List.copyOf(calls);
            block.onUnload();
            assertFalse(block.serverTick());
            assertEquals(before, calls);
        }
    }
}
