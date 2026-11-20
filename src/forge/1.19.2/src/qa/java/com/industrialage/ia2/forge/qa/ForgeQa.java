package com.industrialage.ia2.forge.qa;

import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Loads the independent Forge acceptance fixtures only in the QA source set. */
@Mod("ic2_qa")
@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class ForgeQa {
    /** Registers only the probes selected for this launch. */
    public ForgeQa() throws ParserConfigurationException {
        if (Boolean.getBoolean("forge.gameTestServer")) {
            GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new File("game-test.xml")));
        }
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        if (System.getProperty("ic2.compat.mode") != null) {
            new com.industrialage.ia2.compatqa.OptionalCompatFixture();
        }
        var phase = System.getProperty("ic2.qa.worldPhase");
        if (phase != null) {
            bus.addListener(
                    (RegisterCapabilitiesEvent event) -> {
                        event.register(String.class);
                        event.register(Integer.class);
                    });
            bus.addListener(
                    (FMLConstructModEvent event) ->
                            event.enqueueWork(
                                    () -> {
                                        new PlatformWorldFixture(phase, bus);
                                    }));
        }
        if (Boolean.getBoolean("ic2.qa.storage")) new InstalledStorageFixture();
        if (Boolean.getBoolean("ic2.qa.worldProgram")) new InstalledWorldProgram();
        if (Boolean.getBoolean("ic2.qa.clientAcceptance")) {
            var menus = new PlayMenuFixture(bus);
            DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT, () -> () -> new PlayClientFixture(bus, menus));
        }
    }

    /** Loads an external structure, places a block, and checks it two ticks later. */
    @GameTest(template = "smoke", required = true, timeoutTicks = 20)
    public static void harnessSmoke(GameTestHelper helper) {
        if (!ModList.get().isLoaded("ic2")) {
            throw new AssertionError("Missing production mod");
        }
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
