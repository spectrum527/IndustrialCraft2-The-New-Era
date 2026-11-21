package com.industrialage.ia2.industrial;

import com.industrialage.ia2.energy.graph.PacketBudget;
import com.industrialage.ia2.equipment.ArmorEffects;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Resolves exposure to current actually carried by uncovered cables during one world tick. */
public final class CableShock {
    private final Map<BlockPos, Double> current = new HashMap<>();

    /** Whether the cable material has exposed metal before applying a construction foam cover. */
    public static boolean isBare(String path) {
        return switch (path) {
            case "tin_cable", "copper_cable", "gold_cable", "iron_cable" -> true;
            default -> false;
        };
    }

    /** Adds one successful delivery on a bare segment, using the same EU as the conductor meter. */
    public void record(BlockPos pos, double delivered) {
        if (delivered > 0) current.merge(pos.immutable(), delivered, Double::sum);
    }

    /**
     * Applies the strongest nearby exposure once per living entity after all packets have settled.
     */
    public void apply(ServerLevel level) {
        if (current.isEmpty()) return;
        var damage = new HashMap<LivingEntity, Integer>();
        current.forEach(
                (pos, eu) -> {
                    int radius = radius(eu);
                    int points = (int) Math.ceil(eu / 64);
                    var center = Vec3.atCenterOf(pos);
                    for (var entity :
                            level.getEntitiesOfClass(
                                    LivingEntity.class, new AABB(pos).inflate(radius))) {
                        if (entity.isAlive()
                                && !entity.isSpectator()
                                && withinReach(center, entity.getBoundingBox(), radius)) {
                            damage.merge(entity, points, Math::max);
                        }
                    }
                });
        damage.forEach(
                (entity, points) -> {
                    if (!ArmorEffects.fullHazmat(entity))
                        PowerPlatform.shock(level, entity, points);
                });
    }

    private static int radius(double current) {
        for (int tier = 1; tier < 5; tier++) {
            if (current <= PacketBudget.packetSize(tier)) return tier;
        }
        return 5;
    }

    private static boolean withinReach(Vec3 center, AABB box, int radius) {
        return center.distanceToSqr(
                        Mth.clamp(center.x, box.minX, box.maxX),
                        Mth.clamp(center.y, box.minY, box.maxY),
                        Mth.clamp(center.z, box.minZ, box.maxZ))
                <= radius * radius;
    }
}
