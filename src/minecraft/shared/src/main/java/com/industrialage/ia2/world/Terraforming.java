package com.industrialage.ia2.world;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

final class Terraforming {
    private static final int RADIUS = 12;
    private static final int WIDTH = RADIUS * 2 + 1;
    private static final int AREA = WIDTH * WIDTH;
    private static final Map<String, Plan> PLANS =
            Map.of(
                    "ic2:flatification_tfbp", Plan.FLAT,
                    "ic2:chilling_tfbp", Plan.COLD,
                    "ic2:cultivation_tfbp", Plan.GREEN,
                    "ic2:desertification_tfbp", Plan.SAND,
                    "ic2:irrigation_tfbp", Plan.WATER,
                    "ic2:mushroom_tfbp", Plan.FUNGI);

    private Terraforming() {}

    static boolean blueprint(String id) {
        return PLANS.containsKey(id);
    }

    static boolean tick(WorldMachine machine, ServerLevel level, long time) {
        var plan = PLANS.get(WorldNative.itemId(machine.inventory.getItem(0)));
        if (plan == null || !machine.canPay(80) || time % 4 != 0) return false;
        var origin = machine.getBlockPos();
        for (int attempt = 0; attempt < 16; attempt++) {
            int index = Math.floorMod(machine.cursor++, AREA);
            int x = origin.getX() + index % WIDTH - RADIUS;
            int z = origin.getZ() + index / WIDTH - RADIUS;
            if (!level.hasChunkAt(new BlockPos(x, origin.getY(), z))
                    || x == origin.getX() && z == origin.getZ()) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            var ground = new BlockPos(x, y, z);
            if (Math.abs(y - origin.getY()) > 8 || level.getBlockEntity(ground) != null) continue;
            if (modify(level, ground, origin.getY() - 1, plan)) {
                machine.cursor %= AREA;
                return machine.pay(80);
            }
        }
        machine.cursor %= AREA;
        machine.setChanged();
        return false;
    }

    private static boolean modify(ServerLevel level, BlockPos ground, int targetY, Plan plan) {
        var current = level.getBlockState(ground);
        boolean soil = WorldTerrain.soil(current);
        boolean sediment = current.is(Blocks.SAND) || current.is(Blocks.GRAVEL);
        boolean lowStone = current.is(Blocks.STONE) || current.is(Blocks.COBBLESTONE);
        return switch (plan) {
            case FLAT -> {
                if (!soil && !sediment && !lowStone) yield false;
                if (ground.getY() < targetY && level.isEmptyBlock(ground.above()))
                    yield level.setBlockAndUpdate(ground.above(), Blocks.DIRT.defaultBlockState());
                if (ground.getY() > targetY)
                    yield level.setBlockAndUpdate(ground, Blocks.AIR.defaultBlockState());
                yield replace(level, ground, Blocks.DIRT);
            }
            case COLD -> {
                if (current.is(Blocks.WATER)) yield replace(level, ground, Blocks.ICE);
                var above = ground.above();
                if ((soil || sediment)
                        && level.isEmptyBlock(above)
                        && Blocks.SNOW.defaultBlockState().canSurvive(level, above))
                    yield replace(level, above, Blocks.SNOW);
                yield false;
            }
            case GREEN -> {
                if (sediment || lowStone || current.is(Blocks.COARSE_DIRT))
                    yield replace(level, ground, Blocks.DIRT);
                if (current.is(Blocks.DIRT) && level.canSeeSky(ground.above()))
                    yield replace(level, ground, Blocks.GRASS_BLOCK);
                yield false;
            }
            case SAND ->
                    (soil || lowStone || current.is(Blocks.GRAVEL) || current.is(Blocks.SNOW_BLOCK))
                            && replace(level, ground, Blocks.SAND);
            case WATER -> {
                if (current.is(Blocks.FARMLAND)
                        && current.getValue(BlockStateProperties.MOISTURE) < 7)
                    yield level.setBlockAndUpdate(
                            ground, current.setValue(BlockStateProperties.MOISTURE, 7));
                yield (sediment || lowStone || current.is(Blocks.COARSE_DIRT))
                        && replace(level, ground, Blocks.DIRT);
            }
            case FUNGI -> (soil || sediment || lowStone) && replace(level, ground, Blocks.MYCELIUM);
        };
    }

    private static boolean replace(ServerLevel level, BlockPos pos, Block result) {
        return !level.getBlockState(pos).is(result)
                && level.setBlockAndUpdate(pos, result.defaultBlockState());
    }

    private enum Plan {
        FLAT,
        COLD,
        GREEN,
        SAND,
        WATER,
        FUNGI
    }
}
