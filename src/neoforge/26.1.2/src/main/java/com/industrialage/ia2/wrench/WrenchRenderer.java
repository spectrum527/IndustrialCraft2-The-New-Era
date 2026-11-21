package com.industrialage.ia2.wrench;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

/** Draws the selected wrench face from an immutable snapshot of the world outline. */
public final class WrenchRenderer {
    private WrenchRenderer() {}

    /** Installs the physical-client highlight callback. */
    public static void install(IEventBus bus) {
        bus.addListener(ExtractBlockOutlineRenderStateEvent.class, WrenchRenderer::extract);
    }

    private static void extract(ExtractBlockOutlineRenderStateEvent event) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.screen != null) return;
        var preview = WrenchUse.preview(event.getLevel(), client.player, event.getHitResult());
        if (preview == null) return;
        var pos = event.getBlockPos();
        event.addCustomRenderer(
                (outline, buffers, stack, translucent, state) -> {
                    if (outline.isTranslucent() != translucent) return false;
                    var consumer = buffers.getBuffer(RenderTypes.debugQuads());
                    preview.draw(
                            pos,
                            state.cameraRenderState.pos,
                            (point, color) ->
                                    consumer.addVertex(
                                                    stack.last(),
                                                    (float) point.x,
                                                    (float) point.y,
                                                    (float) point.z)
                                            .setColor(color));
                    buffers.endBatch(RenderTypes.debugQuads());
                    return false;
                });
    }
}
