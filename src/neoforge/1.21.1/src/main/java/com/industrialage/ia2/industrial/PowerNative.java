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

    static boolean tradeCapability(
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos pos,
            Direction face,
            net.minecraft.world.item.ItemStack price,
            net.minecraft.world.item.ItemStack goods) {
        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face);
        if (handler == null) return false;
        int slots = handler.getSlots();
        int[] takePlan = new int[slots];
        int owed = goods.getCount();
        for (int i = 0; i < slots && owed > 0; i++) {
            var current = handler.getStackInSlot(i);
            if (!current.isEmpty() && WorkshopPlatform.sameItem(current, goods)) {
                var taken = handler.extractItem(i, owed, true);
                if (!taken.isEmpty() && WorkshopPlatform.sameItem(taken, goods)) {
                    takePlan[i] = taken.getCount();
                    owed -= taken.getCount();
                }
            }
        }
        if (owed > 0) return false;
        var remainder = price.copy();
        for (int i = 0; i < slots && !remainder.isEmpty(); i++) {
            remainder = handler.insertItem(i, remainder, true);
        }
        if (!remainder.isEmpty()) return false;
        var extracted = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        for (int i = 0; i < slots; i++) {
            if (takePlan[i] > 0) {
                var got = handler.extractItem(i, takePlan[i], false);
                if (!got.isEmpty()) extracted.add(got);
            }
        }
        var payment = price.copy();
        for (int i = 0; i < slots && !payment.isEmpty(); i++) {
            payment = handler.insertItem(i, payment, false);
        }
        if (!payment.isEmpty()) {
            for (var rollback : extracted) {
                var left = rollback;
                for (int i = 0; i < slots && !left.isEmpty(); i++) {
                    left = handler.insertItem(i, left, false);
                }
                if (!left.isEmpty()) Block.popResource(level, pos, left);
            }
            return false;
        }
        return true;
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
