package com.industrialage.ia2.power;

import com.industrialage.ia2.forge.content.ForgePower;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Connects specialized machine lifetimes to the active loader's synchronous EU network. */
public final class WorldEnergy {
    private WorldEnergy() {}

    /**
     * Attaches the block entity on load and refreshes its faces after an orientation or mode
     * change.
     */
    public static void connect(BlockEntity entity) {
        if (entity.getLevel() instanceof ServerLevel level
                && entity instanceof WorldPowerPort endpoint) {
            ForgePower.attach(entity);
        }
    }

    /** Invalidates the exact block entity on removal or chunk unload. */
    public static void disconnect(BlockEntity entity) {
        if (entity.getLevel() instanceof ServerLevel level
                && entity instanceof WorldPowerPort endpoint) {
            ForgePower.detach(entity);
        }
    }
}
