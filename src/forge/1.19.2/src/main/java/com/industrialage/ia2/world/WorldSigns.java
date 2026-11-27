package com.industrialage.ia2.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

final class WorldSigns {
    private static final WoodType RUBBER = WoodType.register(new WoodType("ic2:rubber") {});

    private WorldSigns() {}

    static void install(IEventBus bus) {
        var standing =
                WorldPlatform.registerBlock(
                        "rubber_sign",
                        properties -> new StandingSign(settings(properties), RUBBER),
                        false);
        var wall =
                WorldPlatform.registerBlock(
                        "rubber_wall_sign",
                        properties -> new WallSign(settings(properties), RUBBER),
                        false);
        WorldPlatform.registerItem(
                "rubber_sign",
                props -> new SignItem(props.stacksTo(16), standing.get(), wall.get()));
        type =
                WorldPlatform.registerSignType(
                        () ->
                                WorldNative.type(
                                        RubberSignData::new,
                                        new Block[] {standing.get(), wall.get()}));
        if (FMLEnvironment.dist == Dist.CLIENT) Client.install(bus);
    }

    private static Block.Properties settings(Block.Properties properties) {
        return WorldNative.noCollision(properties).noOcclusion().strength(1F).sound(SoundType.WOOD);
    }

    private static java.util.function.Supplier<BlockEntityType<SignBlockEntity>> type;

    private static final class StandingSign extends StandingSignBlock {
        StandingSign(Properties properties, WoodType wood) {
            super(properties, wood);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new RubberSignData(pos, state);
        }
    }

    private static final class WallSign extends WallSignBlock {
        WallSign(Properties properties, WoodType wood) {
            super(properties, wood);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new RubberSignData(pos, state);
        }
    }

    private static final class RubberSignData extends SignBlockEntity {
        RubberSignData(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public BlockEntityType<?> getType() {
            return type.get();
        }
    }

    private static final class Client {
        static void install(IEventBus bus) {
            bus.addListener(
                    (FMLClientSetupEvent event) ->
                            event.enqueueWork(
                                    () ->
                                            net.minecraft.client.renderer.Sheets.addWoodType(
                                                    RUBBER)));
            bus.addListener(
                    (EntityRenderersEvent.RegisterRenderers event) ->
                            event.registerBlockEntityRenderer(
                                    type.get(),
                                    net.minecraft.client.renderer.blockentity.SignRenderer::new));
        }
    }
}
