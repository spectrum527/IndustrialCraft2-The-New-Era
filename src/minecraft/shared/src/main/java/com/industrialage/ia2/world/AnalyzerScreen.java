package com.industrialage.ia2.world;

import com.industrialage.ia2.gui.AuthoredScreen;
import com.industrialage.ia2.gui.PanelCanvas;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

final class AnalyzerScreen extends AuthoredScreen<AnalyzerMenu> {
    AnalyzerScreen(AnalyzerMenu menu, Inventory player, Component title) {
        super(menu, player, title, 176, 223);
    }

    @Override
    protected void paint(PanelCanvas c) {
        c.background("guicropnalyzer", 176, 223);
        c.text(Component.translatable("item.ic2.cropnalyzer"), 88, 9, 0xff303030, true);
        var seed = CropSeed.read(menu.getSlot(1).getItem());
        if (seed == null) seed = CropSeed.read(menu.getSlot(0).getItem());
        if (seed != null) {
            int stage = seed.genes().scan();
            c.text(
                    Component.translatable("ic2.world.analyzer.stage", stage),
                    14,
                    39,
                    0xffb6e8e7,
                    false);
            if (stage > 0)
                c.text(
                        Component.translatable("block.ic2." + seed.crop().path()),
                        14,
                        54,
                        0xffb6e8e7,
                        false);
            if (stage >= 4) {
                c.text(
                        Component.translatable("ic2.world.analyzer.growth", seed.genes().growth()),
                        14,
                        70,
                        0xffb6e8e7,
                        false);
                c.text(
                        Component.translatable("ic2.world.analyzer.gain", seed.genes().gain()),
                        14,
                        83,
                        0xffb6e8e7,
                        false);
                c.text(
                        Component.translatable(
                                "ic2.world.analyzer.resistance", seed.genes().resistance()),
                        14,
                        96,
                        0xffb6e8e7,
                        false);
            }
        }
        c.text(
                Component.literal(((menu.data.get(0) & 0xffff) | (menu.data.get(1) << 16)) + " EU"),
                14,
                116,
                0xffb6e8e7,
                false);
    }

    @Override
    protected int actionAt(int x, int y, int button) {
        return button == 0 && x >= 25 && x < 41 && y >= 7 && y < 23 ? 0 : -1;
    }
}
