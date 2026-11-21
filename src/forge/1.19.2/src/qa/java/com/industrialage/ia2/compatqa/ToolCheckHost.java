package com.industrialage.ia2.compatqa;

import com.industrialage.ia2.forge.equipment.ForgeGear;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;

final class ToolCheckHost {
    private ToolCheckHost() {}

    static double stored(ItemStack stack) {
        return ForgeGear.stored(stack);
    }

    static void charge(ItemStack stack, int amount) {
        double left = amount;
        while (left > 0) {
            double accepted = ForgeGear.charge(stack, left, 4);
            if (accepted <= 0) throw new AssertionError("Could not seed " + amount + " EU");
            left -= accepted;
        }
    }

    static LivingEntity target(ServerLevel level) {
        return EntityType.VILLAGER.create(level);
    }

    static ItemStack bucket(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return ((SimpleWaterloggedBlock) level.getBlockState(pos).getBlock())
                .pickupBlock(level, pos, level.getBlockState(pos));
    }
}
