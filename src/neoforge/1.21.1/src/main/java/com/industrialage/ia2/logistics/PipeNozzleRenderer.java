package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec.Kind;
import com.industrialage.ia2.logistics.TransportNode.Nozzle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Draws installed pipe sockets, smoothly gliding items, and flowing fluids on 1.21.1 clients. */
@EventBusSubscriber(modid = "ic2", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PipeNozzleRenderer implements BlockEntityRenderer<TransportNode> {
    private static final RenderType BRONZE = layer("bronze");
    private static final RenderType STEEL = layer("steel");
    private final PipeNozzleModel model = new PipeNozzleModel();
    private final ItemRenderer itemRenderer;
    private final float[] itemPos = new float[3];
    private final float[] quadPos = new float[12];
    private final float[] quadUv = new float[8];

    private PipeNozzleRenderer(ItemRenderer itemRenderer) {
        this.itemRenderer = itemRenderer;
    }

    private static RenderType layer(String alloy) {
        return RenderType.entityCutout(
                ResourceLocation.fromNamespaceAndPath(
                        "ic2", "textures/blocks/transport/" + alloy + "/nozzle.png"));
    }

    /** Registers the client renderer for the existing pipe block entity. */
    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                TransportPlatform.type(),
                context -> new PipeNozzleRenderer(context.getItemRenderer()));
    }

    @Override
    public boolean shouldRender(TransportNode node, Vec3 camera) {
        return node.hasVisualRender() && BlockEntityRenderer.super.shouldRender(node, camera);
    }

    @Override
    public void render(
            TransportNode node,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        if (node.hasNozzles()) {
            renderNozzles(node, pose, buffers, light, overlay);
        }
        if (node.spec.kind() == Kind.ITEM_PIPE && !node.cargo.isEmpty()) {
            renderItemCargo(node, partialTick, pose, buffers, light, overlay);
        } else if (node.spec.kind() == Kind.FLUID_PIPE) {
            renderFluidStream(node, pose, buffers, light, overlay);
        }
    }

    private void renderNozzles(
            TransportNode node, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
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

    private void renderItemCargo(
            TransportNode node,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        PipeVisuals.itemPosition(node, partialTick, itemPos);
        float scale = PipeVisuals.itemScale(node.spec.size());
        pose.pushPose();
        pose.translate(itemPos[0], itemPos[1], itemPos[2]);
        pose.mulPose(Axis.YP.rotationDegrees(PipeVisuals.itemSpinDegrees(node, partialTick)));
        pose.scale(scale, scale, scale);
        int seed = (int) node.getBlockPos().asLong();
        itemRenderer.renderStatic(
                node.cargo,
                ItemDisplayContext.FIXED,
                light,
                overlay,
                pose,
                buffers,
                node.getLevel(),
                seed);
        pose.popPose();
    }

    private void renderFluidStream(
            TransportNode node, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float fill = PipeVisuals.smoothFluidFill(node);
        if (fill <= 0.004F || node.liquid == null) return;
        var stack = TransportPlatform.fluid(node);
        if (stack.isEmpty()) return;
        var fluid = stack.getFluid();
        var ext = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation texture = ext.getFlowingTexture(stack);
        if (texture == null) texture = ext.getStillTexture(stack);
        if (texture == null) return;
        TextureAtlasSprite sprite =
                Minecraft.getInstance()
                        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                        .apply(texture);
        boolean gas = fluid.getFluidType().isLighterThanAir();
        int tint =
                node.getLevel() != null
                        ? ext.getTintColor(
                                fluid.defaultFluidState(), node.getLevel(), node.getBlockPos())
                        : ext.getTintColor(stack);
        if ((tint & 0xFFFFFF) == 0 && (tint >>> 24) == 0) tint = 0xFFFFFFFF;
        int alpha = (tint >>> 24) & 0xFF;
        if (alpha == 0) alpha = 0xFF;
        int finalAlpha = gas ? Math.min(alpha, 0xB8) : Math.max(alpha, 0xD8);
        int red = (tint >>> 16) & 0xFF;
        int green = (tint >>> 8) & 0xFF;
        int blue = tint & 0xFF;
        int fluidLight = Math.max(light, fluid.getFluidType().getLightLevel(stack) << 4);
        VertexConsumer consumer =
                buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose entry = pose.last();
        PipeVisuals.emitFluidGeometry(
                node.getBlockState(),
                node.spec.size(),
                fill,
                gas,
                node.fluidFlowDir,
                (face, x0, y0, z0, x1, y1, z1, flowDir) ->
                        emitQuad(
                                entry,
                                consumer,
                                sprite,
                                face,
                                x0,
                                y0,
                                z0,
                                x1,
                                y1,
                                z1,
                                flowDir,
                                red,
                                green,
                                blue,
                                finalAlpha,
                                fluidLight,
                                overlay));
    }

    private void emitQuad(
            PoseStack.Pose entry,
            VertexConsumer consumer,
            TextureAtlasSprite sprite,
            Direction face,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            Direction flowDir,
            int r,
            int g,
            int b,
            int a,
            int light,
            int overlay) {
        PipeVisuals.buildQuad(face, x0, y0, z0, x1, y1, z1, flowDir, quadPos, quadUv);
        float u0 = sprite.getU0();
        float uSpan = sprite.getU1() - u0;
        float v0 = sprite.getV0();
        float vSpan = sprite.getV1() - v0;
        float nx = face.getStepX();
        float ny = face.getStepY();
        float nz = face.getStepZ();
        for (int i = 0; i < 4; i++) {
            int pi = i * 3;
            int ui = i * 2;
            consumer.addVertex(entry.pose(), quadPos[pi], quadPos[pi + 1], quadPos[pi + 2])
                    .setColor(r, g, b, a)
                    .setUv(u0 + uSpan * quadUv[ui], v0 + vSpan * quadUv[ui + 1])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(entry, nx, ny, nz);
        }
    }
}
