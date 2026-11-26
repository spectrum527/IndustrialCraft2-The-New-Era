package com.industrialage.ia2.industrial;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Dimensions, durability specs, and mounting face shared by rotor wear, clearance, and renderer.
 */
final class WindRotorMount {
    static final double OFFSET = 0.875;
    private static final Material[] BY_TYPE = new Material[Material.values().length + 1];
    private static final Map<String, Material> BY_ITEM;
    private static final Map<String, Material> BY_PATH;

    static {
        Map<String, Material> byItem = new HashMap<>();
        Map<String, Material> byPath = new HashMap<>();
        for (var material : Material.values()) {
            BY_TYPE[material.type] = material;
            byItem.put("ic2:" + material.path + "_rotor", material);
            byPath.put(material.path, material);
        }
        BY_ITEM = Map.copyOf(byItem);
        BY_PATH = Map.copyOf(byPath);
    }

    private WindRotorMount() {}

    enum Material {
        // The water size for wood is only used to display a retained old slot item.
        WOOD(1, "wooden", "wood", 70, 5, 4, 9, 10_800, 60, false),
        IRON(3, "iron", "iron", 100, 7, 4, 10, 86_400, 75, true),
        BRONZE(2, "bronze", "bronze", 100, 7, 4, 10, 86_400, 75, true),
        STEEL(4, "steel", "steel", 120, 9, 6, 11, 172_800, 90, true),
        CARBON(5, "carbon", "carbon", 150, 11, 8, 12, 604_800, 110, true);

        private final int type;
        private final String path;
        private final int rating;
        final String texture;
        private final int windDiameter;
        private final int waterDiameter;
        final int bladeWidth;
        final int maxDurability;
        final int maxSafeWind;
        final boolean waterAllowed;

        Material(
                int type,
                String path,
                String texture,
                int rating,
                int windDiameter,
                int waterDiameter,
                int bladeWidth,
                int maxDurability,
                int maxSafeWind,
                boolean waterAllowed) {
            this.type = type;
            this.path = path;
            this.rating = rating;
            this.texture = "textures/items/rotor/" + texture + "_rotor_model.png";
            this.windDiameter = windDiameter;
            this.waterDiameter = waterDiameter;
            this.bladeWidth = bladeWidth;
            this.maxDurability = maxDurability;
            this.maxSafeWind = maxSafeWind;
            this.waterAllowed = waterAllowed;
        }
    }

    static int durability(String materialPath) {
        var material = BY_PATH.get(materialPath);
        return material == null ? 0 : material.maxDurability;
    }

    static int maxDurability(ItemStack stack) {
        var material = material(stack);
        if (material == null) return 0;
        int itemMax = stack.getMaxDamage();
        return itemMax > 0 ? itemMax : material.maxDurability;
    }

    static int maxSafeWind(ItemStack stack) {
        var material = material(stack);
        return material == null ? 0 : material.maxSafeWind;
    }

    static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        var material = material(stack);
        if (material == null) return;
        int max = maxDurability(stack);
        int remaining = Math.max(0, max - stack.getDamageValue());
        double percent = max > 0 ? (remaining * 100.0) / max : 0.0;
        double remainingHours = remaining / 3600.0;
        tooltip.add(
                Component.translatable(
                                "tooltip.ic2.rotor.durability",
                                remaining,
                                max,
                                String.format(Locale.ROOT, "%.1f", percent))
                        .withStyle(percent > 25.0 ? ChatFormatting.GRAY : ChatFormatting.RED));
        tooltip.add(
                Component.translatable(
                                "tooltip.ic2.rotor.remaining_hours",
                                String.format(Locale.ROOT, "%.1f", remainingHours))
                        .withStyle(ChatFormatting.AQUA));
        tooltip.add(
                Component.translatable("tooltip.ic2.rotor.max_safe_wind", material.maxSafeWind)
                        .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(
                Component.translatable(
                                material.waterAllowed
                                        ? "tooltip.ic2.rotor.water_allowed"
                                        : "tooltip.ic2.rotor.wind_only")
                        .withStyle(ChatFormatting.DARK_GRAY));
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
