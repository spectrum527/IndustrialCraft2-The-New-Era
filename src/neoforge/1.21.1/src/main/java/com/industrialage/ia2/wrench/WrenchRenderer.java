package com.industrialage.ia2.wrench;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/** Draws only the current wrench face on NeoForge's native selection render pass. */
public final class WrenchRenderer {
    private WrenchRenderer() {}

    /** Installs the physical-client highlight callback. */
    public static void install(IEventBus bus) {
        bus.addListener(RenderHighlightEvent.Block.class, WrenchRenderer::draw);
    }

    private static void draw(RenderHighlightEvent.Block event) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.screen != null) return;
        var hit = event.getTarget();
        var preview = WrenchUse.preview(client.level, client.player, hit);
        if (preview == null) return;
        var matrix = event.getPoseStack().last().pose();
        var consumer = event.getMultiBufferSource().getBuffer(RenderType.debugQuads());
        preview.draw(
                hit.getBlockPos(),
                event.getCamera().getPosition(),
                (point, color) ->
                        consumer.addVertex(
                                        matrix, (float) point.x, (float) point.y, (float) point.z)
                                .setColor(color));
    }
}
