package com.industrialage.ia2.world;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Persistent soil conditions and genetics for a plotted crop. */
final class CropPlot extends WorldSaveBase {
    CropGenes genes = CropGenes.WILD;
    int growthPoints;
    int hydration;
    int nutrients;
    int protection;

    CropPlot(BlockPos pos, BlockState state) {
        super(WorldPlatform.plotType(), pos, state);
    }

    CropSpecies species() {
        return ((CropBlock) getBlockState().getBlock()).species;
    }

    boolean ripe() {
        var crop = species();
        return crop != null && getBlockState().getValue(CropBlock.AGE) >= crop.maxAge;
    }

    void grow(ServerLevel level, RandomSource random) {
        var crop = species();
        if (crop == null) {
            breed(level, random);
            return;
        }
        var soil = level.getBlockState(worldPosition.below());
        int moisture =
                soil.hasProperty(BlockStateProperties.MOISTURE)
                        ? soil.getValue(BlockStateProperties.MOISTURE)
                        : 0;
        boolean fed = nutrients > 0;
        boolean hydrated = hydration > 0;
        nutrients = Math.max(0, nutrients - 1);
        hydration = Math.max(0, hydration - 1);
        protection = Math.max(0, protection - 1);
        if (!ripe() && goodLight(level, crop) && matchesSubstrate(level, crop)) {
            int suitability = 2 + moisture / 2 + (fed ? 4 : 0) + (hydrated ? 4 : 0);
            growthPoints += suitability + genes.growth() + random.nextInt(5);
            int requirement = 12 + crop.tier * 4 + getBlockState().getValue(CropBlock.AGE) * 2;
            if (growthPoints >= requirement) {
                growthPoints -= requirement;
                level.setBlock(
                        worldPosition, getBlockState().cycle(CropBlock.AGE), Block.UPDATE_CLIENTS);
            }
        } else if (crop == CropSpecies.WEED && protection == 0 && random.nextInt(5) == 0) {
            if (level.getBlockEntity(
                                    worldPosition.relative(
                                            Direction.Plane.HORIZONTAL.getRandomDirection(random)))
                            instanceof CropPlot next
                    && next.protection == 0
                    && next.species() != CropSpecies.WEED
                    && (next.species() == null || random.nextInt(32) >= next.genes.resistance())) {
                next.plant(level, CropSpecies.WEED, CropGenes.WILD);
            }
        }
        setChanged();
    }

    private boolean goodLight(ServerLevel level, CropSpecies crop) {
        int light =
                Math.max(
                        level.getBrightness(LightLayer.BLOCK, worldPosition),
                        level.getBrightness(LightLayer.SKY, worldPosition) - level.getSkyDarken());
        return crop == CropSpecies.BROWN_MUSHROOM || crop == CropSpecies.RED_MUSHROOM
                ? light < 13
                : light >= 9;
    }

    private boolean matchesSubstrate(ServerLevel level, CropSpecies crop) {
        if (crop.support == null || getBlockState().getValue(CropBlock.AGE) < crop.maxAge - 1)
            return true;
        var below = level.getBlockState(worldPosition.below(2));
        return below.is(WorldNative.block(CropSpecies.qualify(crop.support)))
                || crop.supportTag != null && WorldNative.blockTagged(below, crop.supportTag);
    }

    private void breed(ServerLevel level, RandomSource random) {
        if (protection > 0) {
            protection--;
            setChanged();
        }
        if (getBlockState().getValue(CropBlock.CROSSED)) {
            var adjacent = new ArrayList<CropPlot>();
            for (var direction : Direction.Plane.HORIZONTAL) {
                if (level.getBlockEntity(worldPosition.relative(direction)) instanceof CropPlot plot
                        && plot.ripe()
                        && plot.species() != CropSpecies.WEED) adjacent.add(plot);
            }
            if (adjacent.size() >= 2 && random.nextInt(3) == 0) {
                var first = adjacent.remove(random.nextInt(adjacent.size()));
                var second = adjacent.get(random.nextInt(adjacent.size()));
                var hybrid = CropSpecies.cross(first.species(), second.species(), random);
                plant(level, hybrid, first.genes.cross(second.genes, random));
                return;
            }
        }
        if (protection == 0 && random.nextInt(120) == 0)
            plant(level, CropSpecies.WEED, CropGenes.WILD);
    }

    void plant(ServerLevel level, CropSpecies crop, CropGenes genome) {
        var cropBlock = WorldNative.block("ic2:" + crop.path());
        boolean hasTwoSticks = getBlockState().getValue(CropBlock.CROSSED);
        if (level.setBlockAndUpdate(
                        worldPosition,
                        cropBlock.defaultBlockState().setValue(CropBlock.CROSSED, hasTwoSticks))
                && level.getBlockEntity(worldPosition) instanceof CropPlot placed) {
            placed.genes = genome;
            placed.hydration = hydration;
            placed.nutrients = nutrients;
            placed.protection = protection;
            placed.setChanged();
        }
    }

    boolean clearWeed(ServerLevel level) {
        if (species() != CropSpecies.WEED) return false;
        int savedHydration = hydration;
        int savedNutrients = nutrients;
        int savedProtection = protection;
        if (!level.setBlockAndUpdate(
                worldPosition,
                WorldNative.block("ic2:crop_stick")
                        .defaultBlockState()
                        .setValue(CropBlock.CROSSED, getBlockState().getValue(CropBlock.CROSSED))))
            return false;
        if (level.getBlockEntity(worldPosition) instanceof CropPlot cleared) {
            cleared.hydration = savedHydration;
            cleared.nutrients = savedNutrients;
            cleared.protection = savedProtection;
            cleared.setChanged();
        }
        return true;
    }

    ItemStack harvestOutput() {
        return ripe() ? species().produce(Math.min(16, 1 + genes.gain() / 2)) : ItemStack.EMPTY;
    }

    void harvest(ServerLevel level) {
        var crop = species();
        int reset = crop == CropSpecies.WHEAT ? 2 : Math.max(0, crop.maxAge / 2 - 1);
        growthPoints = 0;
        level.setBlock(
                worldPosition,
                getBlockState().setValue(CropBlock.AGE, reset),
                Block.UPDATE_CLIENTS);
        setChanged();
    }

    static List<ItemStack> broken(BlockState state, CropPlot plot) {
        if (!(state.getBlock() instanceof CropBlock crop)) return List.of();
        int sticks = state.getValue(CropBlock.CROSSED) ? 2 : 1;
        var drops = new ArrayList<ItemStack>();
        drops.add(new ItemStack(WorldNative.item("ic2:crop_stick"), sticks));
        if (plot != null
                && crop.species != null
                && crop.species != CropSpecies.WEED
                && state.getValue(CropBlock.AGE) >= Math.max(1, crop.species.maxAge / 2)) {
            drops.add(CropSeed.forCrop(crop.species, plot.genes));
        }
        if (plot != null && plot.ripe()) drops.add(plot.harvestOutput());
        return drops;
    }

    @Override
    void writeFields(BiConsumer<String, Double> state) {
        number(state, "statGrowth", genes.growth());
        number(state, "statGain", genes.gain());
        number(state, "statResistance", genes.resistance());
        number(state, "scanLevel", genes.scan());
        number(state, "growthPoints", growthPoints);
        number(state, "storageWater", hydration);
        number(state, "storageNutrients", nutrients);
        number(state, "storageWeedEX", protection);
    }

    @Override
    void readFields(ToDoubleFunction<String> state) {
        genes =
                new CropGenes(
                        integer(state, "statGrowth"),
                        integer(state, "statGain"),
                        integer(state, "statResistance"),
                        integer(state, "scanLevel"));
        growthPoints = integer(state, "growthPoints");
        hydration = integer(state, "storageWater");
        nutrients = integer(state, "storageNutrients");
        protection = integer(state, "storageWeedEX");
    }
}
