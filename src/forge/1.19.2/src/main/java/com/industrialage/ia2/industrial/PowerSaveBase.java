package com.industrialage.ia2.industrial;

import com.industrialage.ia2.access.MachinePortCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

/** Native save adapter for electrical devices and reactor components. */
abstract class PowerSaveBase extends BlockEntity {
    PowerSaveBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        var node = (PowerNode) this;
        var stacks = NonNullList.withSize(node.inventory.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < stacks.size(); slot++)
            stacks.set(slot, node.inventory.getItem(slot));
        ContainerHelper.saveAllItems(tag, stacks);
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
    public void load(CompoundTag tag) {
        super.load(tag);
        var node = (PowerNode) this;
        var stacks = NonNullList.withSize(node.inventory.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks);
        for (int slot = 0; slot < stacks.size(); slot++)
            node.inventory.setItem(slot, stacks.get(slot));
        node.energy = nonNegative(tag.getDouble("eu"));
        node.waterFraction = nonNegative(tag.getDouble("water_fraction"));
        node.fuel = Math.max(0, tag.getInt("fuel"));
        node.liquid = Math.max(0, tag.getInt("millibuckets"));
        node.mode = Math.max(0, tag.getInt("mode"));
        node.chambers = Math.max(0, tag.getInt("chambers"));
        node.coolant = Math.min(ReactorVessel.TANK_CAPACITY, Math.max(0, tag.getInt("coolant")));
        node.hotCoolant =
                Math.min(ReactorVessel.TANK_CAPACITY, Math.max(0, tag.getInt("hot_coolant")));
        node.owner = parseUuid(tag.getString("trade_owner"));
        node.saleSize = tag.contains("offer_eu") ? Math.max(1, tag.getInt("offer_eu")) : 1000;
        node.trades = tag.getInt("trades");
        node.credit = nonNegative(tag.getDouble("credit"));
        node.target = tag.getBoolean("linked") ? BlockPos.of(tag.getLong("peer")) : null;
        node.cooldownUntil = tag.getLong("transfer_cooldown");
        node.reactor.restore(
                Math.max(0, tag.getInt("hull_heat")), nonNegative(tag.getDouble("output")));
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) && value > 0 ? value : 0;
    }

    private static java.util.UUID parseUuid(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return java.util.UUID.fromString(raw);
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private final MachinePortCache automation = new MachinePortCache(this);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!isRemoved()) {
            var node = (PowerNode) this;
            if (cap == ForgeCapabilities.ITEM_HANDLER)
                return automation
                        .item(
                                side,
                                () -> new PowerItemPort(node, side == null ? Direction.UP : side))
                        .cast();
            if (cap == ForgeCapabilities.FLUID_HANDLER
                    && (node.kind == PowerKind.GEO_GENERATOR
                            || node.kind == PowerKind.REACTOR_FLUID_PORT))
                return automation.fluid(side, () -> new LavaInlet(node)).cast();
        }
        return super.getCapability(cap, side);
    }

    /** Refreshes listeners for item and fluid automation on this device. */
    public void invalidateMachinePorts() {
        automation.invalidate();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        invalidateMachinePorts();
    }
}
