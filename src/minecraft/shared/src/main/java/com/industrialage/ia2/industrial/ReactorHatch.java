package com.industrialage.ia2.industrial;

import java.util.stream.IntStream;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A live inventory view that detaches as soon as the loaded reactor shell opens. */
final class ReactorHatch implements WorldlyContainer {
    private final PowerNode hatch;

    ReactorHatch(PowerNode hatch) {
        this.hatch = hatch;
    }

    private PowerInventory grid() {
        if (hatch.isRemoved() || !(hatch.getLevel() instanceof ServerLevel level)) return null;
        var core = ReactorVessel.core(hatch, level);
        return core == null ? null : core.inventory;
    }

    @Override
    public int getContainerSize() {
        return 54;
    }

    @Override
    public boolean isEmpty() {
        var grid = grid();
        return grid == null || grid.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        var grid = grid();
        return grid == null ? ItemStack.EMPTY : grid.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        var grid = grid();
        return grid == null ? ItemStack.EMPTY : grid.removeItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        var grid = grid();
        return grid == null ? ItemStack.EMPTY : grid.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        var grid = grid();
        if (grid != null && (stack.isEmpty() || grid.canPlaceItem(slot, stack)))
            grid.setItem(slot, stack);
    }

    @Override
    public void setChanged() {
        var grid = grid();
        if (grid != null) grid.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return !hatch.isRemoved() && grid() != null;
    }

    @Override
    public void clearContent() {
        var grid = grid();
        if (grid != null) grid.clearContent();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        var grid = grid();
        return grid != null && grid.canPlaceItem(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        return IntStream.range(0, 54).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        var grid = grid();
        return grid != null && grid.canTakeItemThroughFace(slot, stack, face);
    }
}
