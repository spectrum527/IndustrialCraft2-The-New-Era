package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.forge.equipment.ForgeGear;
import com.industrialage.ia2.worldtest.FoodPaintChecks;
import com.industrialage.ia2.worldtest.WorldChecks;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

final class InstalledWorldProgram {
    private final Properties checks = new Properties();
    private WorldChecks world;
    private FoodPaintChecks details;
    private int ticks;
    private boolean finished;

    InstalledWorldProgram() {
        MinecraftForge.EVENT_BUS.addListener(this::start);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private void start(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            var candidate = ModList.get().getModFileById("ic2").getFile().getFilePath();
            check(
                    "installed-jar",
                    Files.isSameFile(
                            candidate, Path.of("mods", candidate.getFileName().toString())));
            checks.setProperty("artifact", candidate.getFileName().toString());
            if (Boolean.getBoolean("ic2.qa.worldDetails")) {
                details =
                        new FoodPaintChecks(
                                PlayableGameTests::block,
                                path -> PlayableGameTests.stack(path).getItem(),
                                this::check);
                if (Boolean.getBoolean("ic2.qa.worldCold")) {
                    details.cold(server.overworld());
                    complete(server, null);
                    return;
                }
                FoodPaintChecks.registry(
                        ForgeRegistries.BLOCKS.getKeys().stream()
                                .filter(id -> id.getNamespace().equals("ic2"))
                                .map(Object::toString)
                                .toList(),
                        ForgeRegistries.ITEMS.getKeys().stream()
                                .filter(id -> id.getNamespace().equals("ic2"))
                                .map(Object::toString)
                                .toList());
                details.setup(
                        server.overworld(), FakePlayerFactory.getMinecraft(server.overworld()));
                return;
            }
            world =
                    new WorldChecks(
                            PlayableGameTests::block,
                            path -> PlayableGameTests.stack(path).getItem(),
                            ForgeGear::charge,
                            this::check,
                            menu -> {
                                var live = (com.industrialage.ia2.forge.content.MachineMenu) menu;
                                return new WorldChecks.ProcessorValues(
                                        live.progress(),
                                        live.duration(),
                                        live.energy(),
                                        live.capacity());
                            },
                            1,
                            2,
                            "ic2_duration");
            if (Boolean.getBoolean("ic2.qa.worldCold")) {
                world.cold(server.overworld(), FakePlayerFactory.getMinecraft(server.overworld()));
                complete(server, null);
            } else if (Boolean.getBoolean("ic2.qa.worldNatural")) {
                world.natural(server.overworld());
                check("world-saved", server.saveEverything(false, true, true));
                complete(server, null);
            } else {
                world.setup(server.overworld(), FakePlayerFactory.getMinecraft(server.overworld()));
            }
        } catch (RuntimeException | AssertionError | IOException failure) {
            complete(server, failure);
        }
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if ((world == null && details == null) || finished || event.phase != TickEvent.Phase.END)
            return;
        ticks++;
        var server = event.getServer();
        try {
            var player = FakePlayerFactory.getMinecraft(server.overworld());
            if (details != null) {
                if (ticks > 600)
                    throw new AssertionError(
                            "Cables did not drive the three harvesters by 600 ticks");
                if (!details.tick(server.overworld(), player, ticks)) return;
                check("world-saved", server.saveEverything(false, true, true));
                complete(server, null);
                return;
            }
            boolean ready = world.automationTick(server.overworld(), player, ticks);
            if (ticks > 1200)
                throw new AssertionError("World simulation did not finish in 1200 ticks");
            if (ticks < 320 || !ready) return;
            world.checkOutcomes(server.overworld(), player);
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
        if (failure != null) {
            var trace = new java.io.StringWriter();
            failure.printStackTrace(new java.io.PrintWriter(trace));
            checks.setProperty("problem", trace.toString());
        }
        boolean cold = Boolean.getBoolean("ic2.qa.worldCold");
        String report =
                Boolean.getBoolean("ic2.qa.worldDetails")
                        ? (cold ? "world-details-cold.properties" : "world-details.properties")
                        : (cold ? "world-cold.properties" : "world-program.properties");
        checks.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        try (var writer = Files.newBufferedWriter(Path.of(report))) {
            checks.store(writer, "Installed Forge native world program");
        } catch (IOException error) {
            throw new IllegalStateException("Could not write native world program results", error);
        } finally {
            server.halt(false);
        }
    }
}
