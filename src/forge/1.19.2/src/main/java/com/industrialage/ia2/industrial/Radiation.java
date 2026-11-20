package com.industrialage.ia2.industrial;

import com.industrialage.ia2.equipment.ArmorEffects;
import com.industrialage.ia2.forge.content.ForgeContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Native IC2 radiation effect, shielding and single-tablet player interaction. */
public final class Radiation {
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "ic2");
    private static final RegistryObject<MobEffect> EFFECT =
            EFFECTS.register("radiation", NativeRadiation::new);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "ic2");

    private Radiation() {}

    /** Registers the exposure effect and ships the iodine consumable. */
    public static void install(IEventBus bus, IEventBus game) {
        ITEMS.register("iodine", () -> new Item(ForgeContent.itemProperties()));
        ITEMS.register("iodine_tablet", () -> new IodineTablet(ForgeContent.itemProperties()));
        game.addListener(
                (TickEvent.PlayerTickEvent e) -> {
                    if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer player)
                        RadiationInventory.tick(player);
                });
        EFFECTS.register(bus);
        ITEMS.register(bus);
    }

    /** Adds measured in-game radiation unless the full hazmat suit protects the target. */
    public static void expose(LivingEntity target, int ticks, int amplifier) {
        if (ticks <= 0 || ArmorEffects.fullHazmat(target)) return;
        var old = target.getEffect(EFFECT.get());
        if (old == null
                || old.getAmplifier() < amplifier
                || old.getAmplifier() == amplifier && old.getDuration() <= ticks - 10)
            target.addEffect(new MobEffectInstance(EFFECT.get(), ticks, amplifier));
    }

    /** Whether this entity currently has the native radiation effect. */
    public static boolean has(LivingEntity entity) {
        return entity.hasEffect(EFFECT.get());
    }

    /** Removes the effect and returns whether a treatment was necessary. */
    public static boolean clear(LivingEntity entity) {
        return entity.removeEffect(EFFECT.get());
    }

    static boolean treat(Player player, ItemStack tablets) {
        var dose = player.getEffect(EFFECT.get());
        if (dose == null || dose.getDuration() <= 0) return false;
        int remaining = dose.getDuration() - 20;
        clear(player);
        if (remaining > 0)
            player.addEffect(
                    new MobEffectInstance(
                            EFFECT.get(),
                            remaining,
                            dose.getAmplifier(),
                            dose.isAmbient(),
                            dose.isVisible(),
                            dose.showIcon()));
        if (!player.getAbilities().instabuild) tablets.shrink(1);
        return true;
    }
}
