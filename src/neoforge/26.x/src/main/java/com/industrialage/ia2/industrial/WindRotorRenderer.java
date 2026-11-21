package com.industrialage.ia2.industrial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Queues snapshots of the installed wind or water rotor for the 26.x render pipeline. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class WindRotorRenderer
        implements BlockEntityRenderer<WorkshopNode, WindRotorRenderer.State> {
    private final WindRotorModel model = new WindRotorModel();
    private final EnumMap<WindRotorMount.Material, RenderType> layers =
            new EnumMap<>(WindRotorMount.Material.class);

    private WindRotorRenderer() {
        for (var material : WindRotorMount.Material.values()) {
            var texture = Identifier.fromNamespaceAndPath("ic2", material.texture);
            layers.put(material, RenderTypes.entityCutoutCull(texture));
        }
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                WorkshopPlatform.type(), context -> new WindRotorRenderer());
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            WorkshopNode node,
            State state,
            float partialTick,
            Vec3 camera,
            CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(node, state, partialTick, camera, crumbling);
        state.kind = node.kind;
        state.material = WindRotorMount.material(node.visualRotorType());
        state.angle = model.angle(node, state.material, partialTick);
        if (state.material != null) {
            state.face = WindRotorMount.face(node);
            var level = node.getLevel();
            if (level != null) {
                var lightAt = node.getBlockPos().relative(state.face);
                state.lightCoords =
                        LightCoordsUtil.pack(
                                level.getBrightness(LightLayer.BLOCK, lightAt),
                                level.getBrightness(LightLayer.SKY, lightAt));
            }
        }
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.material == null) return;
        var parts = model.parts(state.kind, state.material);
        var layer = layers.get(state.material);

        pose.pushPose();
        WindRotorModel.translateToHub(pose, state.face);
        pose.mulPose(
                state.face.getAxis().isVertical()
                        ? Axis.XP.rotationDegrees(-90 * state.face.getStepY())
                        : Axis.YP.rotationDegrees(-state.face.toYRot()));
        collector.submitModelPart(
                parts.shaft(), pose, layer, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        pose.mulPose(Axis.ZP.rotationDegrees(state.angle));
        collector.submitModelPart(
                parts.wheel(), pose, layer, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // This flag is per renderer in 26.x. shouldRender filters the shared workshop type per
        // block.
        return true;
    }

    @Override
    public boolean shouldRender(WorkshopNode node, Vec3 camera) {
        return node.kind.hasWorldRotor() && BlockEntityRenderer.super.shouldRender(node, camera);
    }

    @Override
    public AABB getRenderBoundingBox(WorkshopNode node) {
        return WindRotorMount.bounds(node);
    }

    static final class State extends BlockEntityRenderState {
        WorkshopKind kind;
        WindRotorMount.Material material;
        Direction face;
        float angle;
    }
}
