package com.industrialage.ia2.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.util.FakePlayer;

final class NativeArmorCheckAccess {
    private NativeArmorCheckAccess() {}

    static ServerPlayer player(ServerLevel level) {
        return new FakePlayer(level, new GameProfile(UUID.randomUUID(), "IC2ArmorQA")) {
            @Override
            public boolean isInvulnerableTo(ServerLevel world, DamageSource source) {
                return false;
            }
        };
    }

    static void fire(ServerLevel level, ServerPlayer player) {
        player.hurtServer(level, player.damageSources().inFire(), 2);
    }

    static void strike(ServerLevel level, ServerPlayer player, float amount) {
        player.hurtServer(level, player.damageSources().cactus(), amount);
    }

    static ItemStack craft(ServerLevel level, CraftingInput input) {
        return level.getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level)
                .orElseThrow()
                .value()
                .assemble(input);
    }
}
