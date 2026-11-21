package com.industrialage.ia2.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** A whole fluid source hit by a cell's use ray. */
record CellSource(BlockPos pos, Direction face, BucketPickup pickup, Fluid fluid) {
    static CellSource find(Level level, BlockHitResult hit) {
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        var pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        var fluid = state.getFluidState();
        if (!fluid.isSource() || !(state.getBlock() instanceof BucketPickup pickup)) return null;
        return new CellSource(pos, hit.getDirection(), pickup, fluid.getType());
    }

    boolean mayCollect(Level level, Player player, ItemStack cell) {
        return level.mayInteract(player, pos) && player.mayUseItemAt(pos, face, cell);
    }
}
