package com.industrialage.ia2.logistics;

import com.industrialage.ia2.gui.AuthoredScreen;
import com.industrialage.ia2.gui.PanelCanvas;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Original 176 by 166 frame with the exchangeable fluid well at (79,35). */
final class TankScreen extends AuthoredScreen<TankMenu> {
    TankScreen(TankMenu menu, Inventory player, Component title) {
        super(menu, player, title, 176, 166);
    }

    @Override
    protected void paint(PanelCanvas g) {
        g.frame(0, 0, 176, 166);
        g.text(title, 88, 6, 0xff263340, true);
        g.blit("common", 79, 35, 103, 7, 18, 18);
        if (menu.amount() > 0) g.fluid(menu.fluid(), 80, 36, 16, 16);
        g.text(WorkshopPlatform.previewFluid(menu.fluid()), 88, 24, 0xff263340, true);
        g.text(
                Component.literal(menu.amount() + " / " + menu.capacity() + " mB"),
                88,
                63,
                0xff263340,
                true);
        for (int i = 0; i < menu.upgradeCount; i++) g.slot(152, 8 + 18 * i);
        g.inventory(8, 84, 142);
    }

    @Override
    protected int actionAt(int x, int y, int button) {
        return button == 0 && x >= 79 && x < 97 && y >= 35 && y < 53 ? 0 : -1;
    }
}
