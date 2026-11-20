package com.industrialage.ia2.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
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
        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null) return ItemStack.EMPTY;
        try (var transaction = Transaction.openRoot()) {
            var stack =
                    ResourceHandlerUtil.extractFirst(handler, resource -> true, max, transaction);
            if (stack == null) return ItemStack.EMPTY;
            transaction.commit();
            return stack.resource().toStack(stack.amount());
        }
    }

    static ItemStack pullMatching(
            ServerLevel level,
            BlockPos pos,
            Direction side,
            int max,
            java.util.function.Predicate<ItemStack> filter) {
        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null) return ItemStack.EMPTY;
        try (var transaction = Transaction.openRoot()) {
            var stack =
                    ResourceHandlerUtil.extractFirst(
                            handler,
                            resource -> filter.test(resource.toStack(1)),
                            max,
                            transaction);
            if (stack == null) return ItemStack.EMPTY;
            transaction.commit();
            return stack.resource().toStack(stack.amount());
        }
    }
}
