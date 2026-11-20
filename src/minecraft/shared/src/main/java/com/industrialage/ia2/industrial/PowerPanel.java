package com.industrialage.ia2.industrial;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

/** Version-independent visual layout using the author's reactor atlas and live menu data. */
final class PowerPanel {
    interface Canvas {
        void rect(int x, int y, int right, int bottom, int colour);

        void text(Component text, int x, int y, int colour, boolean center);

        void texture(String id, int width, int height);
    }

    private PowerPanel() {}

    static void draw(PowerMenu menu, Component title, Canvas g) {
        if (menu.core()) {
            g.texture(
                    menu.fluid() ? "guinuclearreactorfluid" : "guinuclearreactor",
                    menu.width(),
                    menu.height());
            for (int i = 0; i < 54; i++)
                if (!menu.exposed(i)) {
                    Slot s = menu.getSlot(i);
                    g.rect(s.x - 1, s.y - 1, s.x + 17, s.y + 17, 0xd82e3640);
                }
            g.text(title, menu.width() / 2, 6, 0xff253138, true);
            g.rect(7, 135, 205, 152, 0xff243039);
            g.text(
                    Component.translatable("ic2.panel.hull", menu.value(4), menu.value(6)),
                    10,
                    139,
                    0xffffc384,
                    false);
            Component right =
                    menu.fluid()
                            ? Component.translatable(
                                    "ic2.panel.cool", menu.data.get(10), menu.data.get(11))
                            : Component.literal(menu.value(8) / 10.0 + " EU/t");
            g.text(right, 155, 139, 0xffbce9ff, true);
            return;
        }
        g.rect(0, 0, menu.width(), menu.height(), 0xff353e45);
        g.rect(2, 2, menu.width() - 2, menu.height() - 2, 0xffbbc4cc);
        g.rect(5, 20, menu.width() - 5, 78, 0xffd8dfe4);
        g.text(title, menu.width() / 2, 7, 0xff18262d, true);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.getSlot(i);
            if (i < menu.machineSlots && !menu.exposed(i)) continue;
            g.rect(s.x - 1, s.y - 1, s.x + 17, s.y + 17, 0xff5e6b73);
            g.rect(s.x, s.y, s.x + 16, s.y + 16, 0xff939fa8);
            if (i < menu.machineSlots && (menu.kind != PowerKind.RT_GENERATOR || i == 0 || i == 6))
                g.text(
                        Component.translatable("ic2.slot." + menu.role(i)),
                        s.x + 8,
                        s.y - 11,
                        0xff2d404a,
                        true);
        }
        var value = menu.value(12);
        Component status =
                switch (menu.kind) {
                    case TRADE_O_MAT -> Component.translatable("ic2.panel.trades", value);
                    case ENERGY_O_MAT -> Component.translatable("ic2.panel.credit", value);
                    case GEO_GENERATOR -> Component.translatable("ic2.panel.lava", value);
                    case LV_TRANSFORMER, MV_TRANSFORMER, HV_TRANSFORMER, EV_TRANSFORMER ->
                            Component.translatable("ic2.panel.transform." + value);
                    case TELEPORTER -> Component.translatable("ic2.panel.teleporter");
                    default -> Component.empty();
                };
        g.text(status, menu.width() / 2, 57, 0xff26343e, true);
        if (!menu.kind.chargeBlock()
                && menu.kind != PowerKind.TRADE_O_MAT
                && menu.kind != PowerKind.TELEPORTER)
            g.text(
                    Component.literal(menu.value(2) + " EU"),
                    menu.width() / 2,
                    69,
                    0xff21433e,
                    true);
        g.text(Component.translatable("container.inventory"), 24, 86, 0xff273741, false);
    }
}
