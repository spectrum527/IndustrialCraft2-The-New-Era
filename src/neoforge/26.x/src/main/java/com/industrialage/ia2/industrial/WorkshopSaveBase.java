package com.industrialage.ia2.industrial;

import com.industrialage.ia2.industrial.WorkshopPlans.Liquid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

abstract class WorkshopSaveBase extends BlockEntity {
    record Tanks(Liquid inlet, Liquid outlet, Liquid byproduct) {}

    final SnapshotJournal<Tanks> fluidJournal =
            new SnapshotJournal<>() {
                @Override
                protected Tanks createSnapshot() {
                    var node = (WorkshopNode) WorkshopSaveBase.this;
                    return new Tanks(
                            new Liquid(node.inlet.id, node.inlet.amount),
                            new Liquid(node.outlet.id, node.outlet.amount),
                            new Liquid(node.byproduct.id, node.byproduct.amount));
                }

                @Override
                protected void revertToSnapshot(Tanks snapshot) {
                    var node = (WorkshopNode) WorkshopSaveBase.this;
                    node.inlet.restore(snapshot.inlet.id(), snapshot.inlet.amount());
                    node.outlet.restore(snapshot.outlet.id(), snapshot.outlet.amount());
                    node.byproduct.restore(snapshot.byproduct.id(), snapshot.byproduct.amount());
                }
            };

    WorkshopSaveBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var node = (WorkshopNode) this;
        var inventory = new ArrayList<ItemStack>(WorkshopInventory.SIZE);
        for (int index = 0; index < WorkshopInventory.SIZE; index++) {
            inventory.add(node.inventory.getItem(index));
        }
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), inventory);
        output.putDouble("ic2_energy", node.energy);
        output.putInt("ic2_heat", node.heat);
        output.putInt("ic2_heat_fuel", node.heatFuelTicks);
        output.putBoolean("ic2_coal_ash", node.solidProducesAsh);
        output.putInt("ic2_progress", node.progress);
        output.putInt("ic2_work_duration", node.cycleDuration);
        output.putInt("ic2_mode", node.mode);
        output.putInt("ic2_cycle_count", node.cycles);
        output.putInt("ic2_fractional_matter", node.fractionalMatter);
        output.putInt("ic2_amplifier", node.amplifier);
        output.putString("ic2_recipe", node.lastRecipe);
        output.putString("ic2_input_fluid", node.inlet.id);
        output.putInt("ic2_input_amount", node.inlet.amount);
        if (node.outlet != node.inlet) {
            output.putString("ic2_output_fluid", node.outlet.id);
            output.putInt("ic2_output_amount", node.outlet.amount);
        }
        output.putString("ic2_byproduct_fluid", node.byproduct.id);
        output.putInt("ic2_byproduct_amount", node.byproduct.amount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        var node = (WorkshopNode) this;
        var inventory = input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int index = 0; index < WorkshopInventory.SIZE; index++) {
            node.inventory.setItem(
                    index, index < inventory.size() ? inventory.get(index) : ItemStack.EMPTY);
        }
        double stored = input.getDoubleOr("ic2_energy", 0);
        node.energy = Double.isFinite(stored) ? Math.max(0, stored) : 0;
        node.heat = Math.max(0, input.getIntOr("ic2_heat", 0));
        node.heatFuelTicks = Math.max(0, input.getIntOr("ic2_heat_fuel", 0));
        node.solidProducesAsh = input.getBooleanOr("ic2_coal_ash", false);
        node.progress = Math.max(0, input.getIntOr("ic2_progress", 0));
        node.mode = Math.floorMod(input.getIntOr("ic2_mode", 0), node.kind.modes());
        node.cycles = Math.max(0, input.getIntOr("ic2_cycle_count", 0));
        node.amplifier = Math.max(0, input.getIntOr("ic2_amplifier", 0));
        node.fractionalMatter = Math.max(0, input.getIntOr("ic2_fractional_matter", 0));
        node.cycleDuration = Math.max(0, input.getIntOr("ic2_work_duration", 0));
        node.lastRecipe = input.getStringOr("ic2_recipe", "");
        node.inlet.restore(
                input.getStringOr("ic2_input_fluid", ""), input.getIntOr("ic2_input_amount", 0));
        if (node.outlet != node.inlet) {
            node.outlet.restore(
                    input.getStringOr("ic2_output_fluid", ""),
                    input.getIntOr("ic2_output_amount", 0));
        }
        node.byproduct.restore(
                input.getStringOr("ic2_byproduct_fluid", ""),
                input.getIntOr("ic2_byproduct_amount", 0));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (getLevel() instanceof ServerLevel level) {
            Containers.dropContents(level, pos, ((WorkshopNode) this).inventory);
        }
    }
}
