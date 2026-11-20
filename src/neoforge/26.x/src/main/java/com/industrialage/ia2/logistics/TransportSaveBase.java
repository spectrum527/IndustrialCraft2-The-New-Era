package com.industrialage.ia2.logistics;

import net.minecraft.core.BlockPos;
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
                            node.cargo.copy(), node.liquid, node.amount, node.receivedItemAt);
                }

                @Override
                protected void revertToSnapshot(TransferState snapshot) {
                    var node = (TransportNode) TransportSaveBase.this;
                    node.cargo = snapshot.cargo();
                    node.liquid = snapshot.fluid();
                    node.amount = snapshot.amount();
                    node.receivedItemAt = snapshot.receivedAt();
                }
            };

    TransportSaveBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var node = (TransportNode) this;
        output.store("cargo", ItemStack.OPTIONAL_CODEC, node.cargo);
        output.store("fluid", FluidStack.OPTIONAL_CODEC, TransportPlatform.fluid(node));
        output.putInt("closed", node.closed);
        output.putInt("lv", node.lv);
        output.putInt("mv", node.mv);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        TransportPlatform.restore(
                (TransportNode) this,
                input.read("cargo", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY),
                input.read("fluid", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY),
                input.getIntOr("closed", 0),
                input.getIntOr("lv", 0),
                input.getIntOr("mv", 0));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (getLevel() instanceof ServerLevel level) {
            ((TransportNode) this).dropContents(level);
        }
    }

    record TransferState(ItemStack cargo, FluidKey fluid, int amount, long receivedAt) {}
}
