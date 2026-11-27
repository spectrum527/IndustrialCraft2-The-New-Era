package com.industrialage.ia2.progress;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Resolves the currently loaded native advancement, including after datapack reloads. */
final class ProgressPlatform {
    private ProgressPlatform() {}

    static void poweredToolHit(ServerPlayer player, ItemStack tool) {
        var server = player.getServer();
        if (server == null) return;
        var item = ForgeRegistries.ITEMS.getKey(tool.getItem());
        if (item == null || !item.getNamespace().equals("ic2")) return;
        var advancement =
                server.getAdvancements()
                        .getAdvancement(new ResourceLocation("ic2", "industry/tools/powered_hit"));
        if (advancement == null) return;
        String criterion = "hit_" + item.getPath();
        if (advancement.getCriteria().containsKey(criterion)) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
