package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.audio.SaberAudio;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Named melee attributes and item-model binding for 1.21.1. */
final class NativeSaber {
    static final AttributeModifier ACTIVE_DAMAGE =
            new AttributeModifier(id("saber_damage"), 20, AttributeModifier.Operation.ADD_VALUE);
    static final AttributeModifier IDLE_DAMAGE =
            new AttributeModifier(id("saber_damage"), 4, AttributeModifier.Operation.ADD_VALUE);
    static final AttributeModifier SPEED =
            new AttributeModifier(id("saber_speed"), -3, AttributeModifier.Operation.ADD_VALUE);

    private NativeSaber() {}

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath("ic2", name);
    }

    static final class Client {
        static void install(IEventBus bus, IEventBus game) {
            var sound = new SaberAudio();
            game.addListener(
                    ClientTickEvent.Post.class, event -> sound.reconcile(NeoSaber::active));
            bus.addListener(
                    FMLClientSetupEvent.class,
                    event ->
                            event.enqueueWork(
                                    () ->
                                            ItemProperties.register(
                                                    BuiltInRegistries.ITEM.get(id("nano_saber")),
                                                    id("saber_active"),
                                                    (stack, world, entity, seed) ->
                                                            NeoSaber.active(stack) ? 1 : 0)));
        }
    }
}
