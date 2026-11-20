package com.industrialage.ia2.industrial;

import com.industrialage.ia2.gui.AuthoredScreen;
import com.industrialage.ia2.gui.PanelCanvas;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The workbench has its own painted atlas. Storage boxes use the redrawn IC2 frame and wells. */
final class BoxScreen extends AuthoredScreen<BoxMenu> {
    BoxScreen(BoxMenu menu, Inventory player, Component title) {
        super(menu, player, title, menu.width, menu.height);
    }

    @Override
    protected void paint(PanelCanvas canvas) {
        if (menu.kind == BoxKind.INDUSTRIAL_WORKBENCH) {
            canvas.background("guiindustrialworkbench", menu.width, menu.height);
            canvas.blit("common", 94, 43, 192, 62, 14, 14);
        } else {
            canvas.frame(0, 0, menu.width, menu.height);
            for (var slot : menu.slots) canvas.slot(slot.x, slot.y);
        }
        canvas.text(title, menu.width / 2, 6, 0xff263340, true);
    }

    @Override
    protected int actionAt(int x, int y, int button) {
        return menu.kind == BoxKind.INDUSTRIAL_WORKBENCH
                        && button == 0
                        && x >= 92
                        && x < 110
                        && y >= 41
                        && y < 59
                ? 0
                : -1;
    }
}
