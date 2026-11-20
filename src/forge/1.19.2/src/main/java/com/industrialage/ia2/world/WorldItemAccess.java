package com.industrialage.ia2.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;

final class WorldItemAccess {
    private WorldItemAccess() {}

    static int push(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
        var entity = level.getBlockEntity(pos);
        var handler =
                entity == null
                        ? null
                        : entity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
        if (handler == null) return 0;
        return stack.getCount()
                - ItemHandlerHelper.insertItemStacked(handler, stack, false).getCount();
    }

    static ItemStack pull(ServerLevel level, BlockPos pos, Direction side, int max) {
        var entity = level.getBlockEntity(pos);
        var handler =
                entity == null
                        ? null
                        : entity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
        if (handler == null) return ItemStack.EMPTY;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            var stack = handler.extractItem(slot, max, false);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    static ItemStack pullMatching(
            ServerLevel level,
            BlockPos pos,
            Direction side,
            int max,
            java.util.function.Predicate<ItemStack> filter) {
        var entity = level.getBlockEntity(pos);
        var handler =
                entity == null
                        ? null
                        : entity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
        if (handler == null) return ItemStack.EMPTY;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            var preview = handler.extractItem(slot, max, true);
            if (!preview.isEmpty() && filter.test(preview)) {
                return handler.extractItem(slot, Math.min(max, preview.getMaxStackSize()), false);
            }
        }
        return ItemStack.EMPTY;
    }
}
