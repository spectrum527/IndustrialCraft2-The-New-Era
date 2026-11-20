package com.industrialage.ia2.industrial;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Synchronizes live measurements and protects each input, output and owner-editable shop slot. */
final class PowerMenu extends AbstractContainerMenu {
    final PowerKind kind;
    final PowerInventory live;
    final SimpleContainerData data = new SimpleContainerData(14);
    private final Container contents;
    final int machineSlots;

    PowerMenu(int id, Inventory player, PowerInventory inventory) {
        this(PowerMenus.type(inventory.device.kind), id, player, inventory.device.kind, inventory);
    }

    PowerMenu(
            MenuType<PowerMenu> type,
            int id,
            Inventory player,
            PowerKind kind,
            PowerInventory inventory) {
        super(type, id);
        this.kind = kind;
        live = inventory;
        machineSlots = core() ? 54 : 9;
        contents = inventory == null ? new SimpleContainer(machineSlots) : inventory;
        for (int i = 0; i < machineSlots; i++) {
            int index = i;
            addSlot(
                    new Slot(contents, i, slotX(i), slotY(i)) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return isActive()
                                    && (live == null
                                            || permitted(index, player.player)
                                                    && live.canPlaceItem(index, stack));
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
                                    : Math.min(stack.getMaxStackSize(), live.limit(stack));
                        }
                    });
        }
        int x = core() ? 26 : 25, y = core() ? 161 : 99;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(player, 9 + row * 9 + col, x + 18 * col, y + 18 * row));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(player, col, x + 18 * col, y + (core() ? 60 : 58)));
        addDataSlots(data);
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
        if (core())
            return i % 9 < (live == null ? Math.max(3, data.get(0)) : live.device.coreWidth());
        if (i == kind.chargeSlot() || i == 1 && kind.storage()) return true;
        return switch (kind) {
            case RT_GENERATOR -> i < 6;
            case WATER_GENERATOR, GEO_GENERATOR, NUKE -> i < 2;
            case ENERGY_O_MAT -> i == 0 || i == 2 || i == 3;
            case TRADE_O_MAT -> i <= 3;
            case RCI_RSH, RCI_LZH -> i == 0;
            default -> false;
        };
    }

    private int slotX(int i) {
        return core()
                ? 26 + i % 9 * 18
                : kind == PowerKind.RT_GENERATOR ? 23 + i * 24 : 25 + i * 46;
    }

    private int slotY(int i) {
        return core() ? 25 + i / 9 * 18 : 35;
    }

    String role(int i) {
        if (i == kind.chargeSlot()) return "charge";
        if (kind.storage() && i == 1) return "discharge";
        return switch (kind) {
            case NUCLEAR_REACTOR -> "component";
            case TRADE_O_MAT, ENERGY_O_MAT ->
                    switch (i) {
                        case 0 -> "price";
                        case 1 -> "goods";
                        case 2 -> "payment";
                        default -> kind == PowerKind.ENERGY_O_MAT ? "charge" : "delivery";
                    };
            case NUKE -> i == 0 ? "cartridge" : "slot";
            case WATER_GENERATOR, GEO_GENERATOR -> i == 0 ? "fuel" : "empty";
            case RT_GENERATOR -> "fuel";
            default -> "input";
        };
    }

    int width() {
        return core() ? 212 : 210;
    }

    int height() {
        return core() ? 242 : 184;
    }

    boolean fluid() {
        return data.get(1) != 0;
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
        data.set(10, n.coolant);
        data.set(11, n.hotCoolant);
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
        return contents.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = getSlot(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var prior = stack.copy();
        boolean changed;
        if (index < machineSlots)
            changed = moveItemStackTo(stack, machineSlots, slots.size(), true);
        else if (kind.trader()) {
            int target =
                    kind == PowerKind.ENERGY_O_MAT && PowerPlatform.acceptsCharge(stack, 4) ? 3 : 2;
            changed = moveItemStackTo(stack, target, target + 1, false);
        } else changed = moveItemStackTo(stack, 0, machineSlots, false);
        if (!changed) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return prior;
    }
}
