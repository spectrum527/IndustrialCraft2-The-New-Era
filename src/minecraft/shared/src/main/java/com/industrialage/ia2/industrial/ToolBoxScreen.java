package com.industrialage.ia2.industrial;

import com.industrialage.ia2.gui.AuthoredScreen;
import com.industrialage.ia2.gui.PanelCanvas;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

final class ToolBoxScreen extends AuthoredScreen<ToolBoxMenu> {
    ToolBoxScreen(ToolBoxMenu menu, Inventory player, Component title) {
        super(menu, player, title, 176, 166);
    }

    @Override
    protected void paint(PanelCanvas canvas) {
        canvas.background(
                menu.kind == WorkshopToolBox.Kind.TOOLS ? "guitoolbox" : "guicontainmentbox",
                176,
                166);
        canvas.text(Component.translatable("item.ic2." + menu.kind.path), 8, 6, 0xff303030, false);
    }
}
