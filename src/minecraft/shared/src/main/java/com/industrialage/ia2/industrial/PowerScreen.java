package com.industrialage.ia2.industrial;

import com.industrialage.ia2.gui.AuthoredScreen;
import com.industrialage.ia2.gui.PanelCanvas;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Native input and tooltips over the same authored panel used to place inventory slots. */
final class PowerScreen extends AuthoredScreen<PowerMenu> {
    PowerScreen(PowerMenu menu, Inventory player, Component title) {
        super(menu, player, title, menu.layout.width(), menu.layout.height());
    }

    @Override
    protected void paint(PanelCanvas canvas) {
        PowerPanel.draw(menu, title, canvas);
    }

    @Override
    protected int actionAt(int x, int y, int button) {
        if (button != 0) return -1;
        if (menu.kind.storage() && x >= 151 && x < 174 && y >= 3 && y < 24) return 0;
        if (menu.kind.lowTier() > 0 && x >= 148 && x < 170 && y >= 28 && y < 54) return 0;
        if (menu.kind == PowerKind.ENERGY_O_MAT
                && !menu.guest
                && x >= 102
                && x < 165
                && y >= 37
                && y < 56) return x < 132 ? 0 : 1;
        return -1;
    }
}
