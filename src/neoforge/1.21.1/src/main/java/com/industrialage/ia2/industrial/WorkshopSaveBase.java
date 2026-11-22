package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

abstract class WorkshopSaveBase extends BlockEntity {
    WorkshopSaveBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ((WorkshopNode) this).kind.hasWorldRotor()
                ? ClientboundBlockEntityDataPacket.create(this)
                : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var node = (WorkshopNode) this;
        if (!node.kind.hasWorldRotor()) return super.getUpdateTag(registries);
        var tag = new CompoundTag();
        tag.putInt("rotor", node.visualRotorType());
        tag.putInt("speed", node.visualRotorSpeed());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        var node = (WorkshopNode) this;
        if (node.kind.hasWorldRotor())
            node.acceptRotorVisual(tag.getInt("rotor"), tag.getInt("speed"));
        else super.handleUpdateTag(tag, registries);
    }

    @Override
    public void onDataPacket(
            net.minecraft.network.Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        var node = (WorkshopNode) this;
        if (node.kind.hasWorldRotor()) {
            var tag = packet.getTag();
            if (tag != null) node.acceptRotorVisual(tag.getInt("rotor"), tag.getInt("speed"));
        } else super.onDataPacket(connection, packet, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        var node = (WorkshopNode) this;
        var items = NonNullList.withSize(WorkshopInventory.SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, node.inventory.getItem(slot));
        }
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putDouble("ic2_energy", node.energy);
        tag.putInt("ic2_heat", node.heat);
        tag.putInt("ic2_heat_fuel", node.heatFuelTicks);
        if (node.inventory.isFuelProduct(node.inventory.getItem(0)))
            tag.put("ic2_fuel_return", node.completedFuel.saveOptional(registries));
        tag.putBoolean("ic2_coal_ash", node.solidProducesAsh);
        tag.putInt("ic2_progress", node.progress);
        tag.putInt("ic2_work_duration", node.cycleDuration);
        tag.putInt("ic2_mode", node.mode);
        tag.putInt("ic2_cycle_count", node.cycles);
        tag.putInt("ic2_fractional_matter", node.fractionalMatter);
        tag.putInt("ic2_amplifier", node.amplifier);
        tag.putString("ic2_recipe", node.lastRecipe);
        saveTank(tag, "ic2_input_fluid", node.inlet);
        if (node.outlet != node.inlet) {
            saveTank(tag, "ic2_output_fluid", node.outlet);
        }
        saveTank(tag, "ic2_byproduct_fluid", node.byproduct);
    }

    private static void saveTank(CompoundTag tag, String field, WorkshopNode.Tank tank) {
        if (tank.amount > 0) {
            var saved = new CompoundTag();
            saved.putString("FluidName", tank.id);
            saved.putInt("Amount", tank.amount);
            tag.put(field, saved);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        var node = (WorkshopNode) this;
        var items = NonNullList.withSize(WorkshopInventory.SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        for (int slot = 0; slot < items.size(); slot++) {
            node.inventory.setItem(slot, items.get(slot));
        }
        double stored = tag.getDouble("ic2_energy");
        node.energy = Double.isFinite(stored) ? Math.max(0, stored) : 0;
        node.heat = Math.max(0, tag.getInt("ic2_heat"));
        node.heatFuelTicks = Math.max(0, tag.getInt("ic2_heat_fuel"));
        node.completedFuel =
                ItemStack.parseOptional(registries, tag.getCompound("ic2_fuel_return"));
        if (node.completedFuel.isEmpty()
                && node.kind == WorkshopKind.SOLID_HEAT
                && node.inventory.getItem(0).is(net.minecraft.world.item.Items.BUCKET))
            node.completedFuel = node.inventory.getItem(0).copy();
        node.solidProducesAsh = tag.getBoolean("ic2_coal_ash");
        node.progress = Math.max(0, tag.getInt("ic2_progress"));
        node.mode = Math.floorMod(tag.getInt("ic2_mode"), node.kind.modes());
        node.cycles = Math.max(0, tag.getInt("ic2_cycle_count"));
        node.amplifier = Math.max(0, tag.getInt("ic2_amplifier"));
        node.fractionalMatter = Math.max(0, tag.getInt("ic2_fractional_matter"));
        node.cycleDuration = Math.max(0, tag.getInt("ic2_work_duration"));
        node.lastRecipe = tag.getString("ic2_recipe");
        loadTank(tag, "ic2_input_fluid", node.inlet);
        if (node.outlet != node.inlet) {
            loadTank(tag, "ic2_output_fluid", node.outlet);
        }
        loadTank(tag, "ic2_byproduct_fluid", node.byproduct);
    }

    private static void loadTank(CompoundTag root, String field, WorkshopNode.Tank tank) {
        var saved = root.getCompound(field);
        tank.restore(saved.getString("FluidName"), saved.getInt("Amount"));
    }
}
