package com.industrialage.ia2.industrial;

import com.industrialage.ia2.gui.DeviceAccessMenu;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Synchronizes live measurements and protects each input, output and owner-editable shop slot. */
final class PowerMenu extends DeviceAccessMenu {
    final PowerKind kind;
    final PowerLayout layout;
    final boolean guest;
    final int playerStart;
    final PowerInventory live;
    final SimpleContainerData data = new SimpleContainerData(18);
    private final Container contents;
    final int machineSlots;

    PowerMenu(int id, Inventory player, PowerInventory inventory) {
        this(view(inventory.device, player.player), id, player, inventory.device.kind, inventory);
    }

    private static String view(PowerNode device, Player opener) {
        if (device.isFluidReactor()) return device.kind.path + "_fluid";
        return device.kind.trader()
                        && device.owner != null
                        && !device.owner.equals(opener.getUUID())
                ? device.kind.path + "_guest"
                : device.kind.path;
    }

    PowerMenu(String key, int id, Inventory player, PowerKind kind, PowerInventory inventory) {
        super(PowerMenus.type(key), id, player, inventory == null ? null : inventory.device);
        this.kind = kind;
        layout = PowerLayout.get(key);
        guest = key.endsWith("_guest");
        live = inventory;
        machineSlots =
                kind == PowerKind.NUCLEAR_REACTOR && key.endsWith("_fluid")
                        ? 58
                        : kind == PowerKind.NUCLEAR_REACTOR ? 54 : PowerInventory.size(kind);
        contents = inventory == null ? new SimpleContainer(machineSlots) : inventory;
        for (int i = 0; i < machineSlots; i++) {
            int index = i;
            int[] at = layout.origin(i);
            if (kind == PowerKind.MAGNETIZER && i == 0) {
                addSlot(
                        com.industrialage.ia2.gui.EquipmentSlots.worn(
                                player,
                                net.minecraft.world.entity.EquipmentSlot.FEET,
                                at[0],
                                at[1]));
                continue;
            }
            int backing = i;
            addSlot(
                    new Slot(contents, backing, at[0], at[1]) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            if (!isActive()) return false;
                            if (live != null) {
                                return permitted(index, player.player)
                                        && live.canPlaceItem(backing, stack);
                            }
                            return kind != PowerKind.GEO_GENERATOR
                                    || PowerInventory.acceptsGeothermalFuel(backing, stack);
                        }

                        @Override
                        public boolean mayPickup(Player who) {
                            return isActive() && permitted(index, who);
                        }

                        @Override
                        public boolean isActive() {
                            return exposed(index);
                        }

                        @Override
                        public int getMaxStackSize(ItemStack stack) {
                            return live == null
                                    ? stack.getMaxStackSize()
                                    : Math.min(stack.getMaxStackSize(), live.limit(backing, stack));
                        }
                    });
        }
        if (kind.storage()) {
            int position = 8;
            for (var armor :
                    new net.minecraft.world.entity.EquipmentSlot[] {
                        net.minecraft.world.entity.EquipmentSlot.FEET,
                        net.minecraft.world.entity.EquipmentSlot.LEGS,
                        net.minecraft.world.entity.EquipmentSlot.CHEST,
                        net.minecraft.world.entity.EquipmentSlot.HEAD
                    }) {
                addSlot(com.industrialage.ia2.gui.EquipmentSlots.worn(player, armor, position, 84));
                position += 18;
            }
        }
        playerStart = slots.size();
        var inv = layout.inventory();
        for (int i = 0; i < 27; i++)
            addSlot(new Slot(player, 9 + i, inv[0] + 18 * (i % 9), inv[1] + 18 * (i / 9)));
        for (int i = 0; i < 9; i++) addSlot(new Slot(player, i, inv[0] + 18 * i, inv[2]));
        addDataSlots(data);
        addAccessData();
        measure();
    }

    boolean core() {
        return kind == PowerKind.NUCLEAR_REACTOR;
    }

    private boolean permitted(int index, Player who) {
        return live == null
                || !kind.trader()
                || index >= 2
                || who.getUUID().equals(live.device.owner);
    }

    boolean exposed(int i) {
        if (!layout.slots().containsKey(i)) return false;
        return !core()
                || i >= 54
                || i % 9 < (live == null ? Math.max(3, data.get(0)) : live.device.coreWidth());
    }

    @Override
    protected boolean clickDeviceButton(Player who, int action) {
        if (live == null || !stillValid(who)) return false;
        var device = live.device;
        if (!(device.getLevel() instanceof net.minecraft.server.level.ServerLevel level))
            return false;
        if ((kind.lowTier() > 0 || kind.storage()) && action == 0) device.changeMode(level);
        else if (kind == PowerKind.ENERGY_O_MAT
                && !guest
                && who.getUUID().equals(device.owner)
                && (action == 0 || action == 1)) {
            int[] rates = {1000, 5000, 10000};
            int current = device.saleSize == 5000 ? 1 : device.saleSize == 10000 ? 2 : 0;
            device.saleSize = rates[Math.floorMod(current + (action == 1 ? 1 : -1), rates.length)];
            device.setChanged();
        } else return false;
        broadcastChanges();
        return true;
    }

    private void value(int i, int n) {
        data.set(i, n & 0xffff);
        data.set(i + 1, n >>> 16);
    }

    int value(int i) {
        return (data.get(i) & 0xffff) | ((data.get(i + 1) & 0xffff) << 16);
    }

    private void measure() {
        if (live == null) return;
        var n = live.device;
        data.set(0, n.coreWidth());
        data.set(1, n.isFluidReactor() ? 1 : 0);
        value(2, (int) Math.round(n.storedEu()));
        value(4, n.hullHeat());
        value(6, n.reactor.limit());
        value(8, (int) Math.round(n.reactorOutput() * 10));
        var tank = kind == PowerKind.REACTOR_FLUID_PORT ? n.coolantTank() : n;
        data.set(10, tank == null ? 0 : tank.coolant);
        data.set(11, tank == null ? 0 : tank.hotCoolant);
        value(14, (int) n.capacity());
        data.set(16, n.saleSize);
        data.set(17, n.getBlockState().getValue(PowerBlock.ACTIVE) ? 1 : 0);
        value(
                12,
                kind.trader()
                        ? (kind == PowerKind.ENERGY_O_MAT ? (int) Math.round(n.credit) : n.trades)
                        : kind == PowerKind.GEO_GENERATOR ? n.liquid : n.mode);
    }

    @Override
    public void broadcastChanges() {
        measure();
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return contents.stillValid(player)
                && (live == null
                        || !core()
                        || live.device.isFluidReactor() == (machineSlots == 58));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = getSlot(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var prior = stack.copy();
        boolean changed;
        if (index < playerStart) changed = moveItemStackTo(stack, playerStart, slots.size(), true);
        else if (kind.trader()) {
            int target =
                    kind == PowerKind.ENERGY_O_MAT && PowerPlatform.acceptsCharge(stack, 4) ? 3 : 2;
            changed = insertIntoDevice(stack, target, target + 1);
        } else changed = insertIntoDevice(stack, 0, playerStart);
        if (!changed) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return prior;
    }

    private boolean insertIntoDevice(ItemStack incoming, int start, int end) {
        int before = incoming.getCount();
        for (int pass = 0; pass < 2 && !incoming.isEmpty(); pass++) {
            for (int i = start; i < end && !incoming.isEmpty(); i++) {
                var slot = getSlot(i);
                if (!slot.isActive() || !slot.mayPlace(incoming)) continue;
                var current = slot.getItem();
                if (pass == 0) {
                    if (current.isEmpty() || !WorkshopPlatform.sameItem(current, incoming))
                        continue;
                    int add =
                            Math.min(
                                    incoming.getCount(),
                                    Math.min(
                                                    current.getMaxStackSize(),
                                                    slot.getMaxStackSize(incoming))
                                            - current.getCount());
                    if (add > 0) {
                        current.grow(add);
                        incoming.shrink(add);
                        slot.setChanged();
                    }
                } else if (current.isEmpty()) {
                    int count = Math.min(incoming.getCount(), slot.getMaxStackSize(incoming));
                    if (count > 0) slot.set(incoming.split(count));
                }
            }
        }
        return before != incoming.getCount();
    }
}
