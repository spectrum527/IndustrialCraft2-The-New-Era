package com.industrialage.ia2.world;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** A read-only snapshot of the actual blocks detected by a handheld scanner. */
final class ScannerMenu extends AbstractContainerMenu {
    static final int PAGE_SIZE = 24;
    private final SimpleContainer shown = new SimpleContainer(PAGE_SIZE);
    private final SimpleContainerData counts = new SimpleContainerData(PAGE_SIZE + 2);
    private final List<Map.Entry<Block, Integer>> ores;

    ScannerMenu(MenuType<ScannerMenu> type, int id, Inventory player, Map<Block, Integer> found) {
        super(type, id);
        ores =
                found == null
                        ? null
                        : found.entrySet().stream()
                                .sorted(
                                        Comparator.comparing(
                                                entry -> WorldNative.blockId(entry.getKey())))
                                .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
                                .toList();
        for (int index = 0; index < PAGE_SIZE; index++) {
            addSlot(
                    new Slot(shown, index, x(index), y(index)) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            return false;
                        }

                        @Override
                        public boolean isActive() {
                            return hasItem();
                        }
                    });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(player, 9 + row * 9 + column, 8 + 18 * column, 148 + 18 * row));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(player, column, 8 + 18 * column, 206));
        }
        addDataSlots(counts);
        if (ores != null) {
            counts.set(PAGE_SIZE + 1, Math.max(1, (ores.size() + PAGE_SIZE - 1) / PAGE_SIZE));
            showPage(0);
        }
    }

    static void open(Player player, boolean advanced, Map<Block, Integer> ores) {
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, opener) ->
                                new ScannerMenu(WorldMenus.scanner(), id, inventory, ores),
                        Component.translatable(
                                advanced ? "item.ic2.advanced_scanner" : "item.ic2.scanner")));
    }

    static int x(int index) {
        return 10 + 39 * (index % 4);
    }

    static int y(int index) {
        return 22 + 18 * (index / 4);
    }

    int amount(int index) {
        return counts.get(index);
    }

    int page() {
        return counts.get(PAGE_SIZE);
    }

    int pages() {
        return counts.get(PAGE_SIZE + 1);
    }

    private void showPage(int page) {
        counts.set(PAGE_SIZE, page);
        for (int i = 0; i < PAGE_SIZE; i++) {
            int result = page * PAGE_SIZE + i;
            var ore = result < ores.size() ? ores.get(result) : null;
            counts.set(i, ore == null ? 0 : ore.getValue());
            shown.setItem(i, ore == null ? ItemStack.EMPTY : new ItemStack(ore.getKey()));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (ores == null || button < 0 || button > 1) return false;
        int next = page() + (button == 0 ? -1 : 1);
        if (next < 0 || next >= pages()) return false;
        showPage(next);
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < PAGE_SIZE) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack prior = stack.copy();
        int hotbar = PAGE_SIZE + 27;
        boolean moved =
                index < hotbar
                        ? moveItemStackTo(stack, hotbar, slots.size(), false)
                        : moveItemStackTo(stack, PAGE_SIZE, hotbar, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return prior;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
