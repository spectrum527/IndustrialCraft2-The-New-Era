package com.industrialage.ia2.gametest;

import com.industrialage.ia2.industrial.WorkshopMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;

/** Uses this Minecraft version's native menu-click API. */
public final class NativeWaterActions {
    private NativeWaterActions() {}

    public static void click(WorkshopMenu menu, int slot, String action, ServerPlayer player) {
        menu.clicked(slot, 0, ClickType.valueOf(action), player);
    }
}
