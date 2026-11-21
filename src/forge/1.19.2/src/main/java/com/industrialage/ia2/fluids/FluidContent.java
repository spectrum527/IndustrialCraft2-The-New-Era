package com.industrialage.ia2.fluids;

import com.industrialage.ia2.content.IndustrialFluid;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registers the native 1.19.2 fluid families with their world blocks, buckets and artwork. */
public final class FluidContent {
    private static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, "ic2");
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, "ic2");
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "ic2");
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "ic2");

    private FluidContent() {}

    /** Attaches all seventeen source/flow/block/bucket registrations to the mod bus. */
    public static void register(IEventBus bus) {
        IndustrialFluid.ALL.forEach(Family::new);
        TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private static final class Family {
        final RegistryObject<FluidType> type;
        final RegistryObject<ForgeFlowingFluid.Source> source;
        final RegistryObject<ForgeFlowingFluid.Flowing> flow;
        final RegistryObject<LiquidBlock> block;
        final RegistryObject<BucketItem> bucket;
        final IndustrialFluid spec;

        Family(IndustrialFluid spec) {
            this.spec = spec;
            String path = spec.path();
            type =
                    TYPES.register(
                            path,
                            () ->
                                    new FluidType(
                                            FluidType.Properties.create()
                                                    .descriptionId("fluid.ic2." + path)
                                                    .density(spec.density())
                                                    .viscosity(spec.viscosity())
                                                    .temperature(spec.temperature())
                                                    .lightLevel(spec.light())) {
                                        @Override
                                        public void initializeClient(
                                                Consumer<IClientFluidTypeExtensions> consumer) {
                                            consumer.accept(
                                                    new IClientFluidTypeExtensions() {
                                                        @Override
                                                        public ResourceLocation getStillTexture() {
                                                            return new ResourceLocation(
                                                                    "ic2", spec.still());
                                                        }

                                                        @Override
                                                        public ResourceLocation
                                                                getFlowingTexture() {
                                                            return new ResourceLocation(
                                                                    "ic2", spec.flow());
                                                        }

                                                        @Override
                                                        public int getTintColor() {
                                                            return spec.argb();
                                                        }
                                                    });
                                        }
                                    });
            source = FLUIDS.register(path, () -> new ForgeFlowingFluid.Source(properties()));
            flow =
                    FLUIDS.register(
                            "flowing_" + path, () -> new ForgeFlowingFluid.Flowing(properties()));
            block =
                    BLOCKS.register(
                            path,
                            () ->
                                    new LiquidBlock(
                                            source,
                                            BlockBehaviour.Properties.of(Material.WATER)
                                                    .noCollission()
                                                    .noLootTable()
                                                    .strength(100)
                                                    .lightLevel(s -> spec.light())));
            bucket =
                    ITEMS.register(
                            path + "_bucket",
                            () ->
                                    new BucketItem(
                                            source,
                                            ForgeContent.itemProperties(path + "_bucket")
                                                    .stacksTo(1)
                                                    .craftRemainder(Items.BUCKET)));
        }

        private ForgeFlowingFluid.Properties properties() {
            return new ForgeFlowingFluid.Properties(type, source, flow)
                    .block(block)
                    .bucket(bucket)
                    .tickRate(spec.tickDelay());
        }
    }
}
