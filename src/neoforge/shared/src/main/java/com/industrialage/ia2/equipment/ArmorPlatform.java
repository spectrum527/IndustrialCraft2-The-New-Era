package com.industrialage.ia2.equipment;

import com.industrialage.ia2.gameplay.PortableGear;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the native wearable items and server protection hooks. */
public final class ArmorPlatform {
    private ArmorPlatform() {}

    /** Installs the item and damage bindings for the current loader. */
    public static void install(DeferredRegister.Items items, IEventBus modBus, IEventBus gameBus) {
        NativeArmor.register(items, modBus);
        gameBus.addListener(
                PlayerTickEvent.Post.class,
                event -> {
                    if (event.getEntity() instanceof ServerPlayer player)
                        ArmorEffects.tick(player, (ServerLevel) player.level());
                });
        gameBus.addListener(
                LivingFallEvent.class,
                event -> {
                    if (event.getEntity() instanceof ServerPlayer player
                            && ArmorEffects.absorbFall(player, event.getDistance())) {
                        event.setCanceled(true);
                    }
                });
        gameBus.addListener(
                ItemTooltipEvent.class,
                event ->
                        ArmorEffects.appendChargeTooltip(event.getItemStack(), event.getToolTip()));
        gameBus.addListener(LivingIncomingDamageEvent.class, ArmorPlatform::protect);
        gameBus.addListener(ItemAttributeModifierEvent.class, ArmorPlatform::protectiveAttributes);
    }

    private static void protectiveAttributes(ItemAttributeModifierEvent event) {
        var defense = ArmorEffects.protection(event.getItemStack());
        if (defense == null) return;
        var group = EquipmentSlotGroup.bySlot(defense.slot());
        String id = "powered_armor_" + defense.slot().getName();
        if (defense.armor() > 0)
            event.addModifier(
                    Attributes.ARMOR, NativeArmor.protectionModifier(id, defense.armor()), group);
        else
            event.addModifier(
                    Attributes.ARMOR_TOUGHNESS,
                    NativeArmor.protectionModifier(id, defense.toughness()),
                    group);
    }

    private static void protect(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) return;
        var source = event.getSource();
        if ((source.is(DamageTypeTags.IS_FIRE)
                        || source.is(DamageTypes.HOT_FLOOR)
                        || source.is(DamageTypes.LIGHTNING_BOLT))
                && ArmorEffects.fullHazmat(player)) {
            player.clearFire();
            event.setCanceled(true);
        } else if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            ArmorEffects.absorbImpact(player, event.getAmount());
        }
    }

    static boolean grounded(ServerPlayer player) {
        return player.onGround();
    }

    static boolean sunlight(ServerLevel level, ServerPlayer player) {
        var head = player.blockPosition().above();
        return level.getSkyDarken() < 4 && !level.isRaining() && level.canSeeSky(head);
    }

    static boolean dark(ServerLevel level, BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos) <= 7;
    }

    static void wearBoots(ServerPlayer player, ItemStack boots, int wear) {
        boots.hurtAndBreak(wear, player, EquipmentSlot.FEET);
    }

    static boolean spend(ItemStack item, double amount) {
        return PortableGear.spend(item, amount);
    }

    static double energy(ItemStack item) {
        return PortableGear.stored(item);
    }

    static void restoreEnergy(ItemStack item, double amount) {
        PortableGear.restoreEnergy(item, amount);
    }

    static void charge(ItemStack item, double amount) {
        PortableGear.charge(item, amount, Integer.MAX_VALUE);
    }

    static void sprint(ServerPlayer player, boolean boosted) {
        NativeArmor.sprint(player, boosted);
    }

    static void feed(ServerPlayer player, ItemStack helmet) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var can = inventory.getItem(slot);
            if (!NativeArmor.isFilledCan(can)) continue;
            if (!spend(helmet, 1_000)) return;
            can.shrink(1);
            player.getFoodData().eat(1, 0.6F);
            var tin = NativeArmor.emptyCan();
            if (!inventory.add(tin)) player.drop(tin, false);
            return;
        }
    }
}
