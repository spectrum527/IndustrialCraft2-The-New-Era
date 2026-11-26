package com.industrialage.ia2.world;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Material;
import org.junit.jupiter.api.Test;

/**
 * Host-adapter contracts for mining_pipe and mining_pipe_tip material and waterlogging wiring on
 * Forge 1.19.2.
 */
class MiningPipeWaterlogContractTest {
    @Test
    void miningPipesUseMetalMaterialAndImplementSimpleWaterloggedBlock() throws Exception {
        for (String name : new String[] {"mining_pipe", "mining_pipe_tip"}) {
            assertEquals(Material.METAL, WorldNative.materialFor(name), name);
        }

        assertTrue(SimpleWaterloggedBlock.class.isAssignableFrom(WorldNative.MiningPipeBase.class));
        assertTrue(SimpleWaterloggedBlock.class.isAssignableFrom(WorldBlocks.MiningPipe.class));
        assertNotNull(
                WorldBlocks.MiningPipe.class.getDeclaredMethod(
                        "createBlockStateDefinition", StateDefinition.Builder.class));
        assertNotNull(
                WorldBlocks.MiningPipe.class.getDeclaredMethod("getFluidState", BlockState.class));
        assertNotNull(
                WorldBlocks.MiningPipe.class.getDeclaredMethod(
                        "getStateForPlacement", BlockPlaceContext.class));
        assertNotNull(
                WorldNative.MiningPipeBase.class.getDeclaredMethod(
                        "updateShape",
                        BlockState.class,
                        Direction.class,
                        BlockState.class,
                        LevelAccessor.class,
                        BlockPos.class,
                        BlockPos.class));
    }
}
