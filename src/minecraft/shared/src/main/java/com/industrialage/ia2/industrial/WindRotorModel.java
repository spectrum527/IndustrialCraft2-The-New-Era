package com.industrialage.ia2.industrial;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
import java.util.WeakHashMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.core.Direction;

/** Bakes the author's rotor textures onto a stationary shaft and four pitched blades. */
final class WindRotorModel {
    private final EnumMap<WindRotorMount.Material, Parts> wind =
            new EnumMap<>(WindRotorMount.Material.class);
    private final EnumMap<WindRotorMount.Material, Parts> water =
            new EnumMap<>(WindRotorMount.Material.class);
    private final WeakHashMap<WorkshopNode, Motion> motions = new WeakHashMap<>();

    WindRotorModel() {
        for (var material : WindRotorMount.Material.values()) {
            var windDiameter = WindRotorMount.diameter(WorkshopKind.WIND_KINETIC, material);
            var waterDiameter = WindRotorMount.diameter(WorkshopKind.WATER_KINETIC, material);
            var windModel = bake(material, windDiameter);
            wind.put(material, windModel);
            water.put(
                    material,
                    windDiameter == waterDiameter ? windModel : bake(material, waterDiameter));
        }
    }

    Parts parts(WorkshopKind kind, WindRotorMount.Material material) {
        return (kind == WorkshopKind.WATER_KINETIC ? water : wind).get(material);
    }

    static void translateToHub(PoseStack pose, Direction face) {
        double offset = WindRotorMount.OFFSET;
        pose.translate(
                0.5 + face.getStepX() * offset,
                0.5 + face.getStepY() * offset,
                0.5 + face.getStepZ() * offset);
    }

    float angle(WorkshopNode node, WindRotorMount.Material material, float partialTick) {
        if (material == null || node.getLevel() == null) {
            motions.remove(node);
            return 0;
        }
        var motion = motions.get(node);
        if (motion == null || motion.material != material) {
            motion = new Motion(material, node.getLevel().getGameTime(), partialTick);
            motions.put(node, motion);
        }
        return motion.advance(node.getLevel().getGameTime(), partialTick, node.visualRotorSpeed());
    }

    private static Parts bake(WindRotorMount.Material material, int diameter) {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild(
                "shaft",
                CubeListBuilder.create().texOffs(0, 192).addBox(-2.5F, -2.5F, -7, 5, 5, 7),
                PartPose.ZERO);
        var wheel =
                root.addOrReplaceChild(
                        "wheel",
                        CubeListBuilder.create()
                                .texOffs(0, 208)
                                .addBox(-4, -4, -2.5F, 8, 8, 5)
                                .texOffs(0, 232)
                                .addBox(-2.5F, -2.5F, 2.5F, 5, 5, 2),
                        PartPose.ZERO);
        float width = material.bladeWidth;
        float radius = diameter * 8 - 0.5F;
        float tip = (float) Math.sqrt(radius * radius - width * width * 0.25F);
        for (int blade = 0; blade < 4; blade++) {
            wheel.addOrReplaceChild(
                    "blade_" + blade,
                    CubeListBuilder.create()
                            .texOffs(0, 0)
                            .addBox(-width / 2, -tip, -1, width, tip - 8, 2)
                            .texOffs(0, 176)
                            .addBox(-2.5F, -8, -1.5F, 5, 8, 3),
                    PartPose.offsetAndRotation(0, 0, 0, 0, 0.24F, (float) (blade * Math.PI / 2)));
        }
        var baked = LayerDefinition.create(mesh, 32, 256).bakeRoot();
        return new Parts(baked.getChild("shaft"), baked.getChild("wheel"));
    }

    record Parts(ModelPart shaft, ModelPart wheel) {}

    private static final class Motion {
        private final WindRotorMount.Material material;
        private long tick;
        private float partialTick;
        private float angle;
        private float degreesPerTick;

        Motion(WindRotorMount.Material material, long tick, float partialTick) {
            this.material = material;
            this.tick = tick;
            this.partialTick = partialTick;
        }

        float advance(long now, float partial, int availableKu) {
            double elapsed = Math.max(0, (now - tick) + (double) partial - partialTick);
            angle = (float) ((angle + elapsed * degreesPerTick) % 360);
            tick = now;
            partialTick = partial;
            degreesPerTick = availableKu <= 0 ? 0 : Math.min(15, 0.65F + availableKu / 24F);
            return angle;
        }
    }
}
