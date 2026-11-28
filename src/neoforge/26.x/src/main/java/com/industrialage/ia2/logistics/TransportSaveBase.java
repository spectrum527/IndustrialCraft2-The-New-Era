package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

/** Transport persistence and rollback using the 26.x native save and transfer APIs. */
abstract class TransportSaveBase extends BlockEntity {
    final SnapshotJournal<TransferState> journal =
            new SnapshotJournal<>() {
                @Override
                protected TransferState createSnapshot() {
                    var node = (TransportNode) TransportSaveBase.this;
                    return new TransferState(
                            node.cargo.copy(),
                            node.liquid,
                            node.amount,
                            node.receivedItemAt,
                            node.cargoFrom,
                            node.cargoTo,
                            node.externalIntakeFace,
                            node.cargoStalled,
                            node.cargoSeq,
                            node.fluidFlowMask,
                            node.fluidFlow.snapshot(),
                            node.lastTrackedFluid,
                            node.pendingItemVisualSync,
                            node.pendingFluidVisualSync);
                }

                @Override
                protected void revertToSnapshot(TransferState snapshot) {
                    var node = (TransportNode) TransportSaveBase.this;
                    node.cargo = snapshot.cargo();
                    node.liquid = snapshot.fluid();
                    node.amount = snapshot.amount();
                    node.receivedItemAt = snapshot.receivedAt();
                    node.cargoFrom = snapshot.cargoFrom();
                    node.cargoTo = snapshot.cargoTo();
                    node.externalIntakeFace = snapshot.externalIntakeFace();
                    node.cargoStalled = snapshot.cargoStalled();
                    node.cargoSeq = snapshot.cargoSeq();
                    node.fluidFlowMask = snapshot.fluidFlowMask();
                    node.fluidFlow.restore(snapshot.fluidFlowSnapshot());
                    node.lastTrackedFluid = snapshot.lastTrackedFluid();
                    node.pendingItemVisualSync = snapshot.pendingItemVisualSync();
                    node.pendingFluidVisualSync = snapshot.pendingFluidVisualSync();
                }

                @Override
                protected void onRootCommit(TransferState originalState) {
                    ((TransportNode) TransportSaveBase.this).flushCommittedVisualSync();
                }
            };

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
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        if (node.spec.kind() == Kind.ITEM_PIPE && !node.cargo.isEmpty()) {
            ItemStack.OPTIONAL_CODEC
                    .encodeStart(ops, node.cargo)
                    .result()
                    .ifPresent(encoded -> tag.put("cargo", encoded));
            tag.putInt("cargo_from", encodeDirection(node.cargoFrom));
            tag.putInt("cargo_to", encodeDirection(node.cargoTo));
            tag.putInt("cargo_seq", node.cargoSeq);
            tag.putBoolean("cargo_stalled", node.cargoStalled);
        } else if (node.spec.kind() == Kind.FLUID_PIPE && node.liquid != null && node.amount > 0) {
            FluidStack.OPTIONAL_CODEC
                    .encodeStart(ops, TransportPlatform.fluid(node))
                    .result()
                    .ifPresent(encoded -> tag.put("fluid", encoded));
            tag.putInt("flow_mask", node.fluidFlowMask);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        var node = (TransportNode) this;
        var syncedCargo = input.read("cargo", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        var syncedFluid = input.read("fluid", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
        node.applyClientSync(
                input.getIntOr("nozzle_extraction", 0),
                input.getIntOr("nozzle_injection", 0),
                syncedCargo,
                decodeDirection(input.getIntOr("cargo_from", -1)),
                decodeDirection(input.getIntOr("cargo_to", -1)),
                input.getIntOr("cargo_seq", 0),
                input.getBooleanOr("cargo_stalled", false),
                syncedFluid.isEmpty() ? null : new FluidKey(syncedFluid),
                syncedFluid.getAmount(),
                input.getIntOr("flow_mask", 0) & PipeVisuals.FLOW_MASK_ALL);
    }

    private static int encodeDirection(Direction direction) {
        return direction == null ? -1 : direction.ordinal();
    }

    private static Direction decodeDirection(int ordinal) {
        var values = TransportNode.SIDES;
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        handleUpdateTag(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var node = (TransportNode) this;
        for (int i = 0; i < node.upgrades.getContainerSize(); i++)
            output.store("upgrade_" + i, ItemStack.OPTIONAL_CODEC, node.upgrades.getItem(i));
        output.store("cargo", ItemStack.OPTIONAL_CODEC, node.cargo);
        output.store("fluid", FluidStack.OPTIONAL_CODEC, TransportPlatform.fluid(node));
        output.putInt("closed", node.closed);
        output.putInt("lv", node.lv);
        output.putInt("mv", node.mv);
        if (node.hasNozzles()) {
            output.putInt("nozzle_extraction", node.extractionNozzles);
            output.putInt("nozzle_injection", node.injectionNozzles);
        }
        if (node.spec.kind() == Kind.ITEM_PIPE && node.externalIntakeFace != null) {
            output.putInt("external_intake", encodeDirection(node.externalIntakeFace));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        var tank = (TransportNode) this;
        for (int i = 0; i < tank.upgrades.getContainerSize(); i++)
            tank.upgrades.setItem(
                    i,
                    input.read("upgrade_" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        TransportPlatform.restore(
                (TransportNode) this,
                input.read("cargo", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY),
                input.read("fluid", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY),
                input.getIntOr("closed", 0),
                input.getIntOr("lv", 0),
                input.getIntOr("mv", 0));
        tank.restoreNozzles(
                input.getIntOr("nozzle_extraction", 0), input.getIntOr("nozzle_injection", 0));
        tank.externalIntakeFace =
                tank.spec.kind() == Kind.ITEM_PIPE
                        ? decodeDirection(input.getIntOr("external_intake", -1))
                        : null;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (getLevel() instanceof ServerLevel level) {
            ((TransportNode) this).dropContents(level);
        }
    }

    record TransferState(
            ItemStack cargo,
            FluidKey fluid,
            int amount,
            long receivedAt,
            Direction cargoFrom,
            Direction cargoTo,
            Direction externalIntakeFace,
            boolean cargoStalled,
            int cargoSeq,
            int fluidFlowMask,
            FluidFlowTracker.Snapshot fluidFlowSnapshot,
            FluidKey lastTrackedFluid,
            boolean pendingItemVisualSync,
            boolean pendingFluidVisualSync) {}
}
