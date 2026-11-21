package com.industrialage.ia2.gameplay;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;

/** Selects the inventory glow layer from the quantum item's stored energy. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class QuantumItemArt {
    private QuantumItemArt() {}

    @SubscribeEvent
    public static void register(RegisterConditionalItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath("ic2", "quantum_charged"), Charged.CODEC);
    }

    private record Charged() implements ConditionalItemModelProperty {
        private static final MapCodec<Charged> CODEC = MapCodec.unit(new Charged());

        @Override
        public boolean get(
                ItemStack stack,
                ClientLevel level,
                LivingEntity entity,
                int seed,
                ItemDisplayContext context) {
            return PortableGear.stored(stack) > 0;
        }

        @Override
        public MapCodec<Charged> type() {
            return CODEC;
        }
    }
}
