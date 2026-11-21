package com.industrialage.ia2.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;

/** Creates a real golem using the selected Minecraft entity factory. */
final class NativeCableShockActors {
    private NativeCableShockActors() {}

    static Mob ironGolem(ServerLevel world) {
        var type =
                BuiltInRegistries.ENTITY_TYPE.stream()
                        .filter(
                                candidate ->
                                        BuiltInRegistries.ENTITY_TYPE
                                                .getKey(candidate)
                                                .toString()
                                                .equals("minecraft:iron_golem"))
                        .findFirst()
                        .orElseThrow();
        return (Mob) type.create(world, EntitySpawnReason.COMMAND);
    }
}
