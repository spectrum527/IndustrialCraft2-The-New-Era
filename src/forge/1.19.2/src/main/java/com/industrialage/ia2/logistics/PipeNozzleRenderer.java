package com.industrialage.ia2.logistics;

import com.industrialage.ia2.logistics.TransportNode.Nozzle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
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

/** Draws installed pipe sockets in the Forge block atlas and water render order. */
@Mod.EventBusSubscriber(modid = "ic2", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PipeNozzleRenderer implements BlockEntityRenderer<TransportNode> {
    private static final ResourceLocation BRONZE = sprite("bronze");
    private static final ResourceLocation STEEL = sprite("steel");
    private final PipeNozzleModel model = new PipeNozzleModel();

    private static ResourceLocation sprite(String alloy) {
        return ResourceLocation.parse("ic2:blocks/transport/" + alloy + "/nozzle");
    }

    /** Adds both independently painted metal sleeves to the block atlas. */
    @SubscribeEvent
    public static void stitch(TextureStitchEvent.Pre event) {
        if (TextureAtlas.LOCATION_BLOCKS.equals(event.getAtlas().location())) {
            event.addSprite(BRONZE);
            event.addSprite(STEEL);
        }
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
        var sprite =
                Minecraft.getInstance()
                        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                        .apply(PipeNozzleModel.steel(node.spec) ? STEEL : BRONZE);
        var vertices = sprite.wrap(buffers.getBuffer(Sheets.cutoutBlockSheet()));
        int size = node.spec.size();
        for (var face : TransportNode.SIDES) {
            var mode = node.nozzle(face);
            if (mode == Nozzle.NONE) continue;
            pose.pushPose();
            PipeNozzleModel.position(
                    pose, size, face, TransportBlock.connected(node.getBlockState(), face));
            pose.mulPose(
                    face.getAxis().isVertical()
                            ? Vector3f.XP.rotationDegrees(-90 * face.getStepY())
                            : Vector3f.YP.rotationDegrees(-face.toYRot()));
            PipeNozzleModel.scale(pose, size);
            model.part(mode).render(pose, vertices, light, overlay);
            pose.popPose();
        }
    }
}
