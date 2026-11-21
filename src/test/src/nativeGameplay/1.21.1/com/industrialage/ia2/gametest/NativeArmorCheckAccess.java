package com.industrialage.ia2.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

final class NativeArmorCheckAccess {
    private NativeArmorCheckAccess() {}

    static ServerPlayer player(ServerLevel level) {
        var profile = new GameProfile(UUID.randomUUID(), "IC2ArmorQA");
        var player =
                new ServerPlayer(
                        level.getServer(), level, profile, ClientInformation.createDefault());
        player.connection = new FakePlayer(level, profile).connection;
        player.setPos(20.5, 128, 21.5);
        for (int tick = 0; tick <= 60; tick++) player.tick();
        return player;
    }

    static void fire(ServerLevel level, ServerPlayer player) {
        player.hurt(player.damageSources().inFire(), 2);
    }

    static void strike(ServerLevel level, ServerPlayer player, float amount) {
        player.hurt(player.damageSources().cactus(), amount);
    }

    static void impact(ServerLevel level, ServerPlayer player, String kind, float amount) {
        var source =
                switch (kind) {
                    case "melee" ->
                            player.damageSources()
                                    .mobAttack(
                                            new net.minecraft.world.entity.monster.Zombie(level));
                    case "fire" -> player.damageSources().inFire();
                    case "fall" -> player.damageSources().fall();
                    case "magic" -> player.damageSources().magic();
                    case "void" -> player.damageSources().fellOutOfWorld();
                    default -> player.damageSources().cactus();
                };
        player.hurt(source, amount);
    }

    static void kill(ServerLevel level, ServerPlayer player) {
        player.kill();
    }

    static float miningSpeed(ServerPlayer player) {
        return player.getDigSpeed(Blocks.STONE.defaultBlockState(), player.blockPosition());
    }

    static ItemStack craft(ServerLevel level, CraftingInput input) {
        return level.getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level)
                .orElseThrow()
                .value()
                .assemble(input, level.registryAccess());
    }
}
