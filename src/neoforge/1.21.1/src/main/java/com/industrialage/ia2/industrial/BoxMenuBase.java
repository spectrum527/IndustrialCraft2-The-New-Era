package com.industrialage.ia2.industrial;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;

abstract class BoxMenuBase extends ChestMenu {
    BoxMenuBase(MenuType<?> type, int id, Inventory player, Container container, int rows) {
        super(type, id, player, container, rows);
    }

    abstract boolean intercept(int slot, boolean pickup, Player player);

    @Override
    public void clicked(int slot, int button, ClickType click, Player player) {
        if (!intercept(slot, click == ClickType.PICKUP, player))
            super.clicked(slot, button, click, player);
    }
}
