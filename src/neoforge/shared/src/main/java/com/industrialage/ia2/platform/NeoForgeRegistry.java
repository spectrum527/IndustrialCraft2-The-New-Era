package com.industrialage.ia2.platform;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block and item registrations deferred to the mod event bus. */
public final class NeoForgeRegistry implements RegistryFacade {
    private static final String MOD_ID = "ic2";

    private final DeferredRegister<Block> blocks = DeferredRegister.createBlocks(MOD_ID);
    private final DeferredRegister<Item> items = DeferredRegister.createItems(MOD_ID);

    /** Attaches the instance's two registration queues to the mod bus. */
    public NeoForgeRegistry(IEventBus modBus) {
        blocks.register(modBus);
        items.register(modBus);
    }

    @Override
    public <T> Handle<T> register(Kind<T> kind, Id id, Supplier<? extends T> factory) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        return switch (kind.name()) {
            case "block" -> queue(blocks, BuiltInRegistries.BLOCK, Block.class, kind, id, factory);
            case "item" -> queue(items, BuiltInRegistries.ITEM, Item.class, kind, id, factory);
            default -> throw new IllegalArgumentException("Unknown registry kind: " + kind.name());
        };
    }

    private static <H, T> Handle<T> queue(
            DeferredRegister<H> deferred,
            Registry<H> registry,
            Class<H> hostType,
            Kind<T> kind,
            Id id,
            Supplier<? extends T> factory) {
        if (!hostType.isAssignableFrom(kind.valueType())) {
            throw new IllegalArgumentException(
                    "Incompatible type for registry kind " + kind.name());
        }

        var binding = new Binding<>(id, kind.valueType(), factory);
        var path = id.value().substring(MOD_ID.length() + 1);
        try {
            var holder =
                    deferred.register(
                            path,
                            location -> {
                                if (registry.containsKey(
                                        ResourceKey.create(registry.key(), location))) {
                                    throw new IllegalStateException(
                                            "Host registry already contains " + id.value());
                                }
                                return hostType.cast(binding.create());
                            });
            return () -> binding.read(holder);
        } catch (IllegalArgumentException duplicate) {
            // DeferredRegister rejects duplicate paths with this exception. Id validates the path.
            throw new IllegalStateException(
                    "Duplicate " + kind.name() + " registration: " + id.value(), duplicate);
        }
    }

    private static final class Binding<T> {
        private final Id id;
        private final Class<T> type;
        private Supplier<? extends T> factory;
        private T candidate;

        private Binding(Id id, Class<T> type, Supplier<? extends T> factory) {
            this.id = id;
            this.type = type;
            this.factory = factory;
        }

        private T create() {
            var create = factory;
            factory = null;
            if (create == null) {
                throw new IllegalStateException(
                        "Registration was already attempted: " + id.value());
            }
            candidate = type.cast(create.get());
            if (candidate == null) {
                throw new IllegalStateException(
                        "Registration factory returned null: " + id.value());
            }
            return candidate;
        }

        private T read(DeferredHolder<?, ?> holder) {
            if (candidate == null || !holder.isBound() || holder.get() != candidate) {
                throw new IllegalStateException("Registration is not bound: " + id.value());
            }
            return candidate;
        }
    }
}
