package com.industrialage.ia2.forge;

import com.industrialage.ia2.platform.RegistryFacade;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

/** Defers IC2 block and item factories to their respective Forge register events. */
public final class ForgeRegistry implements RegistryFacade {
    private final DeferredRegister<Block> blocks =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "ic2");
    private final DeferredRegister<Item> items =
            DeferredRegister.create(ForgeRegistries.ITEMS, "ic2");

    /** Attaches these two registration queues to this container's mod event bus. */
    public ForgeRegistry(IEventBus modBus) {
        blocks.register(modBus);
        items.register(modBus);
    }

    @Override
    public <T> Handle<T> register(Kind<T> kind, Id id, Supplier<? extends T> factory) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(id);
        Objects.requireNonNull(factory);
        return switch (kind.name()) {
            case "block" -> queue(blocks, ForgeRegistries.BLOCKS, Block.class, kind, id, factory);
            case "item" -> queue(items, ForgeRegistries.ITEMS, Item.class, kind, id, factory);
            default -> throw new IllegalArgumentException("Unknown registry kind: " + kind.name());
        };
    }

    private static <H, T> Handle<T> queue(
            DeferredRegister<H> deferred,
            IForgeRegistry<H> registry,
            Class<H> hostType,
            Kind<T> kind,
            Id id,
            Supplier<? extends T> factory) {
        if (!hostType.isAssignableFrom(kind.valueType())) {
            throw new IllegalArgumentException(
                    "Incompatible type for registry kind " + kind.name());
        }
        var location = new ResourceLocation(id.value());
        var binding = new Binding<>(kind.valueType(), location, factory);
        try {
            var object =
                    deferred.register(
                            location.getPath(),
                            () -> {
                                if (registry.containsKey(location)) {
                                    throw new IllegalStateException(
                                            "Host registry already contains " + location);
                                }
                                return hostType.cast(binding.create());
                            });
            return () -> binding.read(object);
        } catch (IllegalArgumentException duplicate) {
            throw new IllegalStateException(
                    "Duplicate " + kind.name() + " registration: " + location, duplicate);
        }
    }

    private static final class Binding<T> {
        private final Class<T> type;
        private final ResourceLocation location;
        private Supplier<? extends T> factory;
        private volatile T candidate;

        private Binding(Class<T> type, ResourceLocation location, Supplier<? extends T> factory) {
            this.type = type;
            this.location = location;
            this.factory = factory;
        }

        private T create() {
            var once = factory;
            factory = null;
            if (once == null) {
                throw new IllegalStateException("Registration was already attempted: " + location);
            }
            candidate = type.cast(once.get());
            if (candidate == null) {
                throw new IllegalStateException("Registration factory returned null: " + location);
            }
            return candidate;
        }

        private T read(RegistryObject<?> object) {
            T value = candidate;
            if (value == null || !object.isPresent() || object.get() != value) {
                throw new IllegalStateException("Registration is not bound: " + location);
            }
            return value;
        }
    }
}
