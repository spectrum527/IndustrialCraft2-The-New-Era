package com.industrialage.ia2.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Container data for a meter, synchronized independently of the player's held item. */
public final class MeterMenu extends AbstractContainerMenu {
    /** Number of selectable readings, including voltage tier. */
    public static final int MODES = 4;

    static final int RESET = MODES;

    private final SimpleContainerData data = new SimpleContainerData(8);
    private final ServerLevel level;
    private final BlockPos position;
    private final MeterSampling.Watch watch;

    /** Creates the reader and its player inventory, attached to the target position server-side. */
    public MeterMenu(
            MenuType<MeterMenu> type,
            int id,
            Inventory player,
            ServerLevel level,
            BlockPos position) {
        super(type, id);
        this.level = level;
        this.position = position;
        watch = level == null ? null : MeterSampling.watch(level, position);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(
                        new Slot(
                                player,
                                9 + 9 * row + column,
                                8 + 18 * column,
                                MeterPanel.INVENTORY_TOP + 18 * row));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(player, column, 8 + 18 * column, MeterPanel.HOTBAR_TOP));
        }
        addDataSlots(data);
    }

    @Override
    public void broadcastChanges() {
        if (watch != null) {
            var sample = watch.read(mode());
            data.set(1, sample.count());
            setFloat(2, sample.min());
            setFloat(4, sample.max());
            setFloat(6, sample.average());
        }
        super.broadcastChanges();
    }

    private void setFloat(int index, double quantity) {
        int bits = Float.floatToIntBits((float) quantity);
        data.set(index, bits >>> 16);
        data.set(index + 1, bits & 0xffff);
    }

    private float getFloat(int index) {
        return Float.intBitsToFloat((data.get(index) << 16) | (data.get(index + 1) & 0xffff));
    }

    /** Reports the selected input, output, gain, or voltage reading. */
    public int mode() {
        return data.get(0);
    }

    /** Reports the synchronized current sample statistics. */
    public MeterSampling.Reading reading() {
        return new MeterSampling.Reading(data.get(1), getFloat(2), getFloat(4), getFloat(6));
    }

    @Override
    public boolean clickMenuButton(Player player, int selected) {
        if (selected < 0 || selected > RESET || !stillValid(player)) return false;
        if (selected == RESET) {
            if (watch != null) watch.reset();
        } else {
            data.set(0, selected);
        }
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack prior = stack.copy();
        boolean moved =
                index < 27
                        ? moveItemStackTo(stack, 27, 36, false)
                        : moveItemStackTo(stack, 0, 27, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return prior;
    }

    @Override
    public boolean stillValid(Player player) {
        return watch == null
                || (level.hasChunkAt(position)
                        && player.distanceToSqr(
                                        position.getX() + 0.5,
                                        position.getY() + 0.5,
                                        position.getZ() + 0.5)
                                < 64
                        && MeterPlatform.metered(level, position));
    }

    @Override
    public void removed(Player player) {
        if (watch != null) watch.close();
        super.removed(player);
    }
}
