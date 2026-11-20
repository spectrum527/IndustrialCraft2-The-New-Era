package com.industrialage.ia2.forge.qa;

import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Check every physical connection shape using face-center and outer-edge positions in the actual
 * block.
 */
final class PipeGeometryChecks {
    private static final BooleanProperty[] FACES = {
        BlockStateProperties.DOWN,
        BlockStateProperties.UP,
        BlockStateProperties.NORTH,
        BlockStateProperties.SOUTH,
        BlockStateProperties.WEST,
        BlockStateProperties.EAST
    };

    private PipeGeometryChecks() {}

    static void run(ServerLevel level, Function<String, Block> registry) {
        for (String metal : new String[] {"bronze", "steel"})
            for (String material : new String[] {"item", "fluid"}) {
                String[] sizes = {"tiny", "small", "medium", "large"};
                for (int bore = 0; bore < sizes.length; bore++) {
                    String path = metal + "_" + material + "_pipe_" + sizes[bore];
                    Block block = registry.apply(path);
                    double low = (6 - bore) / 16.0;
                    for (int connections = 0; connections < 64; connections++) {
                        var state = block.defaultBlockState();
                        for (int i = 0; i < FACES.length; i++)
                            state = state.setValue(FACES[i], (connections & (1 << i)) != 0);
                        var boxes = state.getShape(level, BlockPos.ZERO).toAabbs();
                        if (boxes.stream().noneMatch(b -> b.contains(.5, .5, .5))
                                || boxes.stream().anyMatch(b -> b.contains(.01, .01, .01)))
                            throw new AssertionError(path + " core or corner " + connections);
                        for (var face : Direction.values()) {
                            double x = .5 + face.getStepX() * .495,
                                    y = .5 + face.getStepY() * .495,
                                    z = .5 + face.getStepZ() * .495;
                            boolean expected = (connections & (1 << face.ordinal())) != 0;
                            if (expected != boxes.stream().anyMatch(b -> b.contains(x, y, z)))
                                throw new AssertionError(path + " " + face + " " + connections);
                        }
                        if (connections == 0
                                && (boxes.size() != 1
                                        || Math.abs(boxes.get(0).minX - low) > 1e-7
                                        || Math.abs(boxes.get(0).maxX - (1 - low)) > 1e-7))
                            throw new AssertionError(path + " bore diameter");
                    }
                }
            }
    }
}
