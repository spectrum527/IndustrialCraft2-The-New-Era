package com.industrialage.ia2.gui;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Client-version binding for the native item and machine panel drawing operations. */
public final class PanelGraphics {
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    private PanelGraphics() {}

    private static ResourceLocation texture(String path) {
        return TEXTURES.computeIfAbsent(
                path,
                name ->
                        ResourceLocation.fromNamespaceAndPath(
                                "ic2", "textures/gui/" + name + ".png"));
    }

    /** Bind an IC2 panel to a translated origin in a live native screen. */
    public static PanelCanvas at(GuiGraphics g, Font font, int left, int top) {
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
                var appearance =
                        net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions
                                .of(fluid);
                var texture = appearance.getStillTexture();
                if (texture == null) return;
                var sprite =
                        net.minecraft.client.Minecraft.getInstance()
                                .getTextureAtlas(
                                        net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                                .apply(texture);
                int tint = appearance.getTintColor();
                float red = (tint >> 16 & 255) / 255f,
                        green = (tint >> 8 & 255) / 255f,
                        blue = (tint & 255) / 255f,
                        alpha = (tint >>> 24) / 255f;
                g.blit(left + x, top + y, 0, width, height, sprite, red, green, blue, alpha);
            }

            public void fill(int x, int y, int right, int bottom, int colour) {
                g.fill(left + x, top + y, left + right, top + bottom, colour);
            }

            public void text(Component text, int x, int y, int colour, boolean center) {
                g.drawString(
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
                pose.pushPose();
                try {
                    pose.translate(left + centerX, top + y, 0);
                    pose.scale(scale, scale, 1);
                    g.drawString(font, value, -width / 2, 0, colour, false);
                } finally {
                    pose.popPose();
                }
            }
        };
    }
}
