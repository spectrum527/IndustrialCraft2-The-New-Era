package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.slottest.SlotRulesChecks;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runs the native inventory regression on the Forge server. */
@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class SlotRulesGameTests {
    private SlotRulesGameTests() {}

    /** Checks filtered I/O and completed products of real machines after native server ticks. */
    @GameTest(template = "smoke", timeoutTicks = 60)
    public static void machineAutomationUsesSlotRoles(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(1, 105, 1));
        var player = PlayableGameTests.player(helper, origin);
        var checks =
                new SlotRulesChecks(
                        origin,
                        PlayableGameTests::block,
                        PlayableGameTests::stack,
                        new SlotRulesHost(),
                        2);
        BiConsumer<String, Boolean> check =
                (name, passed) -> PlayableGameTests.check(helper, passed, name);
        checks.place(level, player, check);
        helper.onEachTick(
                () -> {
                    if (checks.tick(level, check)) helper.succeed();
                });
    }
}
