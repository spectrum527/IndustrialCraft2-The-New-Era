package com.industrialage.ia2.platform;

import com.industrialage.ia2.client.ClientServices;
import com.industrialage.ia2.network.EchoProtocol;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Composes the services of one NeoForge container and exposes them to its consumers. */
public final class NeoForgePlatform implements IExtensionPoint {
    private static final Logger LOG = LoggerFactory.getLogger(NeoForgePlatform.class);
    private final NeoForgeEvents events;
    private final NeoForgeCapabilities capabilities;
    private final Platform services;
    private volatile boolean initializationOpen = true;

    /** Wires both logical sides. Physical client actions are created only on the client. */
    public NeoForgePlatform(
            IEventBus modBus, IEventBus gameBus, ModContainer container, Dist side) {
        events = new NeoForgeEvents(gameBus);
        capabilities = new NeoForgeCapabilities(modBus, gameBus);
        var network = new NeoForgeNetwork(modBus, side);
        var echo =
                new EchoProtocol(
                        network.channel(),
                        (direction, sequence) ->
                                LOG.debug(
                                        "IC2 echo completed on {} sequence {}",
                                        direction,
                                        sequence));
        var config = new NeoForgeConfig(container, gameBus);
        ClientFacade client;
        if (side == Dist.CLIENT) {
            client = ClientFacade.physicalClient(() -> initializationOpen);
            client.initialize(
                    () ->
                            () ->
                                    ClientServices.install(
                                            modBus, gameBus, echo, network::clientSender));
        } else {
            client = ClientFacade.dedicatedServer();
        }
        modBus.addListener(FMLLoadCompleteEvent.class, event -> initializationOpen = false);
        services =
                new Platform(
                        new NeoForgeRegistry(modBus),
                        events,
                        network.channel(),
                        capabilities,
                        client,
                        config.facade());
    }

    /** Returns the six loader-independent services for this mod instance. */
    public Platform services() {
        return services;
    }

    /** Returns the adapter for explicitly forwarding host events to neutral streams. */
    public NeoForgeEvents hostEvents() {
        return events;
    }

    /** Returns the adapter used to register and locate sided block capabilities. */
    public NeoForgeCapabilities blockCapabilities() {
        return capabilities;
    }
}
