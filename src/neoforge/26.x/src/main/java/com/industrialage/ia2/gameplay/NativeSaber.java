package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.audio.SaberAudio;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Named melee attributes and held audio for 26.x. */
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
        static void install(IEventBus bus, IEventBus game) {
            var sound = new SaberAudio();
            game.addListener(
                    ClientTickEvent.Post.class, event -> sound.reconcile(NeoSaber::active));
        }
    }
}
