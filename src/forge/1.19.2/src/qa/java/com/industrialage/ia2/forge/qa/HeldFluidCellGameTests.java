package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.celltest.HeldCellChecks;
import com.industrialage.ia2.celltest.HeldCellChecks.Liquid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Tests the ordinary Forge player hand and block interaction paths for the native fluid cells. */
@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class HeldFluidCellGameTests {
    private HeldFluidCellGameTests() {}

    /** Collects world sources and checks that cells pour into compatible fluid ports. */
    @GameTest(template = "smoke")
    public static void normalHeldCellUse(GameTestHelper helper) {
        var source = helper.absolutePos(new BlockPos(1, 6, 1));
        var owner = PlayableGameTests.player(helper, source);
        HeldCellChecks.run(
                helper.getLevel(),
                owner,
                source,
                new HeldCellChecks.Host(
                        id -> PlayableGameTests.stack(id).getItem(),
                        PlayableGameTests::block,
                        cell -> liquid(FluidUtil.getFluidContained(cell).orElse(FluidStack.EMPTY)),
                        HeldFluidCellGameTests::seed,
                        pos -> liquid(tank(helper, pos).getFluidInTank(0)),
                        (pos, contents, simulate) ->
                                tank(helper, pos)
                                        .fill(
                                                fluid(contents),
                                                simulate
                                                        ? FluidAction.SIMULATE
                                                        : FluidAction.EXECUTE)),
                (name, ok) -> PlayableGameTests.check(helper, ok, name));
        helper.succeed();
    }

    private static ItemStack seed(ItemStack stack, Liquid contents) {
        var handler = FluidUtil.getFluidHandler(stack).orElseThrow(AssertionError::new);
        if (handler.fill(fluid(contents), FluidAction.EXECUTE) != contents.amount()) {
            throw new AssertionError("Could not seed the held fluid test cell");
        }
        return handler.getContainer();
    }

    private static IFluidHandler tank(GameTestHelper helper, BlockPos pos) {
        return PlayableGameTests.machine(helper, pos)
                .getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                .orElseThrow(() -> new AssertionError("Missing native fluid port at " + pos));
    }

    private static Liquid liquid(FluidStack stack) {
        return new Liquid(stack.getFluid(), stack.getAmount());
    }

    private static FluidStack fluid(Liquid liquid) {
        return new FluidStack(liquid.fluid(), liquid.amount());
    }
}
