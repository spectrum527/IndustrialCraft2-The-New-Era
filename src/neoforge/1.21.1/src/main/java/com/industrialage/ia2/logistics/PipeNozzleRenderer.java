package com.industrialage.ia2.logistics;

import com.industrialage.ia2.logistics.TransportNode.Nozzle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Draws installed pipe sockets on the native 1.21.1 client. */
@EventBusSubscriber(modid = "ic2", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PipeNozzleRenderer implements BlockEntityRenderer<TransportNode> {
    private static final RenderType BRONZE = layer("bronze");
    private static final RenderType STEEL = layer("steel");
    private final PipeNozzleModel model = new PipeNozzleModel();

    private static RenderType layer(String alloy) {
        return RenderType.entityCutout(
                ResourceLocation.fromNamespaceAndPath(
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
    public void render(
            TransportNode node,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        if (!node.hasNozzles()) return;
        var vertices = buffers.getBuffer(PipeNozzleModel.steel(node.spec) ? STEEL : BRONZE);
        int size = node.spec.size();
        for (var face : TransportNode.SIDES) {
            var mode = node.nozzle(face);
            if (mode == Nozzle.NONE) continue;
            pose.pushPose();
            PipeNozzleModel.position(
                    pose, size, face, TransportBlock.connected(node.getBlockState(), face));
            pose.mulPose(
                    face.getAxis().isVertical()
                            ? Axis.XP.rotationDegrees(-90 * face.getStepY())
                            : Axis.YP.rotationDegrees(-face.toYRot()));
            PipeNozzleModel.scale(pose, size);
            model.part(mode).render(pose, vertices, light, overlay);
            pose.popPose();
        }
    }
}
