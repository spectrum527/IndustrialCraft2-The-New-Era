package com.industrialage.ia2.industrial;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** One serving of tinned food is eaten immediately without a container remainder. */
public final class CannedFood {
    private CannedFood() {}

    /** Feeds a hungry player and consumes exactly one tin on the owning server. */
    public static boolean eat(Level level, Player player, ItemStack stack) {
        if (!player.canEat(false)) return false;
        if (level instanceof ServerLevel) {
            player.getFoodData().eat(1, 0.6F);
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            if (!player.isCreative()) stack.shrink(1);
        }
        return true;
    }
}
