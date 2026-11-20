package com.industrialage.ia2.compatqa;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;

final class CompatHost {
    private CompatHost() {}

    static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("ic2", path));
    }

    static Block block(String path) {
        return BuiltInRegistries.BLOCK.getValue(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("ic2", path));
    }

    static Entity boat(ServerLevel level) {
        String path = "electric_boat";
        return BuiltInRegistries.ENTITY_TYPE
                .getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("ic2", path))
                .create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
    }

    static ServerPlayer player(ServerLevel level) {
        var profile = new GameProfile(UUID.randomUUID(), "IC2CompatQA");
        var player =
                new ServerPlayer(
                        level.getServer(),
                        level,
                        profile,
                        net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = new FakePlayer(level, profile).connection;
        return player;
    }

    static int water(ServerLevel level, BlockPos pos, int quantity) {
        var handler =
                level.getCapability(
                        net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                        pos,
                        Direction.UP);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            var water =
                    net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                            new FluidStack(Fluids.WATER, quantity));
            int inserted = handler.insert(0, water, quantity, transaction);
            transaction.commit();
            return inserted;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void tick(ServerLevel level, BlockPos pos) {
        var entity = level.getBlockEntity(pos);
        entity.onLoad();
        var state = level.getBlockState(pos);
        BlockEntityTicker ticker =
                ((EntityBlock) state.getBlock()).getTicker(level, state, entity.getType());
        ticker.tick(level, pos, state, entity);
    }
}
