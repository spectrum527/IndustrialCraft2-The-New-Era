package com.industrialage.ia2.wrench;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Draws only the current wrench face on Forge's native selection render pass. */
public final class WrenchRenderer {
    private WrenchRenderer() {}

    /** Installs the physical-client highlight callback. */
    public static void install(IEventBus bus) {
        bus.addListener(WrenchRenderer::draw);
    }

    private static void draw(RenderHighlightEvent.Block event) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.screen != null) return;
        var hit = event.getTarget();
        var preview = WrenchUse.preview(client.level, client.player, hit);
        if (preview == null) return;
        var matrix = event.getPoseStack().last().pose();
        var consumer = event.getMultiBufferSource().getBuffer(Marks.TYPE);
        preview.draw(
                hit.getBlockPos(),
                event.getCamera().getPosition(),
                (point, color) ->
                        consumer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                                .color(
                                        (color >> 16) & 255,
                                        (color >> 8) & 255,
                                        color & 255,
                                        color >>> 24)
                                .endVertex());
    }

    private abstract static class Marks extends RenderType {
        private static final RenderType TYPE =
                create(
                        "ic2_wrench_marks",
                        DefaultVertexFormat.POSITION_COLOR,
                        VertexFormat.Mode.QUADS,
                        256,
                        false,
                        true,
                        CompositeState.builder()
                                .setShaderState(POSITION_COLOR_SHADER)
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                .setCullState(NO_CULL)
                                .setWriteMaskState(COLOR_WRITE)
                                .createCompositeState(false));

        private Marks() {
            super(
                    "ic2_wrench_marks",
                    DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    () -> {},
                    () -> {});
        }
    }
}
