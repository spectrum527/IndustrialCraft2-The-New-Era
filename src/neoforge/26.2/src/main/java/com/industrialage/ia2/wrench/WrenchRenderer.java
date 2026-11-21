package com.industrialage.ia2.wrench;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

/** Submits the selected wrench face alongside the physical client world outline. */
public final class WrenchRenderer {
    private WrenchRenderer() {}

    /** Installs the physical-client highlight callback. */
    public static void install(IEventBus bus) {
        bus.addListener(ExtractBlockOutlineRenderStateEvent.class, WrenchRenderer::extract);
    }

    private static void extract(ExtractBlockOutlineRenderStateEvent event) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null) return;
        var preview = WrenchUse.preview(event.getLevel(), client.player, event.getHitResult());
        if (preview == null) return;
        var pos = event.getBlockPos();
        event.addCustomRenderer(
                (outline, collector, stack, state) -> {
                    var camera = state.cameraRenderState.pos;
                    collector.submitCustomGeometry(
                            stack,
                            RenderTypes.debugQuads(),
                            (pose, consumer) ->
                                    preview.draw(
                                            pos,
                                            camera,
                                            (point, color) ->
                                                    consumer.addVertex(
                                                                    pose,
                                                                    (float) point.x,
                                                                    (float) point.y,
                                                                    (float) point.z)
                                                            .setColor(color)));
                    return false;
                });
    }
}
