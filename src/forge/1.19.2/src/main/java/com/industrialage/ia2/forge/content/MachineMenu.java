package com.industrialage.ia2.forge.content;

import com.industrialage.ia2.forge.content.MachineKind.Slot;
import java.util.Locale;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** A server-authoritative machine inventory with synchronized progress and energy. */
public final class MachineMenu extends AbstractContainerMenu {
    private static final int ENERGY_LOW = 0;
    private static final int ENERGY_HIGH = 1;
    private static final int CAPACITY_LOW = 2;
    private static final int CAPACITY_HIGH = 3;
    private static final int PROGRESS = 4;
    private static final int DURATION = 5;
    private static final int WORKING = 6;
    private static final int MODE = 7;
    private static final int DATA_SIZE = 8;
    private final MachineInventory inventory;
    private final ContainerData data;
    private final StorageDevice storage;

    MachineMenu(int id, Inventory player, ForgeDevice machine) {
        this(
                id,
                player,
                machine.inventory(),
                live(machine),
                machine instanceof StorageDevice device ? device : null);
    }

    private MachineMenu(
            int id,
            Inventory player,
            MachineInventory inventory,
            ContainerData data,
            StorageDevice storage) {
        super(ForgeContent.MACHINE_MENU.get(), id);
        this.inventory = inventory;
        this.data = data;
        this.storage = storage;
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            var kind = inventory.kind.slots.get(index);
            int x =
                    switch (kind) {
                        case OUTPUT -> 111;
                        case CHARGE -> inventory.kind.storesEnergy() ? 55 : 111;
                        case DISCHARGE -> inventory.kind.storesEnergy() ? 111 : 55;
                        case INPUT, FUEL -> 55;
                        case UPGRADE -> 151;
                    };
            int y =
                    switch (kind) {
                        case INPUT -> 16;
                        case FUEL -> inventory.kind == MachineKind.GENERATOR ? 30 : 52;
                        case OUTPUT, CHARGE -> 30;
                        case DISCHARGE -> inventory.kind.storesEnergy() ? 30 : 52;
                        case UPGRADE -> 7 + 18 * (index - 3);
                    };
            addSlot(
                    new net.minecraft.world.inventory.Slot(inventory, index, x, y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return inventory.canPlaceItem(getSlotIndex(), stack);
                        }

                        @Override
                        public int getMaxStackSize(ItemStack stack) {
                            return (kind == MachineKind.Slot.CHARGE
                                                    || kind == MachineKind.Slot.DISCHARGE)
                                            && com.industrialage.ia2.forge.equipment.ForgeGear
                                                    .electricalItem(stack)
                                    ? 1
                                    : super.getMaxStackSize(stack);
                        }
                    });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(
                        new net.minecraft.world.inventory.Slot(
                                player, column + (row + 1) * 9, 8 + 18 * column, 84 + 18 * row));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(player, column, 8 + 18 * column, 142));
        }
        addDataSlots(data);
    }

    static MachineMenu fromNetwork(int id, Inventory player, FriendlyByteBuf buffer) {
        return new MachineMenu(
                id,
                player,
                new MachineInventory(buffer.readEnum(MachineKind.class), null),
                new SimpleContainerData(DATA_SIZE),
                null);
    }

    private static ContainerData live(ForgeDevice machine) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case ENERGY_LOW -> (int) machine.energy() & 0xFFFF;
                    case ENERGY_HIGH -> (int) machine.energy() >>> 16;
                    case CAPACITY_LOW -> (int) machine.capacity() & 0xFFFF;
                    case CAPACITY_HIGH -> (int) machine.capacity() >>> 16;
                    case PROGRESS -> machine.progress();
                    case DURATION -> machine.duration();
                    case WORKING -> machine.active() ? 1 : 0;
                    case MODE -> machine instanceof StorageDevice device ? device.mode() : 0;
                    default -> throw new IndexOutOfBoundsException(index);
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_SIZE;
            }
        };
    }

    /** Returns the synchronized stored EU rounded down to a whole unit. */
    public int energy() {
        return data.get(ENERGY_HIGH) << 16 | data.get(ENERGY_LOW) & 0xFFFF;
    }

    /** Returns the synchronized buffer capacity in EU. */
    public int capacity() {
        return data.get(CAPACITY_HIGH) << 16 | data.get(CAPACITY_LOW) & 0xFFFF;
    }

    /** Server-synchronized number of paid work ticks. */
    public int progress() {
        return data.get(PROGRESS);
    }

    /** Server-synchronized duration under the installed upgrades. */
    public int duration() {
        return data.get(DURATION);
    }

    /** Returns the completion ratio of the current job. */
    public float progressRatio() {
        return data.get(DURATION) > 0 ? (float) data.get(PROGRESS) / data.get(DURATION) : 0;
    }

    /** Returns whether the latest server tick was active. */
    public boolean active() {
        return data.get(WORKING) == 1;
    }

    /** Returns the localized purpose of a machine slot, or null for a player inventory slot. */
    public Component slotLabel(int menuIndex) {
        if (menuIndex < 0 || menuIndex >= inventory.getContainerSize()) {
            return null;
        }
        return Component.translatable(
                "gui.ic2.slot."
                        + inventory.kind.slots.get(menuIndex).name().toLowerCase(Locale.ROOT));
    }

    /** Indicates that the menu has a storage redstone control. */
    public boolean storageControl() {
        return inventory.kind.storesEnergy();
    }

    /** Returns the selected storage redstone mode. */
    public int redstoneMode() {
        return data.get(MODE);
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (storage == null || button != 0 || !stillValid(player)) {
            return false;
        }
        storage.cycleMode();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        var stack = slot.getItem();
        var before = stack.copy();
        int firstPlayerSlot = inventory.getContainerSize();
        if (index < firstPlayerSlot) {
            if (!moveItemStackTo(stack, firstPlayerSlot, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            for (Slot role : new Slot[] {Slot.CHARGE, Slot.DISCHARGE, Slot.FUEL, Slot.INPUT}) {
                int target = inventory.kind.slots.indexOf(role);
                if (target >= 0
                        && slots.get(target).mayPlace(stack)
                        && moveItemStackTo(stack, target, target + 1, false)) {
                    break;
                }
            }
            if (stack.getCount() == before.getCount() && firstPlayerSlot > 3) {
                moveItemStackTo(stack, 3, firstPlayerSlot, false);
            }
            if (stack.getCount() == before.getCount()) {
                int hotbar = firstPlayerSlot + 27;
                boolean moved =
                        index < hotbar
                                ? moveItemStackTo(stack, hotbar, slots.size(), false)
                                : moveItemStackTo(stack, firstPlayerSlot, hotbar, false);
                if (!moved) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return before;
    }
}
