package com.industrialage.ia2.world;

import com.industrialage.ia2.gui.AuthoredScreen;
import com.industrialage.ia2.gui.PanelCanvas;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

final class UpgradeScreen extends AuthoredScreen<UpgradeMenu> {
    private final Component name;

    UpgradeScreen(UpgradeMenu menu, Inventory player, Component title) {
        super(menu, player, title, 176, 166);
        name = title;
    }

    private static void button(
            PanelCanvas c, int x, int y, int width, int height, Component caption) {
        c.fill(x, y, x + width, y + height, 0xff3b454e);
        c.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xff9aaab6);
        c.text(caption, x + width / 2, y + (height - 8) / 2, 0xff202020, true);
    }

    @Override
    protected void paint(PanelCanvas c) {
        c.frame(0, 0, 176, 166);
        c.inventory(8, 84, 142);
        c.text(name, 88, 7, 0xff303030, true);
        button(c, 10, 18, 50, 20, Component.translatable("ic2.upgrades.meta"));
        button(c, 10, 40, 50, 20, Component.translatable("ic2.upgrades.energy"));
        button(
                c,
                10,
                62,
                50,
                16,
                Component.translatable(
                        menu.data.get(2) == 0 ? "ic2.upgrades.allow" : "ic2.upgrades.deny"));
        for (int i = 0; i < 3; i++)
            c.blit(
                    "common",
                    65,
                    21 + 22 * i,
                    menu.data.get(Math.min(i, 1)) != 0 && i < 2 ? 206 : 192,
                    62,
                    14,
                    14);
        String[] side = {"*", "D", "U", "N", "S", "W", "E"};
        int direction = Math.max(0, Math.min(6, menu.data.get(3)));
        button(c, 82, 64, 12, 14, Component.literal(side[direction]));
        for (int i = 0; i < 9; i++) c.slot(98 + 18 * (i % 3), 21 + 18 * (i / 3));
    }

    @Override
    protected int actionAt(int x, int y, int button) {
        if (button != 0) return -1;
        if (x >= 10 && x < 60 && y >= 18 && y < 38) return 0;
        if (x >= 10 && x < 60 && y >= 40 && y < 60) return 1;
        if (x >= 10 && x < 60 && y >= 62 && y < 78) return 2;
        if (x >= 82 && x < 94 && y >= 64 && y < 78) return 3;
        return -1;
    }
}
