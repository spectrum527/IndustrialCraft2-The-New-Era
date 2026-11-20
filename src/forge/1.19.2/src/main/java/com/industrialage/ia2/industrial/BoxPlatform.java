package com.industrialage.ia2.industrial;

import com.industrialage.ia2.forge.content.ForgeContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registers portable storage, the personal safe and the industrial workbench. */
public final class BoxPlatform {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "ic2");
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "ic2");
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "ic2");
    private static final List<RegistryObject<BoxBlock>> BOXES = new ArrayList<>();
    private static final RegistryObject<BlockEntityType<BoxNode>> TYPE =
            TYPES.register(
                    "storage_box",
                    () ->
                            BlockEntityType.Builder.of(
                                            BoxNode::new,
                                            BOXES.stream()
                                                    .map(RegistryObject::get)
                                                    .toArray(Block[]::new))
                                    .build(null));

    private BoxPlatform() {}

    /** Registers the storage inventories and automation. */
    public static void install(IEventBus bus) {
        for (var k : BoxKind.values()) {
            var block =
                    BLOCKS.register(
                            k.path,
                            () ->
                                    new BoxBlock(
                                            BlockBehaviour.Properties.of(
                                                            k == BoxKind.WOODEN_STORAGE_BOX
                                                                    ? Material.WOOD
                                                                    : Material.METAL)
                                                    .strength(2, 5)
                                                    .sound(
                                                            k == BoxKind.WOODEN_STORAGE_BOX
                                                                    ? SoundType.WOOD
                                                                    : SoundType.METAL),
                                            k));
            BOXES.add(block);
            ITEMS.register(k.path, () -> new BlockItem(block.get(), ForgeContent.itemProperties()));
        }
        BoxMenus.install(bus);
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist
                == net.minecraftforge.api.distmarker.Dist.CLIENT) BoxMenus.clients(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TYPES.register(bus);
    }

    static BlockEntityType<BoxNode> type() {
        return TYPE.get();
    }

    static ItemStack named(ItemStack stack, Component name) {
        stack.setHoverName(name);
        return stack;
    }
}
