package com.industrialage.ia2.progress;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Public hook for actions whose native outcome has already been confirmed by the server. */
public final class IndustrialProgress {
    private IndustrialProgress() {}

    /** Records a successful, paid powered-tool hit for its attacking player. */
    public static void poweredToolHit(ServerPlayer player, ItemStack tool) {
        if (player != null && tool != null && !tool.isEmpty()) {
            ProgressPlatform.poweredToolHit(player, tool);
        }
    }
}
