package com.industrialage.ia2.forge;

import com.industrialage.ia2.forge.client.ForgeClient;
import com.industrialage.ia2.network.EchoProtocol;
import com.industrialage.ia2.platform.ClientFacade;
import com.industrialage.ia2.platform.Platform;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The common services and Forge bindings of one mod container. Consumers retrieve this record
 * through the container's custom extension API.
 *
 * @param services loader-independent services
 * @param hostEvents mappings from Forge events to neutral streams
 * @param blockCapabilities declarations and coordinates for sided block queries
 */
public record ForgePlatform(
        Platform services, ForgeEvents hostEvents, ForgeCapabilities blockCapabilities)
        implements IExtensionPoint<ForgePlatform> {
    private static final Logger LOG = LoggerFactory.getLogger(ForgePlatform.class);

    /** Installs this container's adapters on the supplied event buses. */
    public static ForgePlatform create(
            IEventBus modBus, IEventBus gameBus, ModContainer container, Dist side) {
        var events = new ForgeEvents(gameBus);
        var capabilities = new ForgeCapabilities(modBus, gameBus);
        var network = new ForgeNetwork(modBus);
        var echo =
                new EchoProtocol(
                        network.channel(),
                        (direction, sequence) ->
                                LOG.debug(
                                        "IC2 echo completed on {} sequence {}",
                                        direction,
                                        sequence));
        var config = new ForgeConfig(container, gameBus);
        ClientFacade client;
        if (side == Dist.CLIENT) {
            var open = new AtomicBoolean(true);
            modBus.addListener((FMLLoadCompleteEvent event) -> open.set(false));
            client = ClientFacade.physicalClient(open::get);
            client.initialize(() -> () -> ForgeClient.register(modBus, gameBus, echo, network));
        } else {
            client = ClientFacade.dedicatedServer();
        }
        var services =
                new Platform(
                        new ForgeRegistry(modBus),
                        events,
                        network.channel(),
                        capabilities,
                        client,
                        config.facade());
        return new ForgePlatform(services, events, capabilities);
    }
}
