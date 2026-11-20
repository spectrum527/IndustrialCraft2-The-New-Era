package com.industrialage.ia2;

import com.industrialage.ia2.audio.SoundPlatform;
import com.industrialage.ia2.compat.OptionalMods;
import com.industrialage.ia2.fluids.FluidContent;
import com.industrialage.ia2.gameplay.IndustrialContent;
import com.industrialage.ia2.industrial.BoxPlatform;
import com.industrialage.ia2.industrial.PowerPlatform;
import com.industrialage.ia2.industrial.Radiation;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import com.industrialage.ia2.lifecycle.ServerLifecycle;
import com.industrialage.ia2.logistics.TransportPlatform;
import com.industrialage.ia2.platform.NeoForgePlatform;
import com.industrialage.ia2.platform.NeoForgeVersion;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loader entry point for IndustrialCraft 2. */
@Mod("ic2")
public final class IndustrialAge2 {
    private static final Logger LOG = LoggerFactory.getLogger(IndustrialAge2.class);

    /** Called by NeoForge when the mod is constructed. */
    public IndustrialAge2(IEventBus modBus, ModContainer container, Dist dist) {
        OptionalMods.install(modBus);
        var platform = new NeoForgePlatform(modBus, NeoForge.EVENT_BUS, container, dist);
        IndustrialContent.install(modBus, NeoForge.EVENT_BUS, platform.services().config(), dist);
        TransportPlatform.install(modBus);
        com.industrialage.ia2.world.WorldPlatform.install(
                modBus, NeoForge.EVENT_BUS, dist == Dist.CLIENT);
        WorkshopPlatform.install(modBus);
        FluidContent.register(modBus, dist == Dist.CLIENT);
        PowerPlatform.install(modBus, platform.services().config());
        if (dist == Dist.CLIENT)
            com.industrialage.ia2.industrial.ChargeTypes.installRenderer(modBus);
        BoxPlatform.install(modBus);
        if (dist == Dist.CLIENT) {
            BoxPlatform.installScreens(modBus);
            com.industrialage.ia2.logistics.TankMenus.clients(modBus);
        }
        Radiation.install(modBus, net.neoforged.neoforge.common.NeoForge.EVENT_BUS);
        SoundPlatform.install(modBus, dist == Dist.CLIENT);
        container.registerExtensionPoint(NeoForgePlatform.class, platform);
        bindServerEvents(NeoForge.EVENT_BUS, platform);

        var metadata = container.getModInfo();
        LOG.info("Loaded {} {}", metadata.getDisplayName(), metadata.getVersion());
    }

    private static void bindServerEvents(IEventBus gameBus, NeoForgePlatform platform) {
        var lifecycle = new ServerLifecycle(platform.services().events());
        lifecycle.initialize();
        LOG.info("IC2 platform initialized");
        gameBus.addListener(
                ServerAboutToStartEvent.class,
                event -> {
                    lifecycle.start();
                    LOG.info("IC2 server session started");
                });
        gameBus.addListener(
                LevelEvent.Load.class,
                event -> {
                    if (event.getLevel() instanceof ServerLevel level) {
                        String id = NeoForgeVersion.dimension(level);
                        lifecycle.loadWorld(id);
                        LOG.info("IC2 world loaded {}", id);
                    }
                });
        gameBus.addListener(
                ServerTickEvent.Post.class,
                event -> {
                    if (lifecycle.tick() == 1) {
                        LOG.info("IC2 first server tick");
                    }
                });
        gameBus.addListener(
                LevelEvent.Unload.class,
                event -> {
                    if (event.getLevel() instanceof ServerLevel level) {
                        String id = NeoForgeVersion.dimension(level);
                        lifecycle.unloadWorld(id);
                        LOG.info("IC2 world unloaded {}", id);
                    }
                });
        gameBus.addListener(
                ServerStoppedEvent.class,
                event -> {
                    try {
                        lifecycle.stop();
                        LOG.info("IC2 server session stopped");
                    } finally {
                        platform.hostEvents().endServerSession();
                    }
                });
    }
}
