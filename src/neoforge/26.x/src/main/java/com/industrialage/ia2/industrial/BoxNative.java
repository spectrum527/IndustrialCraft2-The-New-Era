package com.industrialage.ia2.industrial;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.access.MachinePorts;
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
                                        node.kind.automatable()
                                                        && MachineAccess.connects(node, side)
                                                ? MachinePorts.item(
                                                        node,
                                                        side,
                                                        net.neoforged.neoforge.transfer.item
                                                                .VanillaContainerWrapper.of(
                                                                node.items))
                                                : null));
    }
}
