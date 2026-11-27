package com.industrialage.ia2.gui;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Client-version binding for the native item and machine panel drawing operations. */
public final class PanelGraphics {
    private static final Map<String, Identifier> TEXTURES = new HashMap<>();

    private PanelGraphics() {}

    private static Identifier texture(String path) {
        return TEXTURES.computeIfAbsent(
                path,
                name -> Identifier.fromNamespaceAndPath("ic2", "textures/gui/" + name + ".png"));
    }

    /** Bind an IC2 panel to a translated origin in a live native screen. */
    public static PanelCanvas at(GuiGraphicsExtractor g, Font font, int left, int top) {
        return new PanelCanvas() {
            public void blit(
                    String gui,
                    int x,
                    int y,
                    int u,
                    int v,
                    int width,
                    int height,
                    int textureWidth,
                    int textureHeight) {
                if (width <= 0 || height <= 0) return;
                g.blit(
                        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                        texture(gui),
                        left + x,
                        top + y,
                        (float) u,
                        (float) v,
                        width,
                        height,
                        textureWidth,
                        textureHeight);
            }

            public void fluid(int id, int x, int y, int width, int height) {
                var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(id);
                if (fluid == null) return;
                var state = fluid.defaultFluidState();
                var model =
                        net.minecraft.client.Minecraft.getInstance()
                                .getModelManager()
                                .getFluidStateModelSet()
                                .get(state);
                var still = model.stillMaterial();
                if (still == null) return;
                var tint = model.fluidTintSource();
                int color = tint == null ? -1 : tint.color(state);
                if ((color & 0xFF000000) == 0 && color != 0) {
                    color |= 0xFF000000;
                }
                g.blitSprite(
                        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                        still.sprite(),
                        left + x,
                        top + y,
                        width,
                        height,
                        color);
            }

            public void fill(int x, int y, int right, int bottom, int colour) {
                g.fill(left + x, top + y, left + right, top + bottom, colour);
            }

            public void text(Component text, int x, int y, int colour, boolean center) {
                g.text(
                        font,
                        text,
                        left + x - (center ? font.width(text) / 2 : 0),
                        top + y,
                        colour,
                        false);
            }

            @Override
            public void fittedText(Component value, int centerX, int y, int maxWidth, int colour) {
                int width = font.width(value);
                if (width <= maxWidth) {
                    text(value, centerX, y, colour, true);
                    return;
                }
                float scale = (float) maxWidth / width;
                var pose = g.pose();
                pose.pushMatrix();
                try {
                    pose.translate(left + centerX, top + y);
                    pose.scale(scale);
                    g.text(font, value, -width / 2, 0, colour, false);
                } finally {
                    pose.popMatrix();
                }
            }
        };
    }
}
