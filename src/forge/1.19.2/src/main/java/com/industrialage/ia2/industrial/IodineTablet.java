package com.industrialage.ia2.industrial;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class IodineTablet extends Item {
    IodineTablet(Properties p) {
        super(p);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!Radiation.has(player)) return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (level instanceof ServerLevel) Radiation.treat(player, stack);
        return net.minecraft.world.InteractionResultHolder.success(stack);
    }
}
