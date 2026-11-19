package com.industrialage.ia2.platform;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.platform.ConfigFacade.Publisher;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class PlatformServicesContractTest {
    private static Object dependency(Class<?> type) {
        if (type == ConfigFacade.class) {
            return new Publisher(List.of(), snapshot -> {});
        }
        return Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] {type},
                (proxy, method, args) -> fail("Unexpected call to " + method));
    }

    private static Object[] services(Class<?>[] types) {
        return Arrays.stream(types).map(PlatformServicesContractTest::dependency).toArray();
    }

    @Test
    void platformsRetainAllSixServicesAndRejectEachMissingDependency() throws Exception {
        var constructor =
                Platform.class.getConstructor(
                        RegistryFacade.class,
                        EventFacade.class,
                        NetworkFacade.class,
                        CapabilityFacade.class,
                        ClientFacade.class,
                        ConfigFacade.class);
        var first = services(constructor.getParameterTypes());
        var second = services(constructor.getParameterTypes());
        var a = constructor.newInstance(first);
        var b = constructor.newInstance(second);
        String[] names = {"registry", "events", "network", "capabilities", "client", "config"};
        for (int i = 0; i < names.length; i++) {
            var accessor = Platform.class.getMethod(names[i]);
            assertSame(first[i], accessor.invoke(a));
            assertSame(second[i], accessor.invoke(b));
            assertSame(first[i], accessor.invoke(a));
            var missing = first.clone();
            missing[i] = null;
            var error =
                    assertThrows(
                            InvocationTargetException.class,
                            () -> constructor.newInstance(missing));
            assertInstanceOf(NullPointerException.class, error.getCause());
        }
        ((Publisher) a.config()).publish(Map.of());
        assertTrue(a.config().current().isPresent());
        assertTrue(b.config().current().isEmpty());
    }

    @Test
    void clientEvaluatesFactoryAndActionOnceInTheOpenWindow() {
        var open = new AtomicBoolean(false);
        var client = ClientFacade.physicalClient(open::get);
        var calls = new ArrayList<String>();
        Supplier<Runnable> factory =
                () -> {
                    calls.add("factory");
                    return () -> calls.add("action");
                };
        assertThrows(IllegalStateException.class, () -> client.initialize(factory));
        assertTrue(calls.isEmpty());
        open.set(true);
        assertTrue(client.initialize(factory));
        assertEquals(List.of("factory", "action"), calls);
        var failure = new IllegalArgumentException("client setup");
        Runnable broken =
                () -> {
                    throw failure;
                };
        var reported =
                assertThrows(IllegalStateException.class, () -> client.initialize(() -> broken));
        assertSame(failure, reported.getCause());
        Supplier<Runnable> brokenFactory =
                () -> {
                    throw failure;
                };
        var construction =
                assertThrows(IllegalStateException.class, () -> client.initialize(brokenFactory));
        assertSame(failure, construction.getCause());
    }

    @Test
    void dedicatedServerLoadsWithOnlyCommonAndNeverRequestsTheAction() throws Exception {
        var common = ClientFacade.class.getProtectionDomain().getCodeSource().getLocation();
        try (var loader =
                new URLClassLoader(new URL[] {common}, ClassLoader.getPlatformClassLoader())) {
            assertNull(loader.getResource("net/minecraft/client/Minecraft.class"));
            var client = Class.forName(ClientFacade.class.getName(), true, loader);
            var server = client.getMethod("dedicatedServer").invoke(null);
            Supplier<Runnable> unavailable = () -> fail("Server requested a client action");
            var initialize = client.getMethod("initialize", Supplier.class);
            assertEquals(false, initialize.invoke(server, unavailable));
        }
    }
}
