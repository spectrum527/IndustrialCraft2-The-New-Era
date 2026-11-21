package com.industrialage.ia2.gameplay;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.IEventBus;

/** Named melee attributes for 26.x. */
final class NativeSaber {
    static final AttributeModifier ACTIVE_DAMAGE =
            new AttributeModifier(
                    Identifier.fromNamespaceAndPath("ic2", "saber_damage"),
                    20,
                    AttributeModifier.Operation.ADD_VALUE);
    static final AttributeModifier IDLE_DAMAGE =
            new AttributeModifier(
                    Identifier.fromNamespaceAndPath("ic2", "saber_damage"),
                    4,
                    AttributeModifier.Operation.ADD_VALUE);
    static final AttributeModifier SPEED =
            new AttributeModifier(
                    Identifier.fromNamespaceAndPath("ic2", "saber_speed"),
                    -3,
                    AttributeModifier.Operation.ADD_VALUE);

    private NativeSaber() {}

    static final class Client {
        static void install(IEventBus bus) {
            // The item model selects the active blade from the synced component.
        }
    }
}
