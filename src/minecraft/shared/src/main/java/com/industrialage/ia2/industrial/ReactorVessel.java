package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Three blocks of interior space inside a five-block outer shell, centered on the reactor. */
final class ReactorVessel {
    static final int TANK_CAPACITY = 10_000;

    private ReactorVessel() {}

    static boolean boundary(int x, int y, int z) {
        return Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) == 2;
    }

    /** Minus one means incomplete, zero means unpowered, one means an active redstone port. */
    static int inspect(ServerLevel level, BlockPos center) {
        int signal = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (!boundary(x, y, z)) continue;
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!level.hasChunkAt(cursor)
                            || !(level.getBlockState(cursor).getBlock() instanceof PowerBlock block)
                            || !block.kind.vesselPart()) return -1;
                    if (block.kind == PowerKind.REACTOR_REDSTONE_PORT
                            && level.hasNeighborSignal(cursor)) signal = 1;
                }
            }
        }
        return signal;
    }

    /** A port binds to an intact, loaded shell. Shared walls cannot connect two reactors. */
    static PowerNode core(PowerNode port, ServerLevel level) {
        var origin = port.getBlockPos();
        if (port.vesselCenter != null
                && level.hasChunkAt(port.vesselCenter)
                && level.getBlockEntity(port.vesselCenter) instanceof PowerNode cached
                && cached.fluidMode) return cached;
        port.vesselCenter = null;
        PowerNode result = null;
        for (int x = -2; x <= 2; x++)
            for (int y = -2; y <= 2; y++)
                for (int z = -2; z <= 2; z++) {
                    if (!boundary(x, y, z)) continue;
                    BlockPos candidate = origin.offset(x, y, z);
                    if (level.hasChunkAt(candidate)
                            && level.getBlockEntity(candidate) instanceof PowerNode node
                            && node.kind == PowerKind.NUCLEAR_REACTOR
                            && node.fluidMode) {
                        if (result != null) return null;
                        result = node;
                    }
                }
        if (result != null) port.vesselCenter = result.getBlockPos();
        return result;
    }
}
