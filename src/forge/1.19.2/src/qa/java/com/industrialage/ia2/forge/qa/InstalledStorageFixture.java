package com.industrialage.ia2.forge.qa;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

/** Installs the independent matrix beside a reobfuscated JAR on a real dedicated server. */
final class InstalledStorageFixture {
    private final Properties checks = new Properties();
    private int ticks;
    private boolean finished;

    InstalledStorageFixture() {
        MinecraftForge.EVENT_BUS.addListener(this::start);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private void start(ServerStartedEvent event) {
        var level = event.getServer().overworld();
        level.setChunkForced(0, 0, true);
        level.getChunk(0, 0);
        try {
            var artifact = ModList.get().getModFileById("ic2").getFile().getFilePath();
            check(
                    "production-jar-loaded",
                    Files.isSameFile(artifact, Path.of("mods", artifact.getFileName().toString())));
            checks.setProperty("artifact", artifact.getFileName().toString());
        } catch (IOException | RuntimeException | AssertionError failure) {
            complete(event.getServer(), failure);
        }
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END || ++ticks < 30) return;
        var server = event.getServer();
        var level = server.overworld();
        try {
            var owner =
                    FakePlayerFactory.get(
                            level, new GameProfile(UUID.randomUUID(), "StorageOwner"));
            var outsider =
                    FakePlayerFactory.get(
                            level, new GameProfile(UUID.randomUUID(), "StorageOutsider"));
            StorageMatrix.run(level, new BlockPos(8, 112, 8), owner, outsider, this::check);
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
        if (failure != null) checks.setProperty("problem", failure.toString());
        try (var writer = Files.newBufferedWriter(Path.of("storage.properties"))) {
            checks.store(writer, "IC2 installed Forge storage matrix");
        } catch (IOException error) {
            throw new IllegalStateException(error);
        } finally {
            server.halt(false);
        }
    }
}
