package com.industrialage.ia2.logistics;

import com.industrialage.ia2.logistics.TransportNode.Nozzle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Queues a snapshot of the visible pipe sockets for the 26.x render pipeline. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class PipeNozzleRenderer
        implements BlockEntityRenderer<TransportNode, PipeNozzleRenderer.State> {
    private static final RenderType BRONZE = layer("bronze");
    private static final RenderType STEEL = layer("steel");
    private final PipeNozzleModel model = new PipeNozzleModel();

    private static RenderType layer(String alloy) {
        return RenderTypes.entityCutoutCull(
                Identifier.fromNamespaceAndPath(
                        "ic2", "textures/blocks/transport/" + alloy + "/nozzle.png"));
    }

    /** Registers the client renderer for the existing pipe block entity. */
    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                TransportPlatform.type(), context -> new PipeNozzleRenderer());
    }

    @Override
    public boolean shouldRender(TransportNode node, Vec3 camera) {
        return node.hasNozzles() && BlockEntityRenderer.super.shouldRender(node, camera);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            TransportNode node,
            State state,
            float partialTick,
            Vec3 camera,
            CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(node, state, partialTick, camera, crumbling);
        state.size = node.spec.size();
        state.steel = PipeNozzleModel.steel(node.spec);
        for (var face : TransportNode.SIDES) {
            int index = face.ordinal();
            state.modes[index] = node.nozzle(face);
            state.connected[index] = TransportBlock.connected(node.getBlockState(), face);
        }
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var layer = state.steel ? STEEL : BRONZE;
        for (var face : TransportNode.SIDES) {
            int index = face.ordinal();
            var mode = state.modes[index];
            if (mode == Nozzle.NONE) continue;
            pose.pushPose();
            PipeNozzleModel.position(pose, state.size, face, state.connected[index]);
            pose.mulPose(
                    face.getAxis().isVertical()
                            ? Axis.XP.rotationDegrees(-90 * face.getStepY())
                            : Axis.YP.rotationDegrees(-face.toYRot()));
            PipeNozzleModel.scale(pose, state.size);
            collector.submitModelPart(
                    model.part(mode),
                    pose,
                    layer,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    null);
            pose.popPose();
        }
    }

    static final class State extends BlockEntityRenderState {
        final Nozzle[] modes = new Nozzle[TransportNode.SIDES.length];
        final boolean[] connected = new boolean[TransportNode.SIDES.length];
        int size;
        boolean steel;
    }
}
