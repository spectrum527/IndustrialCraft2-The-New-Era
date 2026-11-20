package com.industrialage.ia2.industrial;

import com.industrialage.ia2.content.RadiationRules;
import com.industrialage.ia2.equipment.ArmorEffects;
import net.minecraft.world.entity.player.Player;

/** Only loose stacks in the player's real inventory contribute exposure. */
final class RadiationInventory {
    private RadiationInventory() {}

    static void tick(Player player) {
        if (player.isCreative() || player.isSpectator() || ArmorEffects.fullHazmat(player)) return;
        RadiationRules.Dose strongest = null;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            var dose = RadiationRules.exposure(WorkshopPlatform.itemId(stack));
            if (dose != null
                    && (strongest == null
                            || dose.amplifier() > strongest.amplifier()
                            || dose.amplifier() == strongest.amplifier()
                                    && dose.duration() > strongest.duration())) strongest = dose;
        }
        if (strongest != null)
            Radiation.expose(player, strongest.duration(), strongest.amplifier());
    }
}
