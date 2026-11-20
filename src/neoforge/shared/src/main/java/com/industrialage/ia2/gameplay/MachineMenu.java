package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.gameplay.MachineKind.SlotRole;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Inventory and display data synchronized by Minecraft's normal container protocol. */
public final class MachineMenu extends AbstractContainerMenu {
    /** Width and height of the machine inventory panel. */
    public static final int WIDTH = 176, HEIGHT = 177;

    private static final int DATA_COUNT = 7;
    private static final int FIRST_INVENTORY_ROW = 95;
    private static final int HOTBAR = 153;
    private final MachineKind kind;
    private final Container container;
    private final ContainerData machineData;

    MachineMenu(int id, Inventory player, RegistryFriendlyByteBuf buffer) {
        this(id, player, MachineKind.fromPath(buffer.readUtf(128)));
    }

    private MachineMenu(int id, Inventory player, MachineKind kind) {
        this(
                id,
                player,
                kind,
                new SimpleContainer(kind.slots.size()),
                new SimpleContainerData(DATA_COUNT));
    }

    MachineMenu(int id, Inventory player, IndustrialMachine machine) {
        this(id, player, machine.kind(), machine, new Status(machine));
    }

    private MachineMenu(
            int id,
            Inventory player,
            MachineKind kind,
            Container container,
            ContainerData machineData) {
        super(IndustrialContent.MACHINE_MENU.get(), id);
        this.kind = kind;
        this.container = container;
        this.machineData = machineData;
        for (int i = 0; i < kind.slots.size(); i++) {
            var role = kind.slots.get(i);
            addSlot(
                    new Slot(container, i, machineSlotX(i), machineSlotY(i)) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            if (role == SlotRole.UPGRADE)
                                return com.industrialage.ia2.world.MachineAutomation
                                        .isProcessingUpgrade(stack);
                            return role != SlotRole.OUTPUT
                                    && container.canPlaceItem(getContainerSlot(), stack);
                        }

                        @Override
                        public int getMaxStackSize(ItemStack stack) {
                            return (role == SlotRole.CHARGE || role == SlotRole.DISCHARGE)
                                            && PortableGear.electricalItem(stack)
                                    ? 1
                                    : super.getMaxStackSize(stack);
                        }
                    });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(
                        new Slot(
                                player,
                                9 + 9 * row + col,
                                8 + 18 * col,
                                FIRST_INVENTORY_ROW + 18 * row));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(player, col, 8 + 18 * col, HOTBAR));
        }
        addDataSlots(machineData);
    }

    /** Number of slots held by the machine itself. */
    public int machineSlots() {
        return kind.slots.size();
    }

    /** Horizontal coordinate of an individual machine slot. */
    public int machineSlotX(int slot) {
        if (kind.electricProcessor()) {
            return switch (kind.slots.get(slot)) {
                case INPUT -> 23;
                case OUTPUT -> 101;
                case DISCHARGE -> 62;
                default -> 151;
            };
        }
        return WIDTH * (1 + 2 * slot) / (2 * kind.slots.size()) - 8;
    }

    /** Vertical location of a slot, sharing the column with the processor's four upgrades. */
    public int machineSlotY(int slot) {
        return upgradeSlot(slot) ? 7 + 18 * (slot - 3) : 40;
    }

    /** Identifies the grouped upgrade column for the native GUI. */
    public boolean upgradeSlot(int slot) {
        return kind.slots.get(slot) == SlotRole.UPGRADE;
    }

    /** Translated slot label shown to the player. */
    public Component slotLabel(int slot) {
        return Component.translatable("container.ic2." + kind.slots.get(slot).label);
    }

    /** Energy, rounded to whole EU for display. */
    public int energy() {
        return wordPair(0);
    }

    /** Energy capacity in EU for the selected machine. */
    public int capacity() {
        return wordPair(2);
    }

    private int wordPair(int index) {
        return (machineData.get(index) & 0xffff) | ((machineData.get(index + 1) & 0xffff) << 16);
    }

    /** Paid working ticks in the current operation. */
    public int progress() {
        return machineData.get(4);
    }

    /** Number of work ticks per operation, or zero without a processor. */
    public int duration() {
        return machineData.get(5);
    }

    /** Remaining fuel time, or zero when waiting for fuel. */
    public int fuelRemaining() {
        return machineData.get(6) & 0xffff;
    }

    /** Whether the machine can accept combustible fuel. */
    public boolean usesFuel() {
        return kind.slots.contains(SlotRole.FUEL);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlots()) {
            if (!moveItemStackTo(stack, machineSlots(), slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            for (int destination = 0; destination < machineSlots(); destination++) {
                if (slots.get(destination).mayPlace(stack)
                        && moveItemStackTo(stack, destination, destination + 1, false)) {
                    break;
                }
            }
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    private record Status(IndustrialMachine machine) implements ContainerData {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) machine.energy() & 0xffff;
                case 1 -> (int) machine.energy() >>> 16;
                case 2 -> (int) machine.capacity() & 0xffff;
                case 3 -> (int) machine.capacity() >>> 16;
                case 4 -> machine.progress();
                case 5 -> machine.duration();
                case 6 -> Math.min(machine.fuelRemaining(), 0xffff);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Only the client-side copy accepts incoming synchronized display values.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    }
}
