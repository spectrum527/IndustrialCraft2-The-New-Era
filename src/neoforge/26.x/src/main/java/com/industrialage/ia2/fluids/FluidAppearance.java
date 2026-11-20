package com.industrialage.ia2.fluids;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;

/** Client-side rendering of the authored sprites with the fluid tint. */
final class FluidAppearance {
    private FluidAppearance() {}

    static void install(IEventBus bus) {
        bus.addListener(
                RegisterFluidModelsEvent.class,
                event -> {
                    for (var family : FluidContent.FAMILIES) {
                        var still =
                                new Material(
                                        Identifier.fromNamespaceAndPath(
                                                "ic2", family.spec.still()));
                        var flow =
                                new Material(
                                        Identifier.fromNamespaceAndPath("ic2", family.spec.flow()));
                        var model =
                                new FluidModel.Unbaked(
                                        still,
                                        flow,
                                        null,
                                        (FluidTintSource) state -> family.spec.argb());
                        event.register(model, family.source, family.flow);
                    }
                });
    }
}
