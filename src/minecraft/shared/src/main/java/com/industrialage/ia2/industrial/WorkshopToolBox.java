package com.industrialage.ia2.industrial;

import com.industrialage.ia2.content.RadiationRules;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class WorkshopToolBox {
    enum Kind {
        TOOLS("tool_box", 9),
        CONTAINMENT("containment_box", 12);
        final String path;
        final int capacity;
        final int rows;

        Kind(String path, int capacity) {
            this.path = path;
            this.capacity = capacity;
            rows = (capacity + 8) / 9;
        }

        boolean accepts(ItemStack stack) {
            String id = WorkshopPlatform.itemId(stack);
            if (stack.isEmpty() || id.equals("ic2:tool_box") || id.equals("ic2:containment_box"))
                return false;
            return this == CONTAINMENT
                    ? RadiationRules.isRadioactive(id)
                    : stack.getMaxStackSize() == 1;
        }
    }

    private WorkshopToolBox() {}

    static void open(Player owner, InteractionHand hand, Kind kind) {
        owner.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, player) -> createMenu(id, inventory, owner, hand, kind),
                        Component.translatable("item.ic2." + kind.path)));
    }

    static ToolBoxMenu createMenu(
            int id, Inventory player, Player owner, InteractionHand hand, Kind kind) {
        var stack = owner.getItemInHand(hand);
        var saved = WorkshopPlatform.loadToolBox(stack, kind.rows * 9);
        Arrays.fill(saved, kind.capacity, saved.length, ItemStack.EMPTY);
        var contents =
                new SimpleContainer(saved) {
                    @Override
                    public boolean stillValid(Player actor) {
                        return actor == owner && actor.getItemInHand(hand) == stack;
                    }

                    @Override
                    public void setChanged() {
                        super.setChanged();
                        var current = new ArrayList<ItemStack>(kind.capacity);
                        for (int slot = 0; slot < kind.capacity; slot++) current.add(getItem(slot));
                        WorkshopPlatform.saveToolBox(stack, current);
                    }
                };
        return new ToolBoxMenu(ToolMenus.type(kind), id, player, kind, contents, stack);
    }
}
