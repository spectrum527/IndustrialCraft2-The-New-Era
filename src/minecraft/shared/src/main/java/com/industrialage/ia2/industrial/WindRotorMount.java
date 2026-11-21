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
        WOOD(1, "wooden", "wood", 70, 5, 9),
        IRON(3, "iron", "iron", 100, 7, 10),
        BRONZE(2, "bronze", "bronze", 100, 7, 10),
        STEEL(4, "steel", "steel", 120, 9, 11),
        CARBON(5, "carbon", "carbon", 150, 11, 12);

        private final int type;
        private final String path;
        private final int rating;
        final String texture;
        final int diameter;
        final int bladeWidth;

        Material(int type, String path, String texture, int rating, int diameter, int bladeWidth) {
            this.type = type;
            this.path = path;
            this.rating = rating;
            this.texture = "textures/items/rotor/" + texture + "_rotor_model.png";
            this.diameter = diameter;
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
        var material = material(stack);
        return material == null ? 0 : material.diameter;
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
        var hub = Vec3.atCenterOf(pos).add(face.getStepX() * OFFSET, 0, face.getStepZ() * OFFSET);
        boolean eastWest = face.getAxis() == Direction.Axis.X;
        double span = material.diameter;
        return housing.minmax(AABB.ofSize(hub, eastWest ? 1 : span, span, eastWest ? span : 1));
    }
}
