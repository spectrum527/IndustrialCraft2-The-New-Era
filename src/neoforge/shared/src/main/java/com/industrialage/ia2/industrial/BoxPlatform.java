package com.industrialage.ia2.industrial;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers portable storage, the personal safe and the industrial workbench. */
public final class BoxPlatform {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("ic2");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ic2");
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "ic2");
    private static final List<DeferredBlock<BoxBlock>> BOXES = new ArrayList<>();
    private static final java.util.function.Supplier<BlockEntityType<BoxNode>> TYPE =
            TYPES.register(
                    "storage_box",
                    () ->
                            BoxNative.type(
                                    BOXES.stream().map(DeferredBlock::get).toArray(Block[]::new)));

    private BoxPlatform() {}

    /** Registers the storage inventories and automation. */
    public static void install(IEventBus bus) {
        for (var k : BoxKind.values()) {
            var block =
                    BLOCKS.registerBlock(
                            k.path,
                            p ->
                                    new BoxBlock(
                                            p.strength(2.0F, 5.0F)
                                                    .sound(
                                                            k == BoxKind.WOODEN_STORAGE_BOX
                                                                    ? SoundType.WOOD
                                                                    : SoundType.METAL),
                                            k));
            BOXES.add(block);
            ITEMS.registerSimpleBlockItem(block);
        }
        BoxNative.install(bus);
        BoxMenus.install(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TYPES.register(bus);
    }

    /** Register the authored box and workbench screens after creating all menu types. */
    public static void installScreens(IEventBus bus) {
        BoxMenus.clients(bus);
    }

    static BlockEntityType<BoxNode> type() {
        return TYPE.get();
    }

    static ItemStack named(ItemStack stack, Component name) {
        stack.set(DataComponents.CUSTOM_NAME, name);
        return stack;
    }
}
