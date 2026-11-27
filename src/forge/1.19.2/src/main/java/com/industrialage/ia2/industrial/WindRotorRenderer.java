package com.industrialage.ia2.industrial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import java.util.EnumMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Draws the shaft and installed wind or water rotor in the world. */
@Mod.EventBusSubscriber(modid = "ic2", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WindRotorRenderer implements BlockEntityRenderer<WorkshopNode> {
    private static final EnumMap<WindRotorMount.Material, ResourceLocation> SPRITES =
            new EnumMap<>(WindRotorMount.Material.class);

    static {
        for (var material : WindRotorMount.Material.values()) {
            var texture = material.texture;
            var sprite =
                    texture.substring("textures/".length(), texture.length() - ".png".length());
            SPRITES.put(material, ResourceLocation.parse("ic2:" + sprite));
        }
    }

    private final WindRotorModel model = new WindRotorModel();

    @SubscribeEvent
    public static void stitch(TextureStitchEvent.Pre event) {
        if (TextureAtlas.LOCATION_BLOCKS.equals(event.getAtlas().location())) {
            SPRITES.values().forEach(event::addSprite);
        }
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                WorkshopPlatform.type(), context -> new WindRotorRenderer());
    }

    @Override
    public void render(
            WorkshopNode node,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight,
            int overlay) {
        if (!node.kind.hasWorldRotor()) return;
        var material = WindRotorMount.material(node.visualRotorType());
        var angle = model.angle(node, material, partialTick);
        if (material == null || node.getLevel() == null) return;

        var face = WindRotorMount.face(node);
        var parts = model.parts(node.kind, material);
        var sprite =
                Minecraft.getInstance()
                        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                        .apply(SPRITES.get(material));
        // Forge 1.19.2 flushes this atlas batch before drawing translucent terrain such as water.
        var vertices = sprite.wrap(buffers.getBuffer(Sheets.cutoutBlockSheet()));
        int light = LevelRenderer.getLightColor(node.getLevel(), node.getBlockPos().relative(face));
        pose.pushPose();
        WindRotorModel.translateToHub(pose, face);
        pose.mulPose(
                face.getAxis().isVertical()
                        ? Vector3f.XP.rotationDegrees(-90 * face.getStepY())
                        : Vector3f.YP.rotationDegrees(-face.toYRot()));
        parts.shaft().render(pose, vertices, light, overlay);
        pose.mulPose(Vector3f.ZP.rotationDegrees(angle));
        parts.wheel().render(pose, vertices, light, overlay);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(WorkshopNode node) {
        // The axle can be outside the current section's visible bounds while its blades are in
        // view.
        return node.kind.hasWorldRotor();
    }

    @Override
    public boolean shouldRender(WorkshopNode node, Vec3 camera) {
        return node.kind.hasWorldRotor() && BlockEntityRenderer.super.shouldRender(node, camera);
    }
}
