package com.industrialage.ia2.progress;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Resolves the currently loaded native advancement, including after datapack reloads. */
final class ProgressPlatform {
    private ProgressPlatform() {}

    static void poweredToolHit(ServerPlayer player, ItemStack tool) {
        var server = player.level().getServer();
        if (server == null) return;
        var item = BuiltInRegistries.ITEM.getKey(tool.getItem());
        if (item == null || !item.getNamespace().equals("ic2")) return;
        var advancement =
                server.getAdvancements()
                        .get(Identifier.fromNamespaceAndPath("ic2", "industry/tools/powered_hit"));
        if (advancement == null) return;
        String criterion = "hit_" + item.getPath();
        if (advancement.value().criteria().containsKey(criterion)) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
