package com.industrialage.ia2.industrial;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

final class PowerNative {
    private PowerNative() {}

    static BlockEntityType<PowerNode> type(Block[] blocks) {
        return new BlockEntityType<>(PowerNode::new, blocks);
    }

    static boolean shock(
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.LivingEntity victim,
            int damage) {
        return victim.hurtServer(level, level.damageSources().magic(), damage);
    }

    static void automation(IEventBus bus) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event ->
                        event.registerBlockEntity(
                                Capabilities.Fluid.BLOCK,
                                PowerPlatform.type(),
                                (node, side) ->
                                        (node.kind == PowerKind.GEO_GENERATOR
                                                        || node.kind
                                                                == PowerKind.REACTOR_FLUID_PORT)
                                                ? node.inlet
                                                : null));
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event ->
                        event.registerBlockEntity(
                                Capabilities.Item.BLOCK,
                                PowerPlatform.type(),
                                (node, side) ->
                                        new PowerItemPort(
                                                node, side == null ? Direction.UP : side)));
    }
}
