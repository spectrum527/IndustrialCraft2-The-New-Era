package com.industrialage.ia2.equipment.client;

import com.industrialage.ia2.equipment.ArmorEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Updates the local lightmap render state without changing the player's effects. */
@Mixin(value = LightmapRenderStateExtractor.class, remap = false)
abstract class QuantumLightmapMixin {
    @Shadow(remap = false)
    private boolean needsUpdate;

    @Unique private boolean ic2$active;

    @Inject(method = "extract", at = @At("HEAD"), remap = false)
    private void ic2$begin(LightmapRenderState state, float partialTick, CallbackInfo callback) {
        boolean active = ArmorEffects.poweredQuantum(Minecraft.getInstance().player);
        if (active != ic2$active) needsUpdate = true;
        ic2$active = active;
    }

    @Inject(method = "extract", at = @At("TAIL"), remap = false)
    private void ic2$render(LightmapRenderState state, float partialTick, CallbackInfo callback) {
        if (state.needsUpdate && ic2$active) state.nightVisionEffectIntensity = 1;
    }
}
