package com.industrialage.ia2.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        var node = (TransportNode) this;
        tag.put("cargo", node.cargo.saveOptional(registries));
        tag.put("fluid", TransportPlatform.fluid(node).saveOptional(registries));
        tag.putInt("closed", node.closed);
        tag.putInt("lv", node.lv);
        tag.putInt("mv", node.mv);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        TransportPlatform.restore(
                (TransportNode) this,
                ItemStack.parseOptional(registries, tag.getCompound("cargo")),
                FluidStack.parseOptional(registries, tag.getCompound("fluid")),
                tag.getInt("closed"),
                tag.getInt("lv"),
                tag.getInt("mv"));
    }
}
