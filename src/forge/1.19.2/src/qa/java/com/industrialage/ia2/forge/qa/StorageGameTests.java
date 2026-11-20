package com.industrialage.ia2.forge.qa;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runs the storage matrix as a native Forge GameTest. */
@GameTestHolder("ic2_storage")
@PrefixGameTestTemplate(false)
public final class StorageGameTests {
    private StorageGameTests() {}

    /** All 24 combinations use survival game-mode breaks, item pickup and placement. */
    @GameTest(template = "smoke", timeoutTicks = 80)
    public static void allBoxRemovalMethods(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        StorageMatrix.run(
                helper.getLevel(),
                pos,
                PlayableGameTests.player(helper, pos),
                PlayableGameTests.player(helper, pos),
                (name, passed) -> {
                    if (!passed) helper.fail(name);
                });
        helper.succeed();
    }
}
