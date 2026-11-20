package com.industrialage.ia2.fluids;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client-side rendering of the authored sprites with the fluid tint. */
final class FluidAppearance {
    private FluidAppearance() {}

    static void install(IEventBus bus) {
        bus.addListener(
                RegisterClientExtensionsEvent.class,
                event -> {
                    for (var family : FluidContent.FAMILIES)
                        event.registerFluidType(
                                new IClientFluidTypeExtensions() {
                                    @Override
                                    public ResourceLocation getStillTexture() {
                                        return ResourceLocation.fromNamespaceAndPath(
                                                "ic2", family.spec.still());
                                    }

                                    @Override
                                    public ResourceLocation getFlowingTexture() {
                                        return ResourceLocation.fromNamespaceAndPath(
                                                "ic2", family.spec.flow());
                                    }

                                    @Override
                                    public int getTintColor() {
                                        return family.spec.argb();
                                    }
                                },
                                family.type.get());
                });
    }
}
