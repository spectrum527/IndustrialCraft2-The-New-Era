package com.industrialage.ia2.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** Crop cards with distinct planting materials, stages, and harvests. */
enum CropSpecies {
    ACACIA_SAPLING(4, 3, "acacia_sapling", "acacia_log"),
    AURELIA(3, 6, null, "gold_nugget", "gold_block", "c:storage_blocks/gold"),
    BEETROOTS(3, 1, "beetroot_seeds", "beetroot"),
    BIRCH_SAPLING(4, 3, "birch_sapling", "birch_log"),
    BLACKTHORN(3, 4, null, "ink_sac"),
    BLAZEREED(3, 6, null, "blaze_powder"),
    BOBS_YER_UNCLE_RANKS_BERRIES(3, 4, null, "ic2:bobs_yer_uncle_ranks_berry"),
    BROWN_MUSHROOM(2, 2, "brown_mushroom", "brown_mushroom"),
    CARROTS(3, 1, "carrot", "carrot"),
    COCOA(3, 2, "cocoa_beans", "cocoa_beans"),
    COFFEE(4, 4, null, "ic2:coffee_beans"),
    CORIUM(3, 5, null, "leather"),
    CORPSE_PLANT(3, 4, null, "rotten_flesh"),
    CREEPER_WEED(3, 5, null, "gunpowder"),
    CYAZINT(3, 3, null, "cyan_dye"),
    CYPRIUM(3, 5, null, "ic2:small_copper_dust", "copper_block", "c:storage_blocks/copper"),
    DANDELION(3, 2, "dandelion", "dandelion"),
    DARK_OAK_SAPLING(4, 3, "dark_oak_sapling", "dark_oak_log"),
    DIAREED(3, 8, null, "diamond", "diamond_block", "c:storage_blocks/diamond"),
    EATING_PLANT(5, 5, null, "ic2:grin_powder"),
    EGG_PLANT(2, 4, null, "egg"),
    ENDER_BLOSSOM(3, 7, null, "ender_pearl"),
    FERRU(3, 6, null, "iron_nugget", "iron_block", "c:storage_blocks/iron"),
    FLAX(3, 2, null, "string"),
    HOPS(6, 5, null, "ic2:hops"),
    JUNGLE_SAPLING(4, 3, "jungle_sapling", "jungle_log"),
    MEAT_ROSE(3, 5, null, "porkchop"),
    MELON(3, 2, "melon_seeds", "melon_slice"),
    MILK_WART(2, 4, null, "ic2:milk_wart"),
    NETHER_WART(2, 3, "nether_wart", "nether_wart"),
    OAK_SAPLING(4, 3, "oak_sapling", "oak_log"),
    OIL_BERRIES(2, 5, null, "ic2:oil_berry"),
    PLUMBISCUS(3, 5, null, "ic2:small_lead_dust", "ic2:lead_block", "c:storage_blocks/lead"),
    POPPY(3, 2, "poppy", "poppy"),
    POTATO(3, 1, "potato", "potato"),
    PUMPKIN(3, 2, "pumpkin_seeds", "pumpkin"),
    RED_MUSHROOM(2, 2, "red_mushroom", "red_mushroom"),
    RED_WHEAT(6, 6, null, "redstone"),
    REED(2, 2, "sugar_cane", "sugar_cane"),
    SHINING(3, 6, null, "glowstone_dust"),
    SLIME_PLANT(3, 5, null, "slime_ball"),
    SPIDERNIP(3, 5, null, "spider_eye"),
    SPRUCE_SAPLING(4, 3, "spruce_sapling", "spruce_log"),
    STAGNIUM(3, 5, null, "ic2:small_tin_dust", "ic2:tin_block", "c:storage_blocks/tin"),
    STICKY_REED(3, 4, null, "ic2:resin"),
    TEARSTALKS(3, 7, null, "ghast_tear"),
    TERRA_WART(2, 4, "ic2:terra_wart", "ic2:terra_wart"),
    TULIP(3, 2, "orange_tulip", "orange_tulip"),
    VENOMILIA(5, 3, null, "ic2:grin_powder"),
    WEED(4, 1, null, "ic2:weed"),
    WHEAT(7, 1, "wheat_seeds", "wheat"),
    WITHEREED(3, 7, null, "coal");

    final String name;
    final int maxAge;
    final int tier;
    final String seed;
    final String output;
    final String support;
    final String supportTag;

    CropSpecies(int maxAge, int tier, String seed, String output) {
        this(maxAge, tier, seed, output, null, null);
    }

    CropSpecies(
            int maxAge, int tier, String seed, String output, String support, String supportTag) {
        name = name().toLowerCase(java.util.Locale.ROOT);
        this.maxAge = maxAge;
        this.tier = tier;
        this.seed = seed;
        this.output = output;
        this.support = support;
        this.supportTag = supportTag;
    }

    String path() {
        return name + "_crop";
    }

    static CropSpecies byName(String name) {
        for (var species : values()) {
            if (species.name.equals(name)) return species;
        }
        return null;
    }

    static CropSpecies bySeed(ItemStack stack) {
        if (stack.isEmpty()) return null;
        String id = WorldNative.itemId(stack);
        for (var species : values()) {
            if (species.seed != null && id.equals(qualify(species.seed))) return species;
        }
        if (id.equals("minecraft:red_tulip")
                || id.equals("minecraft:white_tulip")
                || id.equals("minecraft:pink_tulip")) return TULIP;
        return null;
    }

    ItemStack produce(int count) {
        return new ItemStack(WorldNative.item(qualify(output)), count);
    }

    static CropSpecies cross(CropSpecies first, CropSpecies second, RandomSource random) {
        if (random.nextInt(5) < 2) return random.nextBoolean() ? first : second;
        if (first == REED && second == WHEAT || first == WHEAT && second == REED) {
            if (random.nextBoolean()) return STICKY_REED;
        }
        int targetTier = (first.tier + second.tier + 1) / 2 + 1;
        List<CropSpecies> candidates = new ArrayList<>();
        for (var candidate : values()) {
            if (candidate != WEED && Math.abs(candidate.tier - targetTier) <= 1)
                candidates.add(candidate);
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    static String qualify(String path) {
        return path.indexOf(':') == -1 ? "minecraft:" + path : path;
    }
}
