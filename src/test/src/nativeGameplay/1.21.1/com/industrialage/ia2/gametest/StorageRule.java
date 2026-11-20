package com.industrialage.ia2.gametest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

final class StorageRule {
    static boolean exposesAutomation(ServerLevel level, net.minecraft.core.BlockPos pos) {
        return level.getCapability(
                        net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                        pos,
                        net.minecraft.core.Direction.UP)
                != null;
    }

    static boolean blockDrops(ServerLevel level) {
        return level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
    }
}
