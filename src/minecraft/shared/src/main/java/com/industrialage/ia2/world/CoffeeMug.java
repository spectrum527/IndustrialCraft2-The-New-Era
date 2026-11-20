package com.industrialage.ia2.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A brewed drink gives a short effect and returns the original ceramic mug. */
class CoffeeMug extends Item {
    enum Brew {
        COLD(600, 1, 1),
        DARK(1200, 5, 2),
        MILK(1200, 6, 4);

        final int extraTicks;
        final int limit;
        final int tolerance;

        Brew(int extraTicks, int limit, int tolerance) {
            this.extraTicks = extraTicks;
            this.limit = limit;
            this.tolerance = tolerance;
        }

        static Brew named(String name) {
            return switch (name) {
                case "cold_coffee_mug" -> COLD;
                case "dark_coffee_mug" -> DARK;
                default -> MILK;
            };
        }
    }

    enum Effect {
        SPEED,
        HASTE,
        NAUSEA,
        HARM
    }

    private final Brew brew;

    CoffeeMug(Properties settings, Brew brew) {
        super(settings);
        this.brew = brew;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        var remaining = super.finishUsingItem(stack, level, consumer);
        if (!(level instanceof ServerLevel)) return remaining;
        serve(consumer);
        return consumer instanceof Player player && player.isCreative()
                ? remaining
                : new ItemStack(WorldNative.item("ic2:empty_mug"));
    }

    private void serve(LivingEntity consumer) {
        int additional = boost(consumer, Effect.SPEED) - brew.tolerance;
        boost(consumer, Effect.HASTE);
        if (additional <= 0) return;
        var nausea = WorldNative.coffeeEffect(consumer, Effect.NAUSEA);
        WorldNative.coffeeEffect(
                consumer,
                Effect.NAUSEA,
                addTicks(nausea == null ? 0 : nausea.getDuration(), 200),
                0);
        if (additional > 1) WorldNative.coffeeEffect(consumer, Effect.HARM, 1, additional - 1);
    }

    private int boost(LivingEntity consumer, Effect effect) {
        var existing = WorldNative.coffeeEffect(consumer, effect);
        int current = existing == null ? -1 : existing.getAmplifier();
        int next = current >= brew.limit ? current : current + 1;
        int duration = existing == null ? 300 : addTicks(existing.getDuration(), brew.extraTicks);
        WorldNative.coffeeEffect(consumer, effect, duration, next);
        return next > current ? next : 0;
    }

    private static int addTicks(int duration, int extra) {
        return duration < 0 ? duration : (int) Math.min((long) duration + extra, Integer.MAX_VALUE);
    }
}
