package com.industrialage.ia2;

import com.industrialage.ia2.audio.SoundPlatform;
import com.industrialage.ia2.compat.OptionalMods;
import com.industrialage.ia2.fluids.FluidContent;
import com.industrialage.ia2.forge.ForgePlatform;
import com.industrialage.ia2.forge.content.ForgeContent;
import com.industrialage.ia2.industrial.BoxPlatform;
import com.industrialage.ia2.industrial.PowerPlatform;
import com.industrialage.ia2.industrial.Radiation;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import com.industrialage.ia2.lifecycle.ServerLifecycle;
import com.industrialage.ia2.logistics.TransportPlatform;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Forge 1.19.2 entry point for IndustrialCraft 2. */
@Mod("ic2")
public final class IndustrialAge2 {
    private static final Logger LOG = LoggerFactory.getLogger(IndustrialAge2.class);

    /** Constructs one platform and wires the logical server lifecycle on either physical side. */
    public IndustrialAge2(FMLJavaModLoadingContext context) {
        OptionalMods.install(context.getModEventBus());
        var container = context.getContainer();
        var gameBus = MinecraftForge.EVENT_BUS;
        var platform =
                ForgePlatform.create(
                        context.getModEventBus(), gameBus, container, FMLEnvironment.dist);
        container.registerExtensionPoint(ForgePlatform.class, () -> platform);
        ForgeContent.register(context.getModEventBus(), gameBus, platform.services().config());
        com.industrialage.ia2.forge.equipment.ForgeGear.install(context.getModEventBus(), gameBus);
        TransportPlatform.register(context.getModEventBus());
        com.industrialage.ia2.wrench.WrenchNative.install(gameBus);
        com.industrialage.ia2.world.WorldPlatform.install(context.getModEventBus(), gameBus);
        WorkshopPlatform.install(context.getModEventBus());
        FluidContent.register(context.getModEventBus());
        PowerPlatform.install(context.getModEventBus(), platform.services().config());
        BoxPlatform.install(context.getModEventBus());
        Radiation.install(
                context.getModEventBus(), net.minecraftforge.common.MinecraftForge.EVENT_BUS);
        SoundPlatform.install(context.getModEventBus(), FMLEnvironment.dist.isClient());
        var lifecycle = new ServerLifecycle(platform.services().events());
        lifecycle.initialize();
        LOG.info("IC2 platform initialized");

        gameBus.addListener(
                (ServerAboutToStartEvent event) -> {
                    lifecycle.start();
                    LOG.info("IC2 server session started");
                });
        gameBus.addListener(
                (LevelEvent.Load event) -> {
                    if (event.getLevel() instanceof ServerLevel level) {
                        String id = level.dimension().location().toString();
                        lifecycle.loadWorld(id);
                        LOG.info("IC2 world loaded {}", id);
                    }
                });
        gameBus.addListener(
                (TickEvent.ServerTickEvent event) -> {
                    if (event.phase == TickEvent.Phase.END && lifecycle.tick() == 1) {
                        LOG.info("IC2 first server tick");
                    }
                });
        gameBus.addListener(
                (LevelEvent.Unload event) -> {
                    if (event.getLevel() instanceof ServerLevel level) {
                        String id = level.dimension().location().toString();
                        lifecycle.unloadWorld(id);
                        LOG.info("IC2 world unloaded {}", id);
                    }
                });
        gameBus.addListener(
                (ServerStoppedEvent event) -> {
                    try {
                        lifecycle.stop();
                        LOG.info("IC2 server session stopped");
                    } finally {
                        platform.hostEvents().endServerSession();
                    }
                });
        var info = container.getModInfo();
        LOG.info("Loaded {} {}", info.getDisplayName(), info.getVersion());
    }
}
