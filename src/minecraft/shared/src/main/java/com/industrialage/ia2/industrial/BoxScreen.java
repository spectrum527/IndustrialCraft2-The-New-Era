package com.industrialage.ia2.industrial;

import com.industrialage.ia2.gui.AuthoredScreen;
import com.industrialage.ia2.gui.PanelCanvas;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/** The workbench has its own painted atlas. Storage boxes use the redrawn IC2 frame and wells. */
final class BoxScreen extends AuthoredScreen<BoxMenu> {
    static final int SORT_RIGHT_MARGIN = 20;
    static final int SORT_Y = 4;
    static final int SORT_WIDTH = 12;
    static final int SORT_HEIGHT = 11;
    static final String SORT_TOOLTIP_KEY = "gui.ic2.box.sort";

    BoxScreen(BoxMenu menu, Inventory player, Component title) {
        super(menu, player, title, menu.width, menu.height);
    }

    static int sortX(int panelWidth) {
        return panelWidth - SORT_RIGHT_MARGIN;
    }

    static boolean inSortButton(BoxKind kind, int panelWidth, int x, int y) {
        if (kind == null || !kind.portable()) return false;
        int bx = sortX(panelWidth);
        return x >= bx && x < bx + SORT_WIDTH && y >= SORT_Y && y < SORT_Y + SORT_HEIGHT;
    }

    @Override
    protected void paint(PanelCanvas canvas) {
        paint(canvas, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    @Override
    protected void paint(PanelCanvas canvas, int mouseX, int mouseY) {
        if (menu.kind == BoxKind.INDUSTRIAL_WORKBENCH) {
            canvas.background("guiindustrialworkbench", menu.width, menu.height);
            canvas.blit("common", 94, 43, 192, 62, 14, 14);
            canvas.text(title, menu.width / 2, 6, 0xff263340, true);
            return;
        }
        canvas.frame(0, 0, menu.width, menu.height);
        for (var slot : menu.slots) canvas.slot(slot.x, slot.y);
        canvas.fittedText(title, menu.width / 2, 6, menu.width - 44, 0xff263340);
        drawSortButton(
                canvas,
                sortX(menu.width),
                SORT_Y,
                inSortButton(menu.kind, menu.width, mouseX, mouseY));
    }

    private static void drawSortButton(PanelCanvas canvas, int x, int y, boolean hovered) {
        int shadow = hovered ? 0xff263340 : 0xff39424f;
        int highlight = hovered ? 0xfff0f6fa : 0xffd6dee6;
        int fill = hovered ? 0xff8795a6 : 0xff727d8b;
        int icon = hovered ? 0xffffffff : 0xffeef4f8;

        canvas.fill(x, y, x + SORT_WIDTH, y + SORT_HEIGHT, shadow);
        canvas.fill(x, y, x + SORT_WIDTH - 1, y + SORT_HEIGHT - 1, highlight);
        canvas.fill(x + 1, y + 1, x + SORT_WIDTH - 1, y + SORT_HEIGHT - 1, fill);

        // 3 descending horizontal bars on the left (4px, 3px, 2px).
        canvas.fill(x + 2, y + 2, x + 6, y + 3, icon);
        canvas.fill(x + 2, y + 5, x + 5, y + 6, icon);
        canvas.fill(x + 2, y + 8, x + 4, y + 9, icon);

        // Downward sort arrow on the right (stem + wings).
        canvas.fill(x + 8, y + 2, x + 9, y + 9, icon);
        canvas.fill(x + 7, y + 7, x + 10, y + 8, icon);
    }

    @Override
    protected Component tooltipAt(int x, int y) {
        return inSortButton(menu.kind, menu.width, x, y)
                ? Component.translatable(SORT_TOOLTIP_KEY)
                : null;
    }

    @Override
    protected int actionAt(int x, int y, int button) {
        if (button != 0) return -1;
        if (menu.kind == BoxKind.INDUSTRIAL_WORKBENCH) {
            return x >= 92 && x < 110 && y >= 41 && y < 59 ? 0 : -1;
        }
        if (inSortButton(menu.kind, menu.width, x, y)) {
            if (minecraft != null) {
                minecraft
                        .getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return BoxMenu.SORT_ACTION;
        }
        return -1;
    }
}
