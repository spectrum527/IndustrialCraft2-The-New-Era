package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.logistics.TransportNode.Nozzle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.core.Direction;

/** Shared socket mesh. Its mouth normal is local +Z and its marker follows the flow. */
final class PipeNozzleModel {
    private final ModelPart extraction = bake(true);
    private final ModelPart injection = bake(false);

    ModelPart part(Nozzle mode) {
        return mode == Nozzle.EXTRACTION ? extraction : injection;
    }

    static boolean steel(TransportSpec pipe) {
        return pipe.path().startsWith("steel_");
    }

    static void position(PoseStack pose, int size, Direction face, boolean connected) {
        // Native cores span 4, 6, 8 or 10 model pixels. A connected arm reaches the block edge.
        double distance = connected ? 31.75 / 64 : (4.25 + size) / 16;
        pose.translate(
                0.5 + face.getStepX() * distance,
                0.5 + face.getStepY() * distance,
                0.5 + face.getStepZ() * distance);
    }

    static void scale(PoseStack pose, int size) {
        float width = (5.25F + 2 * size) / 15;
        pose.scale(width, width, 0.62F);
    }

    private static ModelPart bake(boolean inward) {
        var mesh = new MeshDefinition();
        var socket =
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7, -7, -5, 14, 14, 4)
                        .texOffs(0, 24)
                        .addBox(-7.5F, -7.5F, -1.15F, 15, 1.75F, 1.15F)
                        .texOffs(0, 24)
                        .addBox(-7.5F, 5.75F, -1.15F, 15, 1.75F, 1.15F)
                        .texOffs(0, 34)
                        .addBox(-7.5F, -5.75F, -1.15F, 1.75F, 11.5F, 1.15F)
                        .texOffs(0, 34)
                        .addBox(5.75F, -5.75F, -1.15F, 1.75F, 11.5F, 1.15F);
        float[] glyph = {1.1F, 1.1F, 3.5F, 2.5F, 0.65F};
        socket.texOffs(inward ? 1 : 21, 50);
        for (int band = 0; band < glyph.length; band++) {
            float half = glyph[band];
            float z = -4.7F + 0.68F * (inward ? glyph.length - band - 1 : band);
            socket.addBox(-half, -7.13F, z, half * 2, .16F, .68F)
                    .addBox(-half, 6.97F, z, half * 2, .16F, .68F)
                    .addBox(-7.13F, -half, z, .16F, half * 2, .68F)
                    .addBox(6.97F, -half, z, .16F, half * 2, .68F);
        }
        mesh.getRoot().addOrReplaceChild("socket", socket, PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("socket");
    }
}
