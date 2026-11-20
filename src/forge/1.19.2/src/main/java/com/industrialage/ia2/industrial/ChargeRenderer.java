package com.industrialage.ia2.industrial;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.BlockItem;

/** Forge 1.19 has no synced TNT block-state field, so draw the registered IC2 block at the fuse. */
final class ChargeRenderer extends TntRenderer {
    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blocks;

    ChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            PrimedTnt entity,
            float yaw,
            float fraction,
            PoseStack pose,
            MultiBufferSource buffer,
            int light) {
        var item =
                PowerPlatform.item(
                                entity.getType() == ChargeTypes.INDUSTRIAL.get() ? "itnt" : "nuke")
                        .getItem();
        if (!(item instanceof BlockItem block)) return;
        pose.pushPose();
        pose.translate(-0.5, 0, -0.5);
        TntMinecartRenderer.renderWhiteSolidBlock(
                blocks,
                block.getBlock().defaultBlockState(),
                pose,
                buffer,
                light,
                entity.getFuse() / 5 % 2 == 0);
        pose.popPose();
    }
}
