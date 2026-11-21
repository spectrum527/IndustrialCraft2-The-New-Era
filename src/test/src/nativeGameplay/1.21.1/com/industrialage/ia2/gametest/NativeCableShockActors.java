package com.industrialage.ia2.gametest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** Creates a real golem using the selected Minecraft entity factory. */
final class NativeCableShockActors {
    private NativeCableShockActors() {}

    static Mob ironGolem(ServerLevel world) {
        return EntityType.IRON_GOLEM.create(world);
    }
}
