package com.industrialage.ia2.gametest;

import com.industrialage.ia2.gameplay.PortableGear;
import com.industrialage.ia2.slottest.SlotRulesChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class SlotRulesHost implements SlotRulesChecks.Host {
    @Override
    public SlotRulesChecks.Port port(ServerLevel level, BlockPos pos, Direction side) {
        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null) return null;
        return new SlotRulesChecks.Port() {
            @Override
            public int offer(ItemStack stack, boolean simulate) {
                try (var transaction = Transaction.openRoot()) {
                    int moved =
                            ResourceHandlerUtil.insertStacking(
                                    handler, ItemResource.of(stack), stack.getCount(), transaction);
                    if (!simulate) transaction.commit();
                    return moved;
                }
            }

            @Override
            public int offerAt(int slot, ItemStack stack, boolean simulate) {
                try (var transaction = Transaction.openRoot()) {
                    int moved =
                            handler.insert(
                                    slot, ItemResource.of(stack), stack.getCount(), transaction);
                    if (!simulate) transaction.commit();
                    return moved;
                }
            }

            @Override
            public ItemStack take(boolean simulate) {
                try (var transaction = Transaction.openRoot()) {
                    var result =
                            ResourceHandlerUtil.extractFirst(
                                    handler, ignored -> true, 64, transaction);
                    if (!simulate) transaction.commit();
                    return result == null
                            ? ItemStack.EMPTY
                            : result.resource().toStack(result.amount());
                }
            }
        };
    }

    @Override
    public void charge(ItemStack battery, double eu) {
        PortableGear.restoreEnergy(battery, eu);
    }
}
