package com.industrialage.ia2.compatqa;

import com.industrialage.ia2.gameplay.PortableGear;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;

final class ToolCheckHost {
    private ToolCheckHost() {}

    static double stored(ItemStack stack) {
        return PortableGear.stored(stack);
    }

    static void charge(ItemStack stack, int amount) {
        double left = amount;
        while (left > 0) {
            double accepted = PortableGear.charge(stack, left, 4);
            if (accepted <= 0) throw new AssertionError("Could not seed " + amount + " EU");
            left -= accepted;
        }
    }

    static LivingEntity target(ServerLevel level) {
        var type =
                BuiltInRegistries.ENTITY_TYPE
                        .getOptional(Identifier.fromNamespaceAndPath("minecraft", "villager"))
                        .orElseThrow();
        return (LivingEntity)
                type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
    }

    static ItemStack bucket(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return ((SimpleWaterloggedBlock) level.getBlockState(pos).getBlock())
                .pickupBlock(player, level, pos, level.getBlockState(pos));
    }
}
