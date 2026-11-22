package com.industrialage.ia2.gui;

import com.industrialage.ia2.access.MachineAccess;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Native service for the authorization and synchronized automation settings of a fixed menu. */
public abstract class DeviceAccessMenu extends AbstractContainerMenu {
    private static final int FIRST_BUTTON = 224;
    private static final int LIMIT_BUTTON = FIRST_BUTTON + 24;
    private final Player opener;
    private final BlockEntity device;
    private int synchronizedAccess = Direction.NORTH.get3DDataValue() << 12;

    protected DeviceAccessMenu(MenuType<?> type, int id, Inventory inventory, BlockEntity device) {
        super(type, id);
        this.opener = inventory.player;
        this.device = device;
    }

    /**
     * Appends native data after existing device fields. The 16 bits describe one coherent state.
     */
    protected final void addAccessData() {
        addDataSlot(
                new DataSlot() {
                    @Override
                    public int get() {
                        if (device == null) return synchronizedAccess;
                        int facing = MachineAccess.front(device).get3DDataValue() << 12;
                        return (short)
                                (MachineAccess.packed(device)
                                        | facing
                                        | (canEdit(opener) ? 0x8000 : 0));
                    }

                    @Override
                    public void set(int value) {
                        synchronizedAccess = value;
                    }
                });
    }

    /** Returns the last permission, direction and six face modes received from the server. */
    public final int accessData() {
        return synchronizedAccess & 0xffff;
    }

    /** Resolves the front of a device from a synchronized state value. */
    public static Direction accessFront(int state) {
        return Direction.from3DDataValue(state >>> 12 & 7);
    }

    /** Returns the mode of one relative face from a synchronized state value. */
    public static int accessMode(int state, int face) {
        return state >>> (2 * face) & 3;
    }

    /** Identifies whether the viewer can currently change settings. */
    public static boolean accessEditable(int state) {
        return (state & 0x8000) != 0;
    }

    /** Builds the absolute assignment used by native container button clicks. */
    public static int accessButton(int face, int mode) {
        if (face < 0 || face >= 6 || mode < 0 || mode >= 4) return -1;
        return FIRST_BUTTON + face * 4 + mode;
    }

    private boolean canEdit(Player player) {
        return player instanceof ServerPlayer
                && player == opener
                && player.containerMenu == this
                && device != null
                && stillValid(player)
                && MachineAccess.mayConfigure(device, player);
    }

    @Override
    public final boolean clickMenuButton(Player player, int button) {
        // Forge 1.19.2 decodes the existing one-byte button ID as a signed value.
        int unsigned = button >= -32 && button <= -9 ? button + 256 : button;
        if (unsigned < FIRST_BUTTON || unsigned >= LIMIT_BUTTON) {
            return clickDeviceButton(player, button);
        }
        if (!canEdit(player)) return false;
        int choice = unsigned - FIRST_BUTTON;
        return MachineAccess.set(device, choice / 4, choice % 4);
    }

    /** Processes controls belonging to the original native machine menu. */
    protected boolean clickDeviceButton(Player player, int button) {
        return false;
    }
}
