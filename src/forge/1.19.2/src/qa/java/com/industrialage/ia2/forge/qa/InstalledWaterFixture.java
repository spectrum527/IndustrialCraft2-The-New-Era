package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.watertest.WaterKineticChecks;
import com.industrialage.ia2.watertest.WaterNaturalChecks;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;

/** Runs normal player interactions on an installed Forge server with a frozen production JAR. */
final class InstalledWaterFixture {
    private final String phase;
    private final Properties checks = new Properties();
    private WaterKineticChecks water;
    private WaterNaturalChecks naturalWater;
    private int ticks;
    private boolean finished;

    InstalledWaterFixture(String phase) {
        this.phase = phase;
        MinecraftForge.EVENT_BUS.addListener(this::start);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private void start(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            var jar = ModList.get().getModFileById("ic2").getFile().getFilePath();
            check(
                    "production-jar-loaded",
                    Files.isSameFile(jar, Path.of("mods", jar.getFileName().toString())));
            checks.setProperty("artifact", jar.getFileName().toString());
            if (phase.startsWith("waterNatural")
                    || phase.equals("waterCanal")
                    || phase.equals("waterSeabed")
                    || phase.startsWith("waterFrozen")) {
                naturalWater =
                        new WaterNaturalChecks(
                                PlayableGameTests::block,
                                path -> PlayableGameTests.stack(path).getItem(),
                                this::check,
                                phase);
                naturalWater.start(
                        server.overworld(), FakePlayerFactory.getMinecraft(server.overworld()));
            } else {
                water =
                        new WaterKineticChecks(
                                PlayableGameTests::block,
                                path -> PlayableGameTests.stack(path).getItem(),
                                this::check,
                                new BlockPos(72, 56, 72),
                                phase);
                water.start(server.overworld(), FakePlayerFactory.getMinecraft(server.overworld()));
            }
        } catch (IOException | RuntimeException | AssertionError failure) {
            complete(server, failure);
        }
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (finished || water == null && naturalWater == null || event.phase != TickEvent.Phase.END)
            return;
        var server = event.getServer();
        try {
            if (++ticks > (naturalWater == null ? 1250 : 4000))
                throw new AssertionError("Water kinetic test timed out");
            if (!(naturalWater == null
                    ? water.tick(server.overworld())
                    : naturalWater.tick(server.overworld()))) return;
            check("world-saved", server.saveEverything(false, true, true));
            complete(server, null);
        } catch (RuntimeException | AssertionError failure) {
            complete(server, failure);
        }
    }

    private void check(String name, boolean passed) {
        if (!passed) throw new AssertionError(name);
        checks.setProperty(name, "passed");
    }

    private void complete(MinecraftServer server, Throwable failure) {
        finished = true;
        checks.setProperty("status", failure == null ? "passed" : "failed");
        checks.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        checks.setProperty("ticks", Integer.toString(ticks));
        if (failure != null) {
            var trace = new java.io.StringWriter();
            failure.printStackTrace(new java.io.PrintWriter(trace));
            checks.setProperty("problem", trace.toString());
        }
        try (var writer = Files.newBufferedWriter(Path.of(phase + ".properties"))) {
            checks.store(writer, "Installed Forge water kinetic checks");
        } catch (IOException error) {
            throw new IllegalStateException("Could not save water check results", error);
        } finally {
            server.halt(false);
        }
    }
}
