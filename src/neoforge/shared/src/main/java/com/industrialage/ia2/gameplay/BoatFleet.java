package com.industrialage.ia2.gameplay;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The three rideable IC2 watercraft and their map-independent identities. */
final class BoatFleet {
    private static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, "ic2");
    private static final Map<Kind, DeferredHolder<EntityType<?>, EntityType<NativeBoats.Craft>>>
            TYPES_BY_KIND = new EnumMap<>(Kind.class);
    private static final Map<Kind, DeferredHolder<Item, Item>> ITEMS_BY_KIND =
            new EnumMap<>(Kind.class);
    private static DeferredHolder<Item, Item> damagedDinghy;

    private BoatFleet() {}

    static void install(IEventBus bus, DeferredRegister.Items items) {
        damagedDinghy = items.registerSimpleItem("broken_rubber_boat");
        for (var kind : Kind.values()) {
            var entity = TYPES.register(kind.path, () -> NativeBoats.type(kind));
            TYPES_BY_KIND.put(kind, entity);
            ITEMS_BY_KIND.put(
                    kind,
                    items.registerItem(
                            kind.path,
                            properties -> NativeBoats.item(properties.stacksTo(1), kind, entity)));
        }
        TYPES.register(bus);
    }

    static EntityType<NativeBoats.Craft> type(Kind kind) {
        return TYPES_BY_KIND.get(kind).get();
    }

    static Item recovery(Kind kind) {
        return ITEMS_BY_KIND.get(kind).get();
    }

    static Item wreck(Kind kind) {
        return kind == Kind.RUBBER ? damagedDinghy.get() : recovery(kind);
    }

    enum Kind {
        RUBBER("rubber_boat", "boat_rubber"),
        CARBON("carbon_boat", "boat_carbon"),
        ELECTRIC("electric_boat", "boat_electric");

        final String path;
        final String texture;

        Kind(String path, String texture) {
            this.path = path;
            this.texture = texture;
        }

        double thrust(LivingEntity rider, boolean charge) {
            return switch (this) {
                case RUBBER -> 0;
                case CARBON -> 0.016;
                case ELECTRIC -> PortableGear.boatPower(rider, charge) ? 0.042 : 0;
            };
        }
    }
}
