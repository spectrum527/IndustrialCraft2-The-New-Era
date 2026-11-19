package com.industrialage.ia2.gametest;

import com.industrialage.ia2.platform.NeoForgeRegistry;
import com.industrialage.ia2.platform.RegistryFacade;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.neoforge.registries.RegisterEvent;

final class PlatformRegistrationFailures {
    private PlatformRegistrationFailures() {}

    static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            if (type.isInstance(error)) {
                return;
            }
            throw error;
        }
        throw new AssertionError("Expected " + type.getName());
    }

    static void verify(RegisterEvent event, BiConsumer<String, Boolean> check) {
        var blocks = new RegistryFacade.Kind<>("block", Block.class);
        for (String scenario : List.of("null", "throws", "frozen")) {
            var bus =
                    BusBuilder.builder()
                            .setExceptionHandler((ignored, input, listeners, index, failure) -> {})
                            .build();
            var registry = new NeoForgeRegistry(bus);
            var id = new RegistryFacade.Id("ic2:qa_failed_" + scenario);
            expect(
                    IllegalArgumentException.class,
                    () ->
                            registry.register(
                                    new RegistryFacade.Kind<>("unknown", Block.class),
                                    id,
                                    () -> Blocks.STONE));
            expect(
                    IllegalArgumentException.class,
                    () ->
                            registry.register(
                                    new RegistryFacade.Kind<>("block", String.class),
                                    id,
                                    () -> "invalid"));
            var calls = new AtomicInteger();
            var handle =
                    registry.register(
                            blocks,
                            id,
                            () -> {
                                calls.incrementAndGet();
                                return switch (scenario) {
                                    case "null" -> null;
                                    case "throws" ->
                                            throw new IllegalArgumentException(
                                                    "Rejected test factory");
                                    default -> Blocks.STONE;
                                };
                            });
            expect(IllegalStateException.class, handle::get);
            check.accept("failed-factory-lazy-" + scenario, calls.get() == 0);
            expect(RuntimeException.class, () -> bus.post(event));
            expect(IllegalStateException.class, handle::get);
            expect(IllegalStateException.class, handle::get);
            expect(
                    IllegalStateException.class,
                    () -> registry.register(blocks, id, () -> Blocks.DIRT));
            check.accept(
                    "failed-without-publication-" + scenario,
                    calls.get() == 1
                            && BuiltInRegistries.BLOCK
                                    .getOptional(ResourceLocation.parse(id.value()))
                                    .isEmpty());
        }
    }
}
