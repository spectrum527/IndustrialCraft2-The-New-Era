package com.industrialage.ia2.industrial;

import com.industrialage.ia2.world.WorldCrafting;
import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The drawn storage grid and the server share the same persistent inventory indices. */
final class BoxMenu extends AbstractContainerMenu {
    final BoxKind kind;
    final BoxNode node;
    final Container inventory;
    final int width, height, inventoryX, inventoryY, playerStart;
    private long resolvedRevision = Long.MIN_VALUE;
    private final SimpleContainer results = new SimpleContainer(3);

    BoxMenu(int id, Inventory player, BoxKind kind, BoxNode node, int start) {
        super(BoxMenus.type(kind), id);
        this.kind = kind;
        this.node = node;
        inventory = node == null ? new SimpleContainer(kind.slots) : node.items;
        boolean bench = kind == BoxKind.INDUSTRIAL_WORKBENCH;
        int columns = kind == BoxKind.IRIDIUM_STORAGE_BOX ? 18 : 9;
        int top = kind == BoxKind.PERSONAL_CHEST ? 19 : 17;
        width = bench ? 194 : columns * 18 + 14;
        height =
                bench
                        ? 228
                        : (kind == BoxKind.PERSONAL_CHEST
                                ? 222
                                : 112 + 18 * (kind.slots / columns));
        inventoryX = columns == 18 ? 89 : 8;
        inventoryY = bench ? 146 : height - 82;
        if (bench) {
            for (int i = 0; i < 9; i++)
                addSlot(new Slot(inventory, i, 30 + 18 * (i % 3), 43 + 18 * (i / 3)));
            for (int i = 0; i < 4; i++) {
                int slot = i;
                addSlot(
                        new Slot(inventory, 9 + i, 7 + 84 * (i / 2) + 18 * (i % 2), 17) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return slot % 2 != 0
                                        || PowerPlatform.path(stack)
                                                .equals(slot == 0 ? "forge_hammer" : "cutter");
                            }
                        });
            }
            for (int i = 0; i < 18; i++)
                addSlot(new Slot(inventory, 13 + i, 8 + 18 * (i % 9), 106 + 18 * (i / 9)));
            for (int station = 0; station < 3; station++) {
                int stationIndex = station;
                addSlot(
                        new Slot(
                                results,
                                stationIndex,
                                station == 0 ? 124 : station == 1 ? 69 : 153,
                                station == 0 ? 61 : 17) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return false;
                            }

                            @Override
                            public boolean mayPickup(Player who) {
                                if (node == null) return true;
                                var recipe = recipe(stationIndex);
                                return recipe != null && sameResult(recipe.output(), getItem());
                            }

                            @Override
                            public void onTake(Player who, ItemStack taken) {
                                if (node != null) craft(stationIndex, who);
                                super.onTake(who, taken);
                            }
                        });
            }
        } else {
            // Old programmatic page views still expose the same backing indices.
            for (int i = 0; i < kind.slots - start; i++)
                addSlot(
                        new Slot(
                                inventory,
                                start + i,
                                8 + 18 * (i % columns),
                                top + 18 * (i / columns)));
        }
        playerStart = slots.size();
        for (int i = 0; i < 27; i++)
            addSlot(new Slot(player, 9 + i, inventoryX + 18 * (i % 9), inventoryY + 18 * (i / 9)));
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(player, i, inventoryX + 18 * i, inventoryY + 58));
        refresh();
    }

    private static boolean sameResult(ItemStack a, ItemStack b) {
        return !a.isEmpty()
                && !b.isEmpty()
                && a.getCount() == b.getCount()
                && WorkshopPlatform.sameItem(a, b);
    }

    private int inputs(int station) {
        return station == 0 ? 9 : 2;
    }

    private int first(int station) {
        return station == 0 ? 0 : 9 + 2 * (station - 1);
    }

    private WorldCrafting.Result recipe(int station) {
        if (node == null || !(node.getLevel() instanceof ServerLevel level)) return null;
        if (station != 0
                && !PowerPlatform.path(inventory.getItem(first(station)))
                        .equals(station == 1 ? "forge_hammer" : "cutter")) return null;
        var input = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        for (int i = 0; i < inputs(station); i++)
            input.set(i, inventory.getItem(first(station) + i));
        return WorldCrafting.result(level, input);
    }

    private void refresh() {
        if (kind != BoxKind.INDUSTRIAL_WORKBENCH
                || node == null
                || resolvedRevision == node.inventoryRevision) return;
        resolvedRevision = node.inventoryRevision;
        for (int s = 0; s < 3; s++) {
            var match = recipe(s);
            var next = match == null ? ItemStack.EMPTY : match.output();
            var old = results.getItem(s);
            if (!(old.isEmpty() && next.isEmpty()) && !sameResult(old, next))
                results.setItem(s, next);
        }
    }

    private void craft(int station, Player who) {
        var match = recipe(station);
        if (match == null) return;
        for (int i = 0; i < inputs(station); i++) {
            int input = first(station) + i;
            if (inventory.getItem(input).isEmpty()) continue;
            inventory.removeItem(input, 1);
            var remainder = match.remainders().get(i).copy();
            if (remainder.isEmpty()) continue;
            var available = inventory.getItem(input);
            if (available.isEmpty()) inventory.setItem(input, remainder);
            else if (WorkshopPlatform.sameItem(available, remainder)
                    && available.getCount() + remainder.getCount() <= available.getMaxStackSize())
                available.grow(remainder.getCount());
            else if (!who.getInventory().add(remainder)) who.drop(remainder, false);
        }
        inventory.setChanged();
        refresh();
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return node == null || node.inReach(player);
    }

    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var source = getSlot(index);
        if (!source.hasItem() || !source.mayPickup(who)) return ItemStack.EMPTY;
        var item = source.getItem();
        var original = item.copy();
        boolean output =
                kind == BoxKind.INDUSTRIAL_WORKBENCH && index >= kind.slots && index < playerStart;
        if (output && !canStore(item)) return ItemStack.EMPTY;
        boolean moved =
                index < playerStart
                        ? moveItemStackTo(item, playerStart, slots.size(), true)
                        : moveItemStackTo(
                                item,
                                kind == BoxKind.INDUSTRIAL_WORKBENCH ? 13 : 0,
                                kind == BoxKind.INDUSTRIAL_WORKBENCH ? 31 : playerStart,
                                false);
        if (!moved) return ItemStack.EMPTY;
        if (item.isEmpty()) source.set(ItemStack.EMPTY);
        else source.setChanged();
        source.onTake(who, original);
        return original;
    }

    @Override
    public boolean clickMenuButton(Player who, int action) {
        if (kind != BoxKind.INDUSTRIAL_WORKBENCH || action != 0 || node == null || !stillValid(who))
            return false;
        for (int i = 0; i < 9; i++) {
            var item = inventory.removeItemNoUpdate(i);
            if (!item.isEmpty() && !who.getInventory().add(item)) who.drop(item, false);
        }
        inventory.setChanged();
        broadcastChanges();
        return true;
    }

    private boolean canStore(ItemStack stack) {
        int left = stack.getCount();
        for (int i = playerStart; i < slots.size(); i++) {
            var slot = getSlot(i);
            var present = slot.getItem();
            if (present.isEmpty() || WorkshopPlatform.sameItem(present, stack))
                left -= Math.max(0, slot.getMaxStackSize(stack) - present.getCount());
            if (left <= 0) return true;
        }
        return false;
    }
}
