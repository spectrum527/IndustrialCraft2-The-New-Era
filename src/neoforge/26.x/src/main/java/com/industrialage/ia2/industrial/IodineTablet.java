package com.industrialage.ia2.industrial;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

final class IodineTablet extends Item {
    IodineTablet(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!Radiation.has(player)) return InteractionResult.PASS;
        if (level instanceof ServerLevel) Radiation.treat(player, stack);
        return InteractionResult.SUCCESS;
    }
}
