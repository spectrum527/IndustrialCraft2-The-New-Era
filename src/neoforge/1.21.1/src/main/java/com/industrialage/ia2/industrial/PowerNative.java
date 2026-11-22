package com.industrialage.ia2.industrial;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.access.MachinePorts;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

final class PowerNative {
    private PowerNative() {}

    static boolean handFluid(PowerNode node, Player player, InteractionHand hand) {
        return net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(
                player, hand, new LavaInlet(node));
    }

    static BlockEntityType<PowerNode> type(Block[] blocks) {
        return BlockEntityType.Builder.of(PowerNode::new, blocks).build(null);
    }

    static boolean shock(
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.LivingEntity victim,
            int damage) {
        return victim.hurt(level.damageSources().magic(), damage);
    }

    static void automation(IEventBus bus) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event ->
                        event.registerBlockEntity(
                                Capabilities.FluidHandler.BLOCK,
                                PowerPlatform.type(),
                                (node, side) ->
                                        (node.kind == PowerKind.GEO_GENERATOR
                                                                || node.kind
                                                                        == PowerKind
                                                                                .REACTOR_FLUID_PORT)
                                                        && MachineAccess.connects(node, side)
                                                ? MachinePorts.fluid(
                                                        node, side, new LavaInlet(node))
                                                : null));
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event ->
                        event.registerBlockEntity(
                                Capabilities.ItemHandler.BLOCK,
                                PowerPlatform.type(),
                                (node, side) ->
                                        MachineAccess.connects(node, side)
                                                ? MachinePorts.item(
                                                        node,
                                                        side,
                                                        new PowerItemPort(
                                                                node,
                                                                side == null ? Direction.UP : side))
                                                : null));
    }
}
