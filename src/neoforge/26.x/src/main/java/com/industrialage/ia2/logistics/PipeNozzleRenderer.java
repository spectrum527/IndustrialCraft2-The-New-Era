package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec.Kind;
import com.industrialage.ia2.logistics.TransportNode.Nozzle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Queues snapshots of installed pipe sockets, smoothly gliding items, and flowing fluids for the
 * 26.x render pipeline.
 */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class PipeNozzleRenderer
        implements BlockEntityRenderer<TransportNode, PipeNozzleRenderer.State> {
    private static final RenderType BRONZE = layer("bronze");
    private static final RenderType STEEL = layer("steel");
    private static final RenderType FLUID_LAYER =
            RenderTypes.entityTranslucentCullItemTarget(TextureAtlas.LOCATION_BLOCKS);
    private final PipeNozzleModel model = new PipeNozzleModel();
    private final ItemModelResolver itemModelResolver;
    private final float[] itemPos = new float[3];

    private PipeNozzleRenderer(ItemModelResolver itemModelResolver) {
        this.itemModelResolver = itemModelResolver;
    }

    private static RenderType layer(String alloy) {
        return RenderTypes.entityCutoutCull(
                Identifier.fromNamespaceAndPath(
                        "ic2", "textures/blocks/transport/" + alloy + "/nozzle.png"));
    }

    /** Registers the client renderer for the existing pipe block entity. */
    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                TransportPlatform.type(),
                context -> new PipeNozzleRenderer(context.itemModelResolver()));
    }

    @Override
    public boolean shouldRender(TransportNode node, Vec3 camera) {
        return node.hasVisualRender() && BlockEntityRenderer.super.shouldRender(node, camera);
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
        state.pipeState = node.getBlockState();
        state.hasNozzles = node.hasNozzles();
        state.hasItem = false;
        state.hasFluid = false;
        state.itemState.clear();
        state.fluidFlowingSprite = null;
        state.fluidStillSprite = null;

        if (state.hasNozzles) {
            for (var face : TransportNode.SIDES) {
                int index = face.ordinal();
                state.modes[index] = node.nozzle(face);
                state.connected[index] = TransportBlock.connected(state.pipeState, face);
            }
        }

        if (node.spec.kind() == Kind.ITEM_PIPE && !node.cargo.isEmpty()) {
            itemModelResolver.updateForTopItem(
                    state.itemState,
                    node.cargo,
                    ItemDisplayContext.FIXED,
                    node.getLevel(),
                    null,
                    (int) node.getBlockPos().asLong());
            if (!state.itemState.isEmpty()) {
                PipeVisuals.itemPosition(node, partialTick, itemPos);
                state.itemX = itemPos[0];
                state.itemY = itemPos[1];
                state.itemZ = itemPos[2];
                state.itemScale = PipeVisuals.itemScale(state.size);
                state.itemSpin = PipeVisuals.itemSpinDegrees(node, partialTick);
                state.hasItem = true;
            }
        } else if (node.spec.kind() == Kind.FLUID_PIPE) {
            float fill = PipeVisuals.smoothFluidFill(node);
            if (fill > 0.004F && node.liquid != null) {
                var stack = TransportPlatform.fluid(node);
                if (!stack.isEmpty()) {
                    var fluid = stack.getFluid();
                    var fluidState = fluid.defaultFluidState();
                    var fluidModel =
                            Minecraft.getInstance()
                                    .getModelManager()
                                    .getFluidStateModelSet()
                                    .get(fluidState);
                    var flowingMat = fluidModel.flowingMaterial();
                    var stillMat = fluidModel.stillMaterial();
                    if (flowingMat == null) flowingMat = stillMat;
                    if (stillMat == null) stillMat = flowingMat;
                    if (flowingMat != null) {
                        state.fluidFlowingSprite = flowingMat.sprite();
                        state.fluidStillSprite = stillMat.sprite();
                        state.fluidFlowUvScale =
                                state.fluidFlowingSprite.contents().width()
                                                > state.fluidStillSprite.contents().width()
                                        ? 0.5F
                                        : 1.0F;
                        int tint = resolveTint(node, fluid, fluidState, fluidModel);
                        boolean gas = fluid.getFluidType().isLighterThanAir();
                        int alpha = (tint >>> 24) & 0xFF;
                        if (alpha == 0) alpha = 0xFF;
                        state.fluidFill = fill;
                        state.fluidGas = gas;
                        state.fluidFlowMask = node.fluidFlowMask;
                        state.fluidR = (tint >>> 16) & 0xFF;
                        state.fluidG = (tint >>> 8) & 0xFF;
                        state.fluidB = tint & 0xFF;
                        state.fluidA = gas ? Math.min(alpha, 0xB8) : Math.max(alpha, 0xD8);
                        int emissive = fluid.getFluidType().getLightLevel(stack);
                        state.fluidLight =
                                emissive > 0
                                        ? LightCoordsUtil.pack(emissive, 15)
                                        : state.lightCoords;
                        state.hasFluid = true;
                    }
                }
            }
        }
    }

    private static int resolveTint(
            TransportNode node,
            net.minecraft.world.level.material.Fluid fluid,
            net.minecraft.world.level.material.FluidState fluidState,
            net.minecraft.client.renderer.block.FluidModel fluidModel) {
        int tint = 0xFFFFFFFF;
        var fTint = fluidModel.fluidTintSource();
        if (fTint != null) {
            tint = fTint.color(fluidState);
        } else {
            var bTint = fluidModel.tintSource();
            if (bTint != null) {
                var blockState = fluidState.createLegacyBlock();
                tint =
                        node.getLevel() instanceof BlockAndTintGetter getter
                                ? bTint.colorInWorld(blockState, getter, node.getBlockPos())
                                : bTint.color(blockState);
            }
        }
        if ((tint & 0xFFFFFF) == 0 && (tint >>> 24) == 0) {
            tint = 0xFFFFFFFF;
        }
        if ((fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER)
                && (tint & 0xFFFFFF) == 0xFFFFFF) {
            tint = 0xFF3F76E4;
        }
        return tint;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.hasNozzles) {
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
        if (state.hasItem) {
            pose.pushPose();
            pose.translate(state.itemX, state.itemY, state.itemZ);
            pose.mulPose(Axis.YP.rotationDegrees(state.itemSpin));
            pose.scale(state.itemScale, state.itemScale, state.itemScale);
            state.itemState.submit(
                    pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        if (state.hasFluid && state.fluidFlowingSprite != null && state.fluidStillSprite != null) {
            final BlockState pipeState = state.pipeState;
            final int size = state.size;
            final float fill = state.fluidFill;
            final boolean gas = state.fluidGas;
            final int flowMask = state.fluidFlowMask;
            final TextureAtlasSprite flowingSprite = state.fluidFlowingSprite;
            final TextureAtlasSprite stillSprite = state.fluidStillSprite;
            final float flowUvScale = state.fluidFlowUvScale;
            final int r = state.fluidR;
            final int g = state.fluidG;
            final int b = state.fluidB;
            final int a = state.fluidA;
            final int light = state.fluidLight;
            collector.submitCustomGeometry(
                    pose,
                    FLUID_LAYER,
                    (entry, consumer) ->
                            emitFluidQuads(
                                    entry,
                                    consumer,
                                    pipeState,
                                    size,
                                    fill,
                                    gas,
                                    flowMask,
                                    flowingSprite,
                                    stillSprite,
                                    flowUvScale,
                                    r,
                                    g,
                                    b,
                                    a,
                                    light));
        }
    }

    private static void emitFluidQuads(
            PoseStack.Pose entry,
            VertexConsumer consumer,
            BlockState pipeState,
            int size,
            float fill,
            boolean gas,
            int flowMask,
            TextureAtlasSprite flowingSprite,
            TextureAtlasSprite stillSprite,
            float flowUvScale,
            int r,
            int g,
            int b,
            int a,
            int light) {
        float[] quadPos = new float[12];
        float[] quadUv = new float[8];
        PipeVisuals.emitFluidGeometry(
                pipeState,
                size,
                fill,
                gas,
                flowMask,
                (face, x0, y0, z0, x1, y1, z1, dir) -> {
                    PipeVisuals.buildQuad(face, x0, y0, z0, x1, y1, z1, dir, quadPos, quadUv);
                    TextureAtlasSprite sprite = dir == null ? stillSprite : flowingSprite;
                    float uvScale = dir == null ? 1.0F : flowUvScale;
                    float u0 = sprite.getU0();
                    float uSpan = (sprite.getU1() - u0) * uvScale;
                    float v0 = sprite.getV0();
                    float vSpan = (sprite.getV1() - v0) * uvScale;
                    float nx = face.getStepX();
                    float ny = face.getStepY();
                    float nz = face.getStepZ();
                    for (int i = 0; i < 4; i++) {
                        int pi = i * 3;
                        int ui = i * 2;
                        consumer.addVertex(
                                        entry.pose(), quadPos[pi], quadPos[pi + 1], quadPos[pi + 2])
                                .setColor(r, g, b, a)
                                .setUv(u0 + uSpan * quadUv[ui], v0 + vSpan * quadUv[ui + 1])
                                .setOverlay(OverlayTexture.NO_OVERLAY)
                                .setLight(light)
                                .setNormal(entry, nx, ny, nz);
                    }
                });
    }

    static final class State extends BlockEntityRenderState {
        final Nozzle[] modes = new Nozzle[TransportNode.SIDES.length];
        final boolean[] connected = new boolean[TransportNode.SIDES.length];
        BlockState pipeState;
        int size;
        boolean steel;
        boolean hasNozzles;
        boolean hasItem;
        final ItemStackRenderState itemState = new ItemStackRenderState();
        float itemX;
        float itemY;
        float itemZ;
        float itemScale;
        float itemSpin;
        boolean hasFluid;
        TextureAtlasSprite fluidFlowingSprite;
        TextureAtlasSprite fluidStillSprite;
        float fluidFlowUvScale;
        float fluidFill;
        boolean fluidGas;
        int fluidFlowMask;
        int fluidR;
        int fluidG;
        int fluidB;
        int fluidA;
        int fluidLight;
    }
}
