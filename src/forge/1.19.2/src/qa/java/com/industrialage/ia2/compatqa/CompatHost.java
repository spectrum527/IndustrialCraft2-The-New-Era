package com.industrialage.ia2.compatqa;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.registries.ForgeRegistries;

final class CompatHost {
    private CompatHost() {}

    static Item item(String path) {
        return ForgeRegistries.ITEMS.getValue(
                new net.minecraft.resources.ResourceLocation("ic2", path));
    }

    static Block block(String path) {
        return ForgeRegistries.BLOCKS.getValue(
                new net.minecraft.resources.ResourceLocation("ic2", path));
    }

    static Entity boat(ServerLevel level) {
        String path = "electric_boat";
        return ForgeRegistries.ENTITY_TYPES
                .getValue(new net.minecraft.resources.ResourceLocation("ic2", path))
                .create(level);
    }

    static ServerPlayer player(ServerLevel level) {
        var profile = new GameProfile(UUID.randomUUID(), "IC2CompatQA");
        var player = new ServerPlayer(level.getServer(), level, profile, null);
        player.connection = new FakePlayer(level, profile).connection;
        return player;
    }

    static int water(ServerLevel level, BlockPos pos, int quantity) {
        return level.getBlockEntity(pos)
                .getCapability(
                        net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
                        Direction.UP)
                .orElseThrow(AssertionError::new)
                .fill(new FluidStack(Fluids.WATER, quantity), FluidAction.EXECUTE);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void tick(ServerLevel level, BlockPos pos) {
        var entity = level.getBlockEntity(pos);
        var state = level.getBlockState(pos);
        BlockEntityTicker ticker =
                ((EntityBlock) state.getBlock()).getTicker(level, state, entity.getType());
        ticker.tick(level, pos, state, entity);
    }
}
