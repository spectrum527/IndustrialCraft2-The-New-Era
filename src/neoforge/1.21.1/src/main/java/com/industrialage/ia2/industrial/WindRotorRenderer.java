package com.industrialage.ia2.industrial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Draws the shaft and installed wind rotor in the world. */
@EventBusSubscriber(modid = "ic2", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WindRotorRenderer implements BlockEntityRenderer<WorkshopNode> {
    private final WindRotorModel model = new WindRotorModel();
    private final EnumMap<WindRotorMount.Material, RenderType> layers =
            new EnumMap<>(WindRotorMount.Material.class);

    private WindRotorRenderer() {
        for (var material : WindRotorMount.Material.values()) {
            var texture = ResourceLocation.fromNamespaceAndPath("ic2", material.texture);
            layers.put(material, RenderType.entityCutout(texture));
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
        if (node.kind != WorkshopKind.WIND_KINETIC) return;
        var material = WindRotorMount.material(node.visualRotorType());
        var angle = model.angle(node, material, partialTick);
        if (material == null || node.getLevel() == null) return;

        var face = WindRotorMount.face(node);
        var parts = model.parts(material);
        var vertices = buffers.getBuffer(layers.get(material));
        int light = LevelRenderer.getLightColor(node.getLevel(), node.getBlockPos().relative(face));
        pose.pushPose();
        WindRotorModel.translateToHub(pose, face);
        pose.mulPose(Axis.YP.rotationDegrees(-face.toYRot()));
        parts.shaft().render(pose, vertices, light, overlay);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        parts.wheel().render(pose, vertices, light, overlay);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(WorkshopNode node) {
        // The axle can be outside the current section's visible bounds while its blades are in
        // view.
        return node.kind == WorkshopKind.WIND_KINETIC;
    }

    @Override
    public boolean shouldRender(WorkshopNode node, Vec3 camera) {
        return node.kind == WorkshopKind.WIND_KINETIC
                && BlockEntityRenderer.super.shouldRender(node, camera);
    }

    @Override
    public AABB getRenderBoundingBox(WorkshopNode node) {
        return WindRotorMount.bounds(node);
    }
}
