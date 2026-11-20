package com.industrialage.ia2.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** Container data for a meter, synchronized independently of the player's held item. */
public final class MeterMenu extends AbstractContainerMenu {
    /** Number of selectable readings, including voltage tier. */
    public static final int MODES = 4;

    private final SimpleContainerData data = new SimpleContainerData(8);
    private final ServerLevel level;
    private final BlockPos position;
    private final MeterSampling.Watch watch;

    /** Creates the data-only container on the client or attached to a server position. */
    public MeterMenu(MenuType<MeterMenu> type, int id, ServerLevel level, BlockPos position) {
        super(type, id);
        this.level = level;
        this.position = position;
        watch = level == null ? null : MeterSampling.watch(level, position);
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
        if (selected < 0 || selected >= MODES || !stillValid(player)) return false;
        data.set(0, selected);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
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
