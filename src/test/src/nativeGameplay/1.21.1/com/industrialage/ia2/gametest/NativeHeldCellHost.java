package com.industrialage.ia2.gametest;

import com.industrialage.ia2.celltest.HeldCellChecks;
import com.industrialage.ia2.celltest.HeldCellChecks.Liquid;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Native fluid capability observations for the common hand-use matrix. */
final class NativeHeldCellHost {
    private NativeHeldCellHost() {}

    static void run(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            BiConsumer<String, Boolean> check) {
        HeldCellChecks.run(level, player, origin, access(level), check);
    }

    private static HeldCellChecks.Host access(ServerLevel level) {
        return new HeldCellChecks.Host(
                NativeGameplayFixture::item,
                NativeGameplayFixture::block,
                stack -> liquid(FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY)),
                (stack, contents) -> NativeTransferAccess.filledCell(stack, nativeFluid(contents)),
                (stack, name) -> stack.set(DataComponents.CUSTOM_NAME, name),
                pos -> tank(level, pos),
                (pos, contents, simulate) ->
                        NativeTransferAccess.fill(
                                level, pos, Direction.UP, nativeFluid(contents), simulate));
    }

    private static Liquid tank(ServerLevel level, BlockPos pos) {
        var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
        if (handler == null) throw new AssertionError("Missing native fluid port at " + pos);
        return liquid(handler.getFluidInTank(0));
    }

    private static Liquid liquid(FluidStack stack) {
        return new Liquid(stack.getFluid(), stack.getAmount());
    }

    private static FluidStack nativeFluid(Liquid liquid) {
        return new FluidStack(liquid.fluid(), liquid.amount());
    }
}
