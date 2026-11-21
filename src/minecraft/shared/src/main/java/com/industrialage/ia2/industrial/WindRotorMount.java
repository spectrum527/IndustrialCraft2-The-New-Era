package com.industrialage.ia2.industrial;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Dimensions and mounting face shared by the wind clearance check and the renderer. */
final class WindRotorMount {
    static final double OFFSET = 0.875;
    private static final Material[] BY_TYPE = new Material[Material.values().length + 1];
    private static final Map<String, Material> BY_ITEM;

    static {
        Map<String, Material> byItem = new HashMap<>();
        for (var material : Material.values()) {
            BY_TYPE[material.type] = material;
            byItem.put("ic2:" + material.path + "_rotor", material);
        }
        BY_ITEM = Map.copyOf(byItem);
    }

    private WindRotorMount() {}

    enum Material {
        // The water size for wood is only used to display a retained old slot item.
        WOOD(1, "wooden", "wood", 70, 5, 4, 9),
        IRON(3, "iron", "iron", 100, 7, 4, 10),
        BRONZE(2, "bronze", "bronze", 100, 7, 4, 10),
        STEEL(4, "steel", "steel", 120, 9, 6, 11),
        CARBON(5, "carbon", "carbon", 150, 11, 8, 12);

        private final int type;
        private final String path;
        private final int rating;
        final String texture;
        private final int windDiameter;
        private final int waterDiameter;
        final int bladeWidth;

        Material(
                int type,
                String path,
                String texture,
                int rating,
                int windDiameter,
                int waterDiameter,
                int bladeWidth) {
            this.type = type;
            this.path = path;
            this.rating = rating;
            this.texture = "textures/items/rotor/" + texture + "_rotor_model.png";
            this.windDiameter = windDiameter;
            this.waterDiameter = waterDiameter;
            this.bladeWidth = bladeWidth;
        }
    }

    static Map<String, Integer> ratings() {
        return BY_ITEM.entrySet().stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                Map.Entry::getKey, entry -> entry.getValue().rating));
    }

    static List<String> names() {
        return Arrays.stream(Material.values()).map(material -> material.path).toList();
    }

    private static Material material(ItemStack stack) {
        return stack.isEmpty() ? null : BY_ITEM.get(WorkshopPlatform.itemId(stack));
    }

    static int type(ItemStack stack) {
        var material = material(stack);
        return material == null ? 0 : material.type;
    }

    static Material material(int type) {
        return type > 0 && type < BY_TYPE.length ? BY_TYPE[type] : null;
    }

    static int diameter(ItemStack stack) {
        return diameter(WorkshopKind.WIND_KINETIC, stack);
    }

    static int diameter(WorkshopKind kind, ItemStack stack) {
        var material = material(stack);
        return material == null ? 0 : diameter(kind, material);
    }

    static int diameter(WorkshopKind kind, Material material) {
        return switch (kind) {
            case WIND_KINETIC -> material.windDiameter;
            case WATER_KINETIC -> material.waterDiameter;
            default -> 0;
        };
    }

    static Direction face(WorkshopNode node) {
        return WorkshopBlock.direction(node.getBlockState()).getOpposite();
    }

    static AABB bounds(WorkshopNode node) {
        var pos = node.getBlockPos();
        var housing = new AABB(pos);
        var material = material(node.visualRotorType());
        if (material == null) return housing;

        var face = face(node);
        var hub =
                Vec3.atCenterOf(pos)
                        .add(
                                face.getStepX() * OFFSET,
                                face.getStepY() * OFFSET,
                                face.getStepZ() * OFFSET);
        var axis = face.getAxis();
        double span = diameter(node.kind, material);
        return housing.minmax(
                AABB.ofSize(
                        hub,
                        axis == Direction.Axis.X ? 1 : span,
                        axis == Direction.Axis.Y ? 1 : span,
                        axis == Direction.Axis.Z ? 1 : span));
    }
}
