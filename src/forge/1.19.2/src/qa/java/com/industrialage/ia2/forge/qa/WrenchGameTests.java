package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.forge.equipment.ForgeGear;
import com.industrialage.ia2.wrenchtest.WrenchChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Native 1.19.2 wrench fixture. */
@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class WrenchGameTests {
    private WrenchGameTests() {}

    /** Runs wrench interaction with native permissions, inventory and energy handling. */
    @GameTest(template = "smoke")
    public static void wrenchZones(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var drops =
                WrenchChecks.run(
                        helper.getLevel(),
                        pos,
                        PlayableGameTests.player(helper, pos),
                        id -> PlayableGameTests.stack(id).getItem(),
                        PlayableGameTests::block,
                        ForgeGear::restoreEnergy,
                        ForgeGear::stored,
                        (name, passed) -> PlayableGameTests.check(helper, passed, name));
        var level = helper.getLevel();
        var utility =
                com.industrialage.ia2.wrenchtest.WrenchLootChecks.run(
                        level,
                        pos,
                        PlayableGameTests.player(helper, pos),
                        id -> PlayableGameTests.stack(id).getItem(),
                        PlayableGameTests::block,
                        (at, stack) ->
                                level.getBlockEntity(at)
                                        .getCapability(
                                                net.minecraftforge.common.capabilities
                                                        .ForgeCapabilities.ITEM_HANDLER,
                                                net.minecraft.core.Direction.WEST)
                                        .orElseThrow(AssertionError::new)
                                        .insertItem(0, stack, false)
                                        .isEmpty(),
                        (at, amount) ->
                                level.getBlockEntity(at)
                                                .getCapability(
                                                        net.minecraftforge.common.capabilities
                                                                .ForgeCapabilities.FLUID_HANDLER,
                                                        net.minecraft.core.Direction.WEST)
                                                .orElseThrow(AssertionError::new)
                                                .fill(
                                                        new net.minecraftforge.fluids.FluidStack(
                                                                net.minecraft.world.level.material
                                                                        .Fluids.WATER,
                                                                amount),
                                                        net.minecraftforge.fluids.capability
                                                                .IFluidHandler.FluidAction.EXECUTE)
                                        == amount,
                        (name, passed) -> PlayableGameTests.check(helper, passed, name));
        helper.runAfterDelay(
                2,
                () -> {
                    drops.run();
                    utility.run();
                    helper.succeed();
                });
    }
}
