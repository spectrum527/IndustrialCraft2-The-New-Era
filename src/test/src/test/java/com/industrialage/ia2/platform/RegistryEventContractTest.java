package com.industrialage.ia2.platform;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegistryEventContractTest {
    @Test
    void acceptsCompleteIdsAndPreservesValueSemantics() {
        for (String value : List.of("ic2:a", "ic2:0", "ic2:machines/ore._-9")) {
            var id = new RegistryFacade.Id(value);
            assertEquals(value, id.value());
            assertEquals(id, new RegistryFacade.Id(value));
            assertEquals(id.hashCode(), new RegistryFacade.Id(value).hashCode());
        }
        assertNotEquals(new RegistryFacade.Id("ic2:a"), new RegistryFacade.Id("ic2:b"));
    }

    @Test
    void rejectsInvalidIdsAndNull() {
        for (String value :
                List.of(
                        "",
                        "ic2:",
                        "ia2:a",
                        "IC2:a",
                        "ic2:A",
                        "ic2:a b",
                        "ic2:a:b",
                        "ic2:é",
                        "ic2:a\n",
                        "prefixic2:a")) {
            assertThrows(IllegalArgumentException.class, () -> new RegistryFacade.Id(value), value);
        }
        assertThrows(NullPointerException.class, () -> new RegistryFacade.Id(null));
    }

    @Test
    void kindPreservesNameAndTypeInValueSemantics() {
        var kind = new RegistryFacade.Kind<>("item", String.class);
        assertEquals("item", kind.name());
        assertSame(String.class, kind.valueType());
        assertEquals(kind, new RegistryFacade.Kind<>("item", String.class));
        assertEquals(kind.hashCode(), new RegistryFacade.Kind<>("item", String.class).hashCode());
        assertNotEquals(kind, new RegistryFacade.Kind<>("other", String.class));
        assertNotEquals(kind, new RegistryFacade.Kind<>("item", Integer.class));
        assertEquals(" ", new RegistryFacade.Kind<>(" ", String.class).name());
    }

    @Test
    void rejectsEmptyAndNullKindComponents() {
        assertThrows(
                IllegalArgumentException.class, () -> new RegistryFacade.Kind<>("", String.class));
        assertThrows(
                NullPointerException.class, () -> new RegistryFacade.Kind<>(null, String.class));
        assertThrows(NullPointerException.class, () -> new RegistryFacade.Kind<>("item", null));
    }

    @Test
    void subscriptionIsAutoCloseableWithoutCheckedExceptions() throws Exception {
        assertTrue(AutoCloseable.class.isAssignableFrom(EventFacade.Subscription.class));
        Method close = EventFacade.Subscription.class.getMethod("close");
        assertEquals(0, close.getExceptionTypes().length);
    }

    // Compiled against the delivered interfaces: tokens, factories and values retain their types.
    @SuppressWarnings("unused")
    private static void typeContracts(RegistryFacade registry, EventFacade events) {
        RegistryFacade.Kind<Number> kind = new RegistryFacade.Kind<>("number", Number.class);
        RegistryFacade.Handle<Number> handle =
                registry.register(kind, new RegistryFacade.Id("ic2:n"), () -> 1);
        Number value = handle.get();
        EventFacade.Subscription subscription =
                events.subscribe(Number.class, (Object event) -> {});
        events.publish(Number.class, value);
        subscription.close();
    }
}
