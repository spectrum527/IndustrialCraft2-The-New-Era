package com.industrialage.ia2.industrial;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

final class BoxNative {
    private BoxNative() {}

    static BlockEntityType<BoxNode> type(Block[] blocks) {
        return new BlockEntityType<>(BoxNode::new, blocks);
    }

    static void install(IEventBus bus) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                ev ->
                        ev.registerBlockEntity(
                                Capabilities.Item.BLOCK,
                                BoxPlatform.type(),
                                (node, side) ->
                                        node.kind.portable()
                                                ? net.neoforged.neoforge.transfer.item
                                                        .VanillaContainerWrapper.of(node.items)
                                                : null));
    }
}
