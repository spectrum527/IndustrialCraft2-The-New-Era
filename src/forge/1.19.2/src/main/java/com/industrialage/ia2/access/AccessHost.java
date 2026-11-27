package com.industrialage.ia2.access;

import com.industrialage.ia2.logistics.TransportBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

final class AccessHost {
    private AccessHost() {}

    static int read(BlockEntity node) {
        return node.getPersistentData().getInt(MachineAccess.SAVE_KEY);
    }

    static Level playerLevel(Player player) {
        return player.level;
    }

    static void refresh(ServerLevel level, BlockEntity node) {
        if (node instanceof MachineAccessTarget target) target.invalidateMachinePorts();
        level.updateNeighborsAt(node.getBlockPos(), node.getBlockState().getBlock());
        TransportBlock.refreshConnections(level, node.getBlockPos());
    }
}
