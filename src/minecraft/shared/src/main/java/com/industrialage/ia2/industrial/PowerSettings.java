package com.industrialage.ia2.industrial;

import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.platform.ConfigFacade;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

final class PowerSettings {
    static ConfigFacade config;

    private PowerSettings() {}

    static double multiplier(String path) {
        String key = "balance/energy/generator/" + path;
        if (path.equals("solar") || path.equals("water") || path.equals("wind")) {
            return config.current()
                    .map(s -> s.get(ActiveConfig.scalarKey(key, Double.class)))
                    .orElse(1.0);
        }
        return config.current()
                .map(s -> s.get(ActiveConfig.scalarKey(key, Float.class)))
                .orElse(1.0F);
    }

    static boolean inventoryWeight() {
        return config.current()
                .map(
                        s ->
                                s.get(
                                        ActiveConfig.scalarKey(
                                                "balance/teleporterUseInventoryWeight",
                                                Boolean.class)))
                .orElse(true);
    }

    static boolean gameChargesEnabled() {
        return config.current()
                .map(s -> s.get(ActiveConfig.scalarKey("protection/enableNuke", Boolean.class)))
                .orElse(true);
    }

    static float gameChargeLimit() {
        return config.current()
                .map(
                        s ->
                                s.get(
                                        ActiveConfig.scalarKey(
                                                "protection/nukeExplosionPowerLimit", Float.class)))
                .orElse(60F);
    }

    static float explosionLimit() {
        return config.current()
                .map(
                        s ->
                                s.get(
                                        ActiveConfig.scalarKey(
                                                "protection/reactorExplosionPowerLimit",
                                                Float.class)))
                .orElse(45F);
    }

    static boolean sun(ServerLevel world, BlockPos pos) {
        return world.dimensionType().hasSkyLight()
                && world.getSkyDarken() < 4
                && world.canSeeSky(pos.above())
                && !world.isRainingAt(pos.above());
    }
}
