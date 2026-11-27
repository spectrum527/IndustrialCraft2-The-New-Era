package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.forge.ForgeRegistry;
import com.industrialage.ia2.platform.RegistryFacade;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.registries.RegisterEvent;

final class PlatformRegistrationFailures {
    private PlatformRegistrationFailures() {}

    static RuntimeException expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            if (type.isInstance(error)) {
                return error;
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
                            .setTrackPhases(false)
                            .setExceptionHandler((ignored, input, listeners, index, failure) -> {})
                            .build();
            var registry = new ForgeRegistry(bus);
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
            var rejected = expect(RuntimeException.class, () -> bus.post(event));
            if (calls.get() != 1) {
                throw new AssertionError(
                        scenario + " factory called " + calls.get() + " times", rejected);
            }
            expect(RuntimeException.class, () -> bus.post(event));
            expect(IllegalStateException.class, handle::get);
            expect(IllegalStateException.class, handle::get);
            expect(
                    IllegalStateException.class,
                    () ->
                            registry.register(
                                    blocks,
                                    new RegistryFacade.Id(id.value() + "_late"),
                                    () -> Blocks.DIRT));
            check.accept(
                    "failed-without-publication-" + scenario,
                    calls.get() == 1
                            && Registry.BLOCK
                                    .getOptional(new ResourceLocation(id.value()))
                                    .isEmpty());
        }
    }
}
