package com.industrialage.ia2.world;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class TerraWart extends Item {
    TerraWart(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        var remainder = super.finishUsingItem(stack, level, consumer);
        if (level instanceof ServerLevel) {
            for (var effect : List.copyOf(consumer.getActiveEffects())) {
                if (WorldNative.harmful(effect)) consumer.removeEffect(effect.getEffect());
            }
        }
        return remainder;
    }
}
