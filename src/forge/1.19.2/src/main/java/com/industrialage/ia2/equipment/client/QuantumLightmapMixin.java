package com.industrialage.ia2.equipment.client;

import com.industrialage.ia2.equipment.ArmorEffects;
import com.industrialage.ia2.equipment.QuantumLightmapColor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adjusts only the local rendered lightmap; no effect is added to the wearer. */
@Mixin(value = LightTexture.class, remap = false)
abstract class QuantumLightmapMixin {
    @Shadow(aliases = "f_109871_", remap = false)
    @Final
    private NativeImage lightPixels;

    @Shadow(aliases = "f_109870_", remap = false)
    @Final
    private DynamicTexture lightTexture;

    @Shadow(aliases = "f_109873_", remap = false)
    private boolean updateLightTexture;

    @Unique private boolean ic2$active;
    @Unique private boolean ic2$paint;

    @Inject(
            method = {"updateLightTexture(F)V", "m_109881_(F)V"},
            at = @At("HEAD"),
            remap = false)
    private void ic2$begin(float partialTick, CallbackInfo callback) {
        boolean active = ArmorEffects.poweredQuantum(Minecraft.getInstance().player);
        if (active != ic2$active) updateLightTexture = true;
        ic2$active = active;
        ic2$paint = active && updateLightTexture;
    }

    @Inject(
            method = {"updateLightTexture(F)V", "m_109881_(F)V"},
            at = @At("TAIL"),
            remap = false)
    private void ic2$render(float partialTick, CallbackInfo callback) {
        if (!ic2$paint) return;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                lightPixels.setPixelRGBA(
                        x, y, QuantumLightmapColor.brighten(lightPixels.getPixelRGBA(x, y)));
            }
        }
        lightTexture.upload();
    }
}
