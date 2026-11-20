package com.industrialage.ia2.industrial;

import com.industrialage.ia2.equipment.ArmorEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native IC2 radiation effect, shielding and single-tablet player interaction. */
public final class Radiation {
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, "ic2");
    private static final DeferredHolder<MobEffect, MobEffect> EFFECT =
            EFFECTS.register("radiation", NativeRadiation::new);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ic2");

    private Radiation() {}

    /** Registers the exposure effect and ships the iodine consumable. */
    public static void install(IEventBus bus, IEventBus game) {
        ITEMS.registerSimpleItem("iodine");
        ITEMS.registerItem("iodine_tablet", IodineTablet::new);
        game.addListener(
                PlayerTickEvent.Post.class,
                e -> {
                    if (e.getEntity() instanceof ServerPlayer player)
                        RadiationInventory.tick(player);
                });
        EFFECTS.register(bus);
        ITEMS.register(bus);
    }

    /** Adds measured in-game radiation unless the full hazmat suit protects the target. */
    public static void expose(LivingEntity target, int ticks, int amplifier) {
        if (ticks <= 0 || ArmorEffects.fullHazmat(target)) return;
        var old = target.getEffect(EFFECT);
        if (old == null
                || old.getAmplifier() < amplifier
                || old.getAmplifier() == amplifier && old.getDuration() <= ticks - 10)
            target.addEffect(new MobEffectInstance(EFFECT, ticks, amplifier));
    }

    /** Whether this entity currently has the native radiation effect. */
    public static boolean has(LivingEntity entity) {
        return entity.hasEffect(EFFECT);
    }

    /** Removes the effect and returns whether a treatment was necessary. */
    public static boolean clear(LivingEntity entity) {
        return entity.removeEffect(EFFECT);
    }

    static boolean treat(Player player, ItemStack tablets) {
        var dose = player.getEffect(EFFECT);
        if (dose == null || dose.getDuration() <= 0) return false;
        int remaining = dose.getDuration() - 20;
        clear(player);
        if (remaining > 0)
            player.addEffect(
                    new MobEffectInstance(
                            EFFECT,
                            remaining,
                            dose.getAmplifier(),
                            dose.isAmbient(),
                            dose.isVisible(),
                            dose.showIcon()));
        if (!player.getAbilities().instabuild) tablets.shrink(1);
        return true;
    }
}
