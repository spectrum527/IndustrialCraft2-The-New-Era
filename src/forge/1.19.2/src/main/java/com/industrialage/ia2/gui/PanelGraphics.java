package com.industrialage.ia2.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Client-version binding for the native item and machine panel drawing operations. */
public final class PanelGraphics {
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    private PanelGraphics() {}

    private static ResourceLocation texture(String path) {
        return TEXTURES.computeIfAbsent(
                path, name -> new ResourceLocation("ic2", "textures/gui/" + name + ".png"));
    }

    /** Bind an IC2 panel to a translated origin in a live native screen. */
    public static PanelCanvas at(PoseStack g, Font font, int left, int top) {
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
                com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, texture(gui));
                net.minecraft.client.gui.GuiComponent.blit(
                        g,
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
                var fluid = net.minecraft.core.Registry.FLUID.byId(id);
                if (fluid == null) return;
                var appearance =
                        net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(
                                fluid);
                var texture = appearance.getStillTexture();
                if (texture == null) return;
                var atlas = net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
                var sprite =
                        net.minecraft.client.Minecraft.getInstance()
                                .getTextureAtlas(atlas)
                                .apply(texture);
                int tint = appearance.getTintColor();
                float red = (tint >> 16 & 255) / 255f,
                        green = (tint >> 8 & 255) / 255f,
                        blue = (tint & 255) / 255f,
                        alpha = (tint >>> 24) / 255f;
                com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, atlas);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(red, green, blue, alpha);
                net.minecraft.client.gui.GuiComponent.blit(
                        g, left + x, top + y, 0, width, height, sprite);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }

            public void fill(int x, int y, int right, int bottom, int colour) {
                net.minecraft.client.gui.GuiComponent.fill(
                        g, left + x, top + y, left + right, top + bottom, colour);
            }

            public void text(Component text, int x, int y, int colour, boolean center) {
                font.draw(g, text, left + x - (center ? font.width(text) / 2 : 0), top + y, colour);
            }

            @Override
            public void fittedText(Component value, int centerX, int y, int maxWidth, int colour) {
                int width = font.width(value);
                if (width <= maxWidth) {
                    text(value, centerX, y, colour, true);
                    return;
                }
                float scale = (float) maxWidth / width;
                g.pushPose();
                try {
                    g.translate(left + centerX, top + y, 0);
                    g.scale(scale, scale, 1);
                    font.draw(g, value, -width / 2f, 0, colour);
                } finally {
                    g.popPose();
                }
            }
        };
    }
}
