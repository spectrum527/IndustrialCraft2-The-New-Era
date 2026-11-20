package com.industrialage.ia2.fluids;

import com.industrialage.ia2.content.IndustrialFluid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Native source, flowing state, world block and bucket for each industrial fluid. */
public final class FluidContent {
    private static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, "ic2");
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, "ic2");
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("ic2");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ic2");
    static final List<Family> FAMILIES = new ArrayList<>();

    private FluidContent() {}

    /** Installs registrations before the loader freezes its registries. */
    public static void register(IEventBus bus, boolean client) {
        for (IndustrialFluid spec : IndustrialFluid.ALL) FAMILIES.add(new Family(spec));
        TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        if (client) FluidAppearance.install(bus);
    }

    static final class Family {
        final IndustrialFluid spec;
        final DeferredHolder<FluidType, FluidType> type;
        final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flow;
        final DeferredHolder<net.minecraft.world.level.block.Block, LiquidBlock> block;
        final DeferredHolder<Item, BucketItem> bucket;

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
                                                    .lightLevel(spec.light())));
            source = FLUIDS.register(path, () -> new BaseFlowingFluid.Source(properties()));
            flow =
                    FLUIDS.register(
                            "flowing_" + path, () -> new BaseFlowingFluid.Flowing(properties()));
            block =
                    BLOCKS.registerBlock(
                            path,
                            props ->
                                    new LiquidBlock(
                                            source.get(),
                                            FluidBlockProperties.nonSolid(props)
                                                    .noLootTable()
                                                    .liquid()
                                                    .replaceable()
                                                    .strength(100)
                                                    .lightLevel(state -> spec.light())));
            bucket =
                    ITEMS.registerItem(
                            path + "_bucket",
                            props ->
                                    new BucketItem(
                                            source.get(),
                                            props.stacksTo(1).craftRemainder(Items.BUCKET)));
        }

        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(type, source, flow)
                    .block(block)
                    .bucket(bucket)
                    .tickRate(spec.tickDelay());
        }
    }
}
