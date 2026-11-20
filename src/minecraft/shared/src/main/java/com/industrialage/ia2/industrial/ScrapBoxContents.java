package com.industrialage.ia2.industrial;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class ScrapBoxContents {
    private ScrapBoxContents() {}

    static void open(Level level, Player player, ItemStack box) {
        if (level instanceof ServerLevel server) {
            player.awardStat(Stats.ITEM_USED.get(box.getItem()));
            if (!player.isCreative()) {
                box.shrink(1);
            }
            WorkshopPlatform.scrapRewards(
                    server,
                    reward -> {
                        var dropped =
                                new ItemEntity(
                                        server,
                                        player.getX(),
                                        player.getY() + 0.5,
                                        player.getZ(),
                                        reward);
                        dropped.setDefaultPickUpDelay();
                        server.addFreshEntity(dropped);
                    });
        }
    }
}
