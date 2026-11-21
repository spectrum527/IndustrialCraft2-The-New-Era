package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.windtest.WindKineticChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Runs the real player-assembled wind to kinetic generator to storage line. */
@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class WindKineticGameTests {
    private WindKineticGameTests() {}

    /** Checks both available and delivered KU, swept clearance, orientation and recovery. */
    @GameTest(template = "smoke", batch = "wind", timeoutTicks = 250)
    public static void playerAssembledWindLine(GameTestHelper helper) {
        var near = helper.absolutePos(BlockPos.ZERO);
        var pos = new BlockPos((near.getX() & ~15) + 7, 200, (near.getZ() & ~15) + 7);
        var checks =
                new WindKineticChecks(
                        id ->
                                ForgeRegistries.BLOCKS.getValue(
                                        ResourceLocation.fromNamespaceAndPath("ic2", id)),
                        id ->
                                ForgeRegistries.ITEMS.getValue(
                                        ResourceLocation.fromNamespaceAndPath("ic2", id)),
                        (name, ok) -> {
                            if (!ok) helper.fail(name);
                        },
                        pos);
        checks.start(helper.getLevel(), FakePlayerFactory.getMinecraft(helper.getLevel()), false);
        tick(helper, checks);
    }

    private static void tick(GameTestHelper helper, WindKineticChecks checks) {
        helper.runAfterDelay(
                1,
                () -> {
                    if (checks.tick(helper.getLevel())) helper.succeed();
                    else tick(helper, checks);
                });
    }
}
