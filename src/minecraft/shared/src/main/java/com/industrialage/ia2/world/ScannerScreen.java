package com.industrialage.ia2.world;

import com.industrialage.ia2.gui.AuthoredScreen;
import com.industrialage.ia2.gui.PanelCanvas;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The player's measured ore counts are drawn over the original handheld scanner atlas. */
final class ScannerScreen extends AuthoredScreen<ScannerMenu> {
    private static final int READOUT = 0xff91ff86;

    ScannerScreen(ScannerMenu menu, Inventory player, Component title) {
        super(menu, player, title, 176, 230);
    }

    @Override
    protected void paint(PanelCanvas canvas) {
        canvas.background("guitoolscanner", 176, 230);
        canvas.text(title, 88, 6, 0xff26323a, true);
        for (int i = 0; i < ScannerMenu.PAGE_SIZE; i++) {
            int count = menu.amount(i);
            if (count != 0) {
                canvas.text(
                        Component.literal(Integer.toString(count)),
                        ScannerMenu.x(i) + 17,
                        ScannerMenu.y(i) + 7,
                        READOUT,
                        false);
            }
        }
        if (menu.pages() > 1) {
            canvas.text(Component.literal("<"), 16, 134, READOUT, true);
            canvas.text(
                    Component.literal((menu.page() + 1) + " / " + menu.pages()),
                    88,
                    134,
                    READOUT,
                    true);
            canvas.text(Component.literal(">"), 160, 134, READOUT, true);
        }
    }

    @Override
    protected int actionAt(int x, int y, int button) {
        if (button != 0 || menu.pages() < 2 || y < 130 || y >= 144) return -1;
        if (x >= 8 && x < 26) return 0;
        if (x >= 150 && x < 168) return 1;
        return -1;
    }
}
