package com.industrialage.ia2.world;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;

/** Version binding for holographic sample clicks. */
abstract class UpgradeClickBase extends AbstractContainerMenu {
    UpgradeClickBase(MenuType<?> type, int id) {
        super(type, id);
    }

    protected abstract void selectGhost(int index, Player player);

    @Override
    public void clicked(int index, int button, ClickType click, Player player) {
        if (index >= 0 && index < 9) {
            if (click == ClickType.PICKUP) selectGhost(index, player);
            return;
        }
        super.clicked(index, button, click, player);
    }
}
