package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.content.MachineLayout;
import com.industrialage.ia2.gameplay.MachineKind.SlotRole;
import com.industrialage.ia2.gui.DeviceAccessMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Inventory and display data synchronized by Minecraft's normal container protocol. */
public final class MachineMenu extends DeviceAccessMenu {
    private static final int DATA_COUNT = 9;
    private final MachineKind kind;
    private final MachineLayout layout;
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
        super(
                IndustrialContent.MACHINE_MENU.get(),
                id,
                player,
                container instanceof IndustrialMachine machine ? machine : null);
        this.kind = kind;
        this.layout = MachineLayout.byPath(kind.path);
        this.container = container;
        this.machineData = machineData;
        for (int i = 0; i < kind.slots.size(); i++) {
            var role = kind.slots.get(i);
            var position = layout.item(slotRole(i), Math.max(0, i - 3));
            addSlot(
                    new Slot(container, i, position.x(), position.y()) {
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
                            return role == SlotRole.CHARGE || role == SlotRole.DISCHARGE
                                    ? Math.min(
                                            super.getMaxStackSize(stack),
                                            PortableGear.powerSlotLimit(stack))
                                    : super.getMaxStackSize(stack);
                        }
                    });
        }
        if (layout == MachineLayout.STORAGE) {
            int x = 8;
            for (var armor :
                    new EquipmentSlot[] {
                        EquipmentSlot.FEET,
                        EquipmentSlot.LEGS,
                        EquipmentSlot.CHEST,
                        EquipmentSlot.HEAD
                    }) {
                addSlot(com.industrialage.ia2.gui.EquipmentSlots.worn(player, armor, x, 84));
                x += 18;
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(
                        new Slot(
                                player,
                                9 + 9 * row + col,
                                8 + 18 * col,
                                layout.inventoryTop() + 1 + 18 * row));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(player, col, 8 + 18 * col, layout.hotbarTop() + 1));
        }
        addDataSlots(machineData);
        addAccessData();
    }

    /** Number of slots held by the machine itself. */
    public int machineSlots() {
        return kind.slots.size();
    }

    /** Authored panel type and native inventory anchors. */
    public MachineLayout layout() {
        return layout;
    }

    /** Purpose of a machine slot within this panel. */
    public MachineLayout.Kind slotRole(int slot) {
        return MachineLayout.Kind.valueOf(kind.slots.get(slot).name());
    }

    /** Translated slot label shown to the player. */
    public Component slotLabel(int slot) {
        return slot >= 0 && slot < machineSlots()
                ? Component.translatable("container.ic2." + kind.slots.get(slot).label)
                : null;
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

    /** Remaining heat or burn time relative to the current fuel's initial duration. */
    public double fuelRatio() {
        return machineData.get(8) / 10_000.0;
    }

    /** Whether the native inventory takes combustible fuel. */
    public boolean usesFuel() {
        return kind.slots.contains(SlotRole.FUEL);
    }

    /** Currently selected redstone behavior of the energy storage. */
    public int redstoneMode() {
        return machineData.get(7);
    }

    @Override
    protected boolean clickDeviceButton(Player player, int id) {
        if (id == 0
                && layout.storage()
                && stillValid(player)
                && player instanceof ServerPlayer server
                && container instanceof IndustrialMachine machine) {
            machine.cycleRedstoneMode(server);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int firstInventory = slots.size() - 36;
        if (index < firstInventory) {
            if (!moveItemStackTo(stack, firstInventory, slots.size(), true)) {
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
                case 7 -> machine.redstoneMode();
                case 8 ->
                        machine.fuelCapacity() == 0
                                ? 0
                                : (int)
                                        Math.round(
                                                10_000.0
                                                        * machine.fuelRemaining()
                                                        / machine.fuelCapacity());
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
