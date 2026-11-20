package com.industrialage.ia2.world;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A seed bag retains the parent crop's growth, gain and resistance across planting. */
final class CropSeed extends Item {
    CropSeed(Properties props) {
        super(props);
    }

    static ItemStack forCrop(CropSpecies crop, CropGenes genes) {
        var item = new ItemStack(WorldNative.item("ic2:crop_seed_bag"));
        write(item, crop, genes);
        return item;
    }

    static void write(ItemStack stack, CropSpecies crop, CropGenes genes) {
        WorldNative.setGenome(
                stack,
                String.join(
                        ",",
                        crop.name,
                        String.valueOf(genes.growth()),
                        String.valueOf(genes.gain()),
                        String.valueOf(genes.resistance()),
                        String.valueOf(genes.scan())));
    }

    static SeedData read(ItemStack stack) {
        if (!(stack.getItem() instanceof CropSeed)) return null;
        String[] fields = WorldNative.genome(stack).split(",", -1);
        if (fields.length != 5) return null;
        var crop = CropSpecies.byName(fields[0]);
        if (crop == null) return null;
        try {
            return new SeedData(
                    crop,
                    new CropGenes(
                            Integer.parseInt(fields[1]),
                            Integer.parseInt(fields[2]),
                            Integer.parseInt(fields[3]),
                            Integer.parseInt(fields[4])));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        var seed = read(stack);
        if (seed == null || seed.genes.scan() == 0) {
            return Component.translatable("item.ic2.crop_seed_bag.unidentified");
        }
        return Component.translatable(
                "item.ic2.crop_seed_bag.identified",
                Component.translatable("block.ic2." + seed.crop.path()));
    }

    record SeedData(CropSpecies crop, CropGenes genes) {}
}
