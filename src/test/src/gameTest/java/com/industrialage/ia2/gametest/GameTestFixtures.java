package com.industrialage.ia2.gametest;

import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Registers the report writer and smoke test for the 1.21.1 development server. */
@Mod("ic2_gametest")
@GameTestHolder("ic2_gametest")
@PrefixGameTestTemplate(false)
public final class GameTestFixtures {
    /** Installs the native per-case reporter in the isolated run directory. */
    public GameTestFixtures(IEventBus modBus) throws ParserConfigurationException {
        GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new File("game-test.xml")));
        String phase = System.getProperty("ic2.qa.worldPhase");
        if (phase != null) {
            new PlatformWorldFixture(phase, modBus);
        }
    }

    /** Checks that the live test world loads the template and keeps a block placed by the test. */
    @GameTest(template = "smoke", required = true, timeoutTicks = 20)
    public static void harnessSmoke(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("ic2"), "The production mod was not loaded");
        // The template starts one block above the structure block used as the origin.
        helper.assertBlockPresent(Blocks.GOLD_BLOCK, 0, 1, 0);
        helper.setBlock(0, 2, 0, Blocks.IRON_BLOCK);
        helper.runAfterDelay(
                2,
                () -> {
                    helper.assertBlockPresent(Blocks.IRON_BLOCK, 0, 2, 0);
                    helper.succeed();
                });
    }
}
