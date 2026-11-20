package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Native save adapter for electrical devices and reactor components. */
abstract class PowerSaveBase extends BlockEntity {
    final LavaInlet inlet = new LavaInlet((PowerNode) this);

    PowerSaveBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        var node = (PowerNode) this;
        var contents =
                java.util.stream.IntStream.range(0, node.inventory.getContainerSize())
                        .mapToObj(node.inventory::getItem)
                        .toList();
        tag.store("items", ItemStack.OPTIONAL_CODEC.listOf(), contents);
        tag.putDouble("eu", node.energy);
        tag.putDouble("water_fraction", node.waterFraction);
        tag.putInt("fuel", node.fuel);
        tag.putInt("millibuckets", node.liquid);
        tag.putInt("mode", node.mode);
        tag.putLong("transfer_cooldown", node.cooldownUntil);
        tag.putInt("chambers", node.chambers);
        tag.putInt("coolant", node.coolant);
        tag.putInt("hot_coolant", node.hotCoolant);
        if (node.owner != null) tag.putString("trade_owner", node.owner.toString());
        tag.putInt("offer_eu", node.saleSize);
        tag.putInt("trades", node.trades);
        tag.putDouble("credit", node.credit);
        if (node.target != null) {
            tag.putBoolean("linked", true);
            tag.putLong("peer", node.target.asLong());
        }
        tag.putInt("hull_heat", node.reactor.heat());
        tag.putDouble("output", node.reactor.output());
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        var node = (PowerNode) this;
        var stacks =
                tag.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElseGet(java.util.List::of);
        for (int slot = 0;
                slot < Math.min(stacks.size(), node.inventory.getContainerSize());
                slot++) node.inventory.setItem(slot, stacks.get(slot));
        node.energy = Math.max(0, tag.getDoubleOr("eu", 0));
        node.waterFraction = Math.max(0, tag.getDoubleOr("water_fraction", 0));
        node.fuel = Math.max(0, tag.getIntOr("fuel", 0));
        node.liquid = Math.max(0, tag.getIntOr("millibuckets", 0));
        node.mode = Math.max(0, tag.getIntOr("mode", 0));
        node.chambers = Math.max(0, tag.getIntOr("chambers", 0));
        node.coolant =
                Math.min(ReactorVessel.TANK_CAPACITY, Math.max(0, tag.getIntOr("coolant", 0)));
        node.hotCoolant =
                Math.min(ReactorVessel.TANK_CAPACITY, Math.max(0, tag.getIntOr("hot_coolant", 0)));
        node.owner = tag.getString("trade_owner").map(java.util.UUID::fromString).orElse(null);
        node.saleSize = Math.max(1, tag.getIntOr("offer_eu", 1000));
        node.trades = tag.getIntOr("trades", 0);
        node.credit = Math.max(0, tag.getDoubleOr("credit", 0));
        node.target =
                tag.getBooleanOr("linked", false) ? BlockPos.of(tag.getLongOr("peer", 0)) : null;
        node.cooldownUntil = tag.getLongOr("transfer_cooldown", 0);
        node.reactor.restore(
                Math.max(0, tag.getIntOr("hull_heat", 0)),
                Math.max(0, tag.getDoubleOr("output", 0)));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (getLevel() instanceof ServerLevel server)
            Containers.dropContents(server, pos, ((PowerNode) this).inventory);
    }
}
