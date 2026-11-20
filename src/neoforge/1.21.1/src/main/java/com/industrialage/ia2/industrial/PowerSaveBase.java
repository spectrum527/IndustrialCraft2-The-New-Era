package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Native save adapter for electrical devices and reactor components. */
abstract class PowerSaveBase extends BlockEntity {
    PowerSaveBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        var node = (PowerNode) this;
        var stacks = NonNullList.withSize(node.inventory.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < stacks.size(); slot++)
            stacks.set(slot, node.inventory.getItem(slot));
        ContainerHelper.saveAllItems(tag, stacks, registries);
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
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        var node = (PowerNode) this;
        var stacks = NonNullList.withSize(node.inventory.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks, registries);
        for (int slot = 0; slot < stacks.size(); slot++)
            node.inventory.setItem(slot, stacks.get(slot));
        node.energy = Math.max(0, tag.getDouble("eu"));
        node.waterFraction = Math.max(0, tag.getDouble("water_fraction"));
        node.fuel = Math.max(0, tag.getInt("fuel"));
        node.liquid = Math.max(0, tag.getInt("millibuckets"));
        node.mode = Math.max(0, tag.getInt("mode"));
        node.chambers = Math.max(0, tag.getInt("chambers"));
        node.coolant = Math.min(ReactorVessel.TANK_CAPACITY, Math.max(0, tag.getInt("coolant")));
        node.hotCoolant =
                Math.min(ReactorVessel.TANK_CAPACITY, Math.max(0, tag.getInt("hot_coolant")));
        String id = tag.getString("trade_owner");
        node.owner = id.isEmpty() ? null : java.util.UUID.fromString(id);
        node.saleSize = tag.contains("offer_eu") ? Math.max(1, tag.getInt("offer_eu")) : 1000;
        node.trades = tag.getInt("trades");
        node.credit = Math.max(0, tag.getDouble("credit"));
        node.target = tag.getBoolean("linked") ? BlockPos.of(tag.getLong("peer")) : null;
        node.cooldownUntil = tag.getLong("transfer_cooldown");
        node.reactor.restore(
                Math.max(0, tag.getInt("hull_heat")), Math.max(0, tag.getDouble("output")));
    }
}
