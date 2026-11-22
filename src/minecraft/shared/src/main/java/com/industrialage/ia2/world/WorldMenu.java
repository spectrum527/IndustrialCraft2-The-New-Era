package com.industrialage.ia2.world;

import com.industrialage.ia2.gui.DeviceAccessMenu;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative machine contents and synchronized panel controls. */
final class WorldMenu extends DeviceAccessMenu {
    final WorldMachineBlock.Kind kind;
    final WorldLayout layout;
    final SimpleContainerData data = new SimpleContainerData(8);
    private final WorldInventory live;
    private final Container contents;
    final int deviceSlots;

    WorldMenu(int id, Inventory player, WorldMachine machine) {
        this(WorldMenus.type(machine.kind), id, player, machine.kind, machine.inventory);
    }

    WorldMenu(
            MenuType<WorldMenu> type,
            int id,
            Inventory player,
            WorldMachineBlock.Kind kind,
            WorldInventory inventory) {
        super(type, id, player, inventory == null ? null : inventory.owner());
        this.kind = kind;
        live = inventory;
        layout = WorldLayout.of(kind);
        deviceSlots = kind.size();
        contents = live == null ? new SimpleContainer(deviceSlots) : live;
        for (int i = 0; i < deviceSlots; i++) {
            final int index = i;
            addSlot(
                    new Slot(contents, i, layout.x[i], layout.y[i]) {
                        @Override
                        public boolean isActive() {
                            return layout.page[index] == -1 || layout.page[index] == data.get(0);
                        }

                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return isActive() && (live == null || live.canPlaceItem(index, stack));
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            return isActive();
                        }
                    });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(
                        new Slot(
                                player,
                                9 + row * 9 + col,
                                8 + col * 18,
                                layout.playerY + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(player, col, 8 + col * 18, layout.hotbarY));
        addDataSlots(data);
        addAccessData();
        measure();
    }

    private void measure() {
        if (live == null) return;
        var machine = live.owner();
        data.set(1, (int) machine.storedEnergy());
        data.set(2, machine.fluid(false));
        data.set(3, machine.fluid(true));
        data.set(
                4,
                (kind == WorldMachineBlock.Kind.MINER ? machine.withdrawing : machine.blacklist)
                        ? 1
                        : 0);
        data.set(5, machine.ticketX + 4);
        data.set(6, machine.ticketZ + 4);
        data.set(7, machine.chunkEnabled ? 1 : 0);
    }

    @Override
    public void broadcastChanges() {
        measure();
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return contents.stillValid(player);
    }

    @Override
    protected boolean clickDeviceButton(Player player, int id) {
        if (!stillValid(player)) return false;
        if (id >= 0 && id < layout.pages) {
            data.set(0, id);
            return true;
        }
        if (live == null) return false;
        var machine = live.owner();
        if (kind == WorldMachineBlock.Kind.ADVANCED_MINER && id == 20) {
            machine.blacklist = !machine.blacklist;
            machine.cursor = 0;
            machine.scanDepth = 1;
        } else if (kind == WorldMachineBlock.Kind.MINER && id == 21) {
            machine.withdrawing = !machine.withdrawing;
        } else if (kind == WorldMachineBlock.Kind.CHUNK_LOADER && id >= 100 && id < 181) {
            int cell = id - 100;
            machine.ticketX = cell % 9 - 4;
            machine.ticketZ = cell / 9 - 4;
            machine.chunkEnabled = true;
        } else if (kind == WorldMachineBlock.Kind.CHUNK_LOADER && id == 22) {
            machine.chunkEnabled = !machine.chunkEnabled;
        } else return false;
        machine.setChanged();
        measure();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), prior = stack.copy();
        boolean moved =
                index < deviceSlots
                        ? com.industrialage.ia2.gui.MenuTransfer.move(
                                stack,
                                slots,
                                deviceSlots,
                                slots.size(),
                                true,
                                WorldNative::sameStack)
                        : com.industrialage.ia2.gui.MenuTransfer.move(
                                stack, slots, 0, deviceSlots, false, WorldNative::sameStack);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return prior;
    }
}
