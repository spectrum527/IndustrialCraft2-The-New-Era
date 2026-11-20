package com.industrialage.ia2.world;

import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;

final class WorldSigns {
    private static final WoodType RUBBER =
            WoodType.register(new WoodType("ic2:rubber", BlockSetType.OAK));

    private WorldSigns() {}

    static void install(IEventBus bus, boolean client) {
        var standing =
                WorldPlatform.registerBlock(
                        "rubber_sign",
                        properties -> new StandingSignBlock(RUBBER, settings(properties)),
                        false);
        var wall =
                WorldPlatform.registerBlock(
                        "rubber_wall_sign",
                        properties -> new WallSignBlock(RUBBER, settings(properties)),
                        false);
        WorldPlatform.registerItem(
                "rubber_sign",
                props -> new SignItem(props.stacksTo(16), standing.get(), wall.get()));
        bus.addListener(
                BlockEntityTypeAddBlocksEvent.class,
                event -> event.modify(BlockEntityType.SIGN, standing.get(), wall.get()));
        if (client) WorldSignClient.install(bus, RUBBER);
    }

    private static Block.Properties settings(Block.Properties properties) {
        return WorldNative.noCollision(properties).noOcclusion().strength(1F).sound(SoundType.WOOD);
    }
}
