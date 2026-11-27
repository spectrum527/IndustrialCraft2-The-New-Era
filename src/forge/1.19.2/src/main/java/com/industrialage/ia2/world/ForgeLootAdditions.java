package com.industrialage.ia2.world;

import com.industrialage.ia2.content.IndustrialMaterials;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraftforge.event.LootTableLoadEvent;

/** Adds a named pool when one of the measured vanilla chest tables is constructed on Forge 43. */
final class ForgeLootAdditions {
    private static final String POOL_NAME = "ic2_exploration_bonus";
    private static final Map<ResourceLocation, ResourceLocation> CHESTS =
            IndustrialMaterials.rows("chest-supplements.tsv").stream()
                    .map(row -> row.substring(0, row.indexOf('\t')))
                    .collect(
                            Collectors.toUnmodifiableMap(
                                    name -> new ResourceLocation("minecraft", "chests/" + name),
                                    name -> new ResourceLocation("ic2", "chests/iridium_" + name)));

    private ForgeLootAdditions() {}

    static void appendToChest(LootTableLoadEvent event) {
        var addition = CHESTS.get(event.getName());
        if (addition == null || event.getTable().getPool(POOL_NAME) != null) return;
        var entry = LootTableReference.lootTableReference(addition);
        var pool = LootPool.lootPool().name(POOL_NAME).add(entry).build();
        event.getTable().addPool(pool);
    }
}
