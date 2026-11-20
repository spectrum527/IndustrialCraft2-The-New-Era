package com.industrialage.ia2.industrial;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Native chest protocol with independent per-viewer paging for 63- and 126-slot inventories. */
final class BoxMenu extends BoxMenuBase {
    private static final List<MenuType<ChestMenu>> TYPES =
            List.of(
                    MenuType.GENERIC_9x1,
                    MenuType.GENERIC_9x2,
                    MenuType.GENERIC_9x3,
                    MenuType.GENERIC_9x4,
                    MenuType.GENERIC_9x5,
                    MenuType.GENERIC_9x6);
    private final View view;

    static BoxMenu open(int id, Inventory player, BoxNode box, int page) {
        return new BoxMenu(id, player, new View(box, page));
    }

    private BoxMenu(int id, Inventory player, View view) {
        super(TYPES.get(view.size / 9 - 1), id, player, view, view.size / 9);
        this.view = view;
        for (int i = view.usable; i < view.size; i++) {
            var previous = slots.get(i);
            var control =
                    new Slot(view, i, previous.x, previous.y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }

                        @Override
                        public boolean mayPickup(Player opener) {
                            return false;
                        }
                    };
            control.index = i;
            slots.set(i, control);
        }
    }

    @Override
    boolean intercept(int slot, boolean pickup, Player player) {
        if (slot >= view.usable && slot < view.size) {
            if (pickup && getCarried().isEmpty() && view.box.inReach(player)) {
                if (slot == 45 && view.page > 0) view.box.open(player, view.page - 1);
                else if (slot == 53 && view.start + view.usable < view.box.kind.slots)
                    view.box.open(player, view.page + 1);
            }
            return true;
        }
        return false;
    }

    private static final class View implements Container {
        final BoxNode box;
        final int page, start, size, usable;
        final ItemStack previous, next;

        View(BoxNode box, int page) {
            this.box = box;
            this.page = page;
            start = page * 45;
            size = Math.min(54, box.kind.slots);
            usable = box.kind.slots > 54 ? Math.min(45, box.kind.slots - start) : size;
            previous =
                    page > 0
                            ? BoxPlatform.named(
                                    new ItemStack(Items.ARROW),
                                    Component.translatable("ic2.box.previous"))
                            : ItemStack.EMPTY;
            next =
                    start + usable < box.kind.slots
                            ? BoxPlatform.named(
                                    new ItemStack(Items.ARROW),
                                    Component.translatable("ic2.box.next"))
                            : ItemStack.EMPTY;
        }

        @Override
        public int getContainerSize() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < usable; i++) if (!getItem(i).isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot < usable
                    ? box.items.getItem(start + slot)
                    : slot == 45 ? previous : slot == 53 ? next : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            return slot < usable ? box.items.removeItem(start + slot, count) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return slot < usable ? box.items.removeItemNoUpdate(start + slot) : ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot < usable) box.items.setItem(start + slot, stack);
        }

        @Override
        public void setChanged() {
            box.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return box.inReach(player);
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < usable; i++) box.items.setItem(start + i, ItemStack.EMPTY);
        }
    }
}
