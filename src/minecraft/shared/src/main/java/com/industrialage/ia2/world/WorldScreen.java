package com.industrialage.ia2.world;

import com.industrialage.ia2.gui.AuthoredScreen;
import com.industrialage.ia2.gui.PanelCanvas;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Uses the artist's original atlas and the same physical slots as its native menu. */
final class WorldScreen extends AuthoredScreen<WorldMenu> {
    WorldScreen(WorldMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.layout.width, menu.layout.height);
    }

    @Override
    protected void paint(PanelCanvas c) {
        var l = menu.layout;
        if (l.texture == null) {
            c.frame(0, 0, l.width, l.height);
            c.inventory(8, l.playerY, l.hotbarY);
            c.slot(l.x[0], l.y[0]);
            c.slot(l.x[8], l.y[8]);
            c.text(Component.translatable("ic2.world.blueprint"), 53, 23, 0xff303030, false);
        } else c.background(l.texture, l.width, l.height);
        if (menu.kind == WorldMachineBlock.Kind.CLASSIC_CROPMATRON
                || menu.kind == WorldMachineBlock.Kind.CROPMATRON) c.slot(l.x[8], l.y[8]);
        if (menu.kind == WorldMachineBlock.Kind.WEIGHTED_DISTRIBUTOR && menu.data.get(0) == 2) {
            for (int i = 18; i < 24; i++) c.slot(l.x[i], l.y[i]);
            c.slot(l.x[25], l.y[25]);
            String[] sides = {"D", "U", "N", "S", "W", "E"};
            for (int i = 0; i < 6; i++)
                c.text(Component.literal(sides[i]), 130, 14 + 16 * i, 0xff303030, true);
        }
        if (menu.kind == WorldMachineBlock.Kind.CROPMATRON) {
            int water = Math.min(40, menu.data.get(2) * 40 / 4000),
                    weed = Math.min(40, menu.data.get(3) * 40 / 4000);
            if (water > 0) c.fill(12, 65 - water, 17, 65, 0xff4289df);
            if (weed > 0) c.fill(56 + 48, 65 - weed, 109, 65, 0xff65b547);
        }
        if (menu.kind == WorldMachineBlock.Kind.CHUNK_LOADER) {
            int x = 24 + 16 * menu.data.get(5), y = 15 + 16 * menu.data.get(6);
            int color = menu.data.get(7) == 0 ? 0xffa34747 : 0xff65e06e;
            c.fill(x, y, x + 15, y + 1, color);
            c.fill(x, y + 14, x + 15, y + 15, color);
            c.fill(x, y, x + 1, y + 15, color);
            c.fill(x + 14, y, x + 15, y + 15, color);
            c.text(
                    Component.translatable(
                            menu.data.get(7) == 0 ? "ic2.world.toggle.off" : "ic2.world.toggle.on"),
                    14,
                    121,
                    color,
                    true);
        }
        if (menu.kind == WorldMachineBlock.Kind.MINER) {
            c.fill(111, 4, 169, 17, 0xff536a7a);
            c.text(
                    Component.translatable(
                            menu.data.get(4) != 0 ? "ic2.world.withdraw" : "ic2.world.mine"),
                    140,
                    6,
                    0xffffffff,
                    true);
        }
        if (menu.kind == WorldMachineBlock.Kind.ADVANCED_MINER) {
            c.text(
                    Component.translatable(
                            menu.data.get(4) != 0 ? "ic2.world.blacklist" : "ic2.world.whitelist"),
                    87,
                    29,
                    0xffc8e9ed,
                    true);
        }
        if (l.pages > 1) {
            for (int i = 0; i < l.pages; i++) {
                int x = 5 + 15 * i;
                c.fill(x, 4, x + 12, 15, menu.data.get(0) == i ? 0xff536a7a : 0xff303a43);
                c.text(Component.literal(Integer.toString(i + 1)), x + 6, 6, 0xffffffff, true);
            }
            String role =
                    menu.kind == WorldMachineBlock.Kind.WEIGHTED_DISTRIBUTOR
                                    && menu.data.get(0) == 2
                            ? "ic2.world.page.weights"
                            : "ic2.world.page.buffer";
            c.text(Component.translatable(role, menu.data.get(0) + 1), 52, 6, 0xff303030, false);
        } else if (menu.kind != WorldMachineBlock.Kind.CHUNK_LOADER) {
            c.text(Component.translatable("block.ic2." + menu.kind.path), 8, 6, 0xff303030, false);
        }
    }

    @Override
    protected int actionAt(int x, int y, int button) {
        if (button != 0) return -1;
        if (menu.layout.pages > 1 && y >= 4 && y < 15 && x >= 5 && x < 5 + 15 * menu.layout.pages) {
            int page = (x - 5) / 15;
            if ((x - 5) % 15 < 12) return page;
        }
        if (menu.kind == WorldMachineBlock.Kind.CHUNK_LOADER) {
            if (x >= 24 && x < 168 && y >= 15 && y < 159)
                return 100 + (x - 24) / 16 + 9 * ((y - 15) / 16);
            if (x >= 7 && x < 23 && y >= 116 && y < 138) return 22;
        }
        if (menu.kind == WorldMachineBlock.Kind.MINER && x >= 111 && x < 169 && y >= 4 && y < 17)
            return 21;
        if (menu.kind == WorldMachineBlock.Kind.ADVANCED_MINER
                && x >= 36
                && x < 120
                && y >= 26
                && y < 42) return 20;
        return -1;
    }
}
