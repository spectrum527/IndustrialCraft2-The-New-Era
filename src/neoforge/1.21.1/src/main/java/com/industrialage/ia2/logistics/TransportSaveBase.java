package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/** Transport persistence using the 1.21.1 native registry-aware NBT API. */
abstract class TransportSaveBase extends BlockEntity {
    TransportSaveBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ((TransportNode) this).spec.kind() == Kind.TANK
                ? null
                : ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        var node = (TransportNode) this;
        tag.putInt("nozzle_extraction", node.extractionNozzles);
        tag.putInt("nozzle_injection", node.injectionNozzles);
        if (node.spec.kind() == Kind.ITEM_PIPE && !node.cargo.isEmpty()) {
            tag.put("cargo", node.cargo.saveOptional(registries));
            tag.putByte("cargo_from", encodeDirection(node.cargoFrom));
            tag.putByte("cargo_to", encodeDirection(node.cargoTo));
            tag.putInt("cargo_seq", node.cargoSeq);
            tag.putBoolean("cargo_stalled", node.cargoStalled);
        } else if (node.spec.kind() == Kind.FLUID_PIPE && node.liquid != null && node.amount > 0) {
            tag.put("fluid", TransportPlatform.fluid(node).saveOptional(registries));
            tag.putByte("flow_dir", encodeDirection(node.fluidFlowDir));
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        var node = (TransportNode) this;
        var syncedCargo =
                tag.contains("cargo")
                        ? ItemStack.parseOptional(registries, tag.getCompound("cargo"))
                        : ItemStack.EMPTY;
        var syncedFluid =
                tag.contains("fluid")
                        ? FluidStack.parseOptional(registries, tag.getCompound("fluid"))
                        : FluidStack.EMPTY;
        node.applyClientSync(
                tag.getInt("nozzle_extraction"),
                tag.getInt("nozzle_injection"),
                syncedCargo,
                decodeDirection(tag.contains("cargo_from") ? tag.getByte("cargo_from") : -1),
                decodeDirection(tag.contains("cargo_to") ? tag.getByte("cargo_to") : -1),
                tag.getInt("cargo_seq"),
                tag.getBoolean("cargo_stalled"),
                syncedFluid.isEmpty() ? null : new FluidKey(syncedFluid),
                syncedFluid.getAmount(),
                decodeDirection(tag.contains("flow_dir") ? tag.getByte("flow_dir") : -1));
    }

    private static byte encodeDirection(Direction direction) {
        return direction == null ? (byte) -1 : (byte) direction.ordinal();
    }

    private static Direction decodeDirection(int ordinal) {
        var values = TransportNode.SIDES;
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag(), registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        var node = (TransportNode) this;
        for (int i = 0; i < node.upgrades.getContainerSize(); i++)
            tag.put("upgrade_" + i, node.upgrades.getItem(i).saveOptional(registries));
        tag.put("cargo", node.cargo.saveOptional(registries));
        tag.put("fluid", TransportPlatform.fluid(node).saveOptional(registries));
        tag.putInt("closed", node.closed);
        tag.putInt("lv", node.lv);
        tag.putInt("mv", node.mv);
        if (node.hasNozzles()) {
            tag.putInt("nozzle_extraction", node.extractionNozzles);
            tag.putInt("nozzle_injection", node.injectionNozzles);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        var tank = (TransportNode) this;
        for (int i = 0; i < tank.upgrades.getContainerSize(); i++)
            tank.upgrades.setItem(
                    i, ItemStack.parseOptional(registries, tag.getCompound("upgrade_" + i)));
        TransportPlatform.restore(
                (TransportNode) this,
                ItemStack.parseOptional(registries, tag.getCompound("cargo")),
                FluidStack.parseOptional(registries, tag.getCompound("fluid")),
                tag.getInt("closed"),
                tag.getInt("lv"),
                tag.getInt("mv"));
        tank.restoreNozzles(tag.getInt("nozzle_extraction"), tag.getInt("nozzle_injection"));
    }
}
