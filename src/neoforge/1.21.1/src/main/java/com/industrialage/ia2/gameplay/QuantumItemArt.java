package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.content.PortableEquipment;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Selects the inventory glow layer from the quantum item's stored energy. */
@EventBusSubscriber(modid = "ic2", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class QuantumItemArt {
    private QuantumItemArt() {}

    @SubscribeEvent
    public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(
                () -> {
                    var charged = ResourceLocation.fromNamespaceAndPath("ic2", "quantum_charged");
                    for (var piece : PortableEquipment.CATALOG) {
                        if (!piece.path.startsWith("quantum_")) continue;
                        var item =
                                BuiltInRegistries.ITEM.get(
                                        ResourceLocation.fromNamespaceAndPath("ic2", piece.path));
                        ItemProperties.register(
                                item,
                                charged,
                                (stack, world, holder, seed) ->
                                        PortableGear.stored(stack) > 0 ? 1 : 0);
                    }
                });
    }
}
