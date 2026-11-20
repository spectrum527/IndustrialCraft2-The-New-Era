package com.industrialage.ia2.world;

import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;

final class WorldSignClient {
    private WorldSignClient() {}

    static void install(IEventBus bus, WoodType wood) {
        bus.addListener(
                net.neoforged.fml.event.lifecycle.FMLClientSetupEvent.class,
                event ->
                        event.enqueueWork(
                                () -> net.minecraft.client.renderer.Sheets.addWoodType(wood)));
    }
}
