package com.industrialage.ia2.world;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class WorldItemAccess {
    private WorldItemAccess() {}

    static int push(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null) return 0;
        try (var transaction = Transaction.openRoot()) {
            int accepted =
                    ResourceHandlerUtil.insertStacking(
                            handler, ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
            return accepted;
        }
    }

    static ItemStack pull(ServerLevel level, BlockPos pos, Direction side, int max) {
        return extractLimited(
                level.getCapability(Capabilities.Item.BLOCK, pos, side), resource -> true, max);
    }

    static ItemStack pullMatching(
            ServerLevel level, BlockPos pos, Direction side, int max, Predicate<ItemStack> filter) {
        return extractLimited(
                level.getCapability(Capabilities.Item.BLOCK, pos, side),
                resource -> filter.test(resource.toStack(1)),
                max);
    }

    private static ItemStack extractLimited(
            ResourceHandler<ItemResource> handler, Predicate<ItemResource> filter, int max) {
        if (handler == null || max <= 0) return ItemStack.EMPTY;
        for (int slot = 0; slot < handler.size(); slot++) {
            var resource = handler.getResource(slot);
            if (resource.isEmpty() || !filter.test(resource)) continue;
            int limit = Math.min(max, resource.toStack(1).getMaxStackSize());
            if (limit <= 0) continue;
            try (var transaction = Transaction.openRoot()) {
                int taken = handler.extract(slot, resource, limit, transaction);
                if (taken > 0) {
                    transaction.commit();
                    return resource.toStack(taken);
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
