package com.industrialage.ia2.logistics;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.content.TransportSpec.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.junit.jupiter.api.Test;

/**
 * Host-adapter contracts for item pipe, fluid pipe, and tank waterlogging wiring on Forge 1.19.2.
 */
class PipeWaterlogContractTest {
    @Test
    void pipeHierarchyImplementsSimpleWaterloggedBlockWhileTanksDoNot() throws Exception {
        long pipes =
                TransportSpec.DEVICES.values().stream()
                        .filter(spec -> spec.kind() != Kind.TANK)
                        .count();
        long tanks =
                TransportSpec.DEVICES.values().stream()
                        .filter(spec -> spec.kind() == Kind.TANK)
                        .count();
        assertEquals(16, pipes);
        assertEquals(5, tanks);

        var loader = PipeWaterlogContractTest.class.getClassLoader();
        Class<?> pipeClass =
                Class.forName("com.industrialage.ia2.logistics.TransportBlock$Pipe", false, loader);
        Class<?> itemPipeClass =
                Class.forName(
                        "com.industrialage.ia2.logistics.TransportBlock$ItemPipe", false, loader);

        assertFalse(SimpleWaterloggedBlock.class.isAssignableFrom(TransportBlock.class));
        assertTrue(SimpleWaterloggedBlock.class.isAssignableFrom(pipeClass));
        assertTrue(SimpleWaterloggedBlock.class.isAssignableFrom(itemPipeClass));

        assertNotNull(
                pipeClass.getDeclaredMethod(
                        "createBlockStateDefinition", StateDefinition.Builder.class));
        assertNotNull(pipeClass.getDeclaredMethod("getFluidState", BlockState.class));
        assertNotNull(pipeClass.getDeclaredMethod("getStateForPlacement", BlockPlaceContext.class));
        assertNotNull(
                TransportBlockBase.class.getDeclaredMethod(
                        "updateShape",
                        BlockState.class,
                        Direction.class,
                        BlockState.class,
                        LevelAccessor.class,
                        BlockPos.class,
                        BlockPos.class));
    }
}
