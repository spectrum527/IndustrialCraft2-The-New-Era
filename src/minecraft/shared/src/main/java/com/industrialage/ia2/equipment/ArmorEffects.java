package com.industrialage.ia2.equipment;

import com.industrialage.ia2.content.PortableEquipment;
import com.industrialage.ia2.industrial.Radiation;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Server gameplay for worn industrial equipment shared by the supported loaders. */
public final class ArmorEffects {
    /** Native slot, material and physical durability of an industrial wearable. */
    public record Piece(
            String path,
            String material,
            EquipmentSlot slot,
            int durability,
            PortableEquipment electric) {}

    /** Supplies the held stack's exact equipment identity across item API generations. */
    public interface Wearable {
        /** {@return the definition associated with this native item} */
        Piece piece();
    }

    /** Reads the portable EU definition from an electric armor stack. */
    public static PortableEquipment electric(ItemStack stack) {
        return stack.getItem() instanceof Wearable item ? item.piece().electric() : null;
    }

    static void appendChargeTooltip(ItemStack stack, List<Component> tooltip) {
        var rating = electric(stack);
        if (rating != null) {
            tooltip.add(
                    Component.translatable(
                            "item.ic2.armor.charge",
                            (long) ArmorPlatform.energy(stack),
                            rating.capacity,
                            rating.tier));
        }
    }

    private static final EquipmentSlot HEAD = EquipmentSlot.HEAD;
    private static final EquipmentSlot CHEST = EquipmentSlot.CHEST;
    private static final EquipmentSlot LEGS = EquipmentSlot.LEGS;
    private static final EquipmentSlot FEET = EquipmentSlot.FEET;

    /** Wearable items instantiated by the platform using native armor or equippable items. */
    public static final List<Piece> PIECES =
            List.of(
                    new Piece("rubber_boots", "hazmat", FEET, 64, null),
                    new Piece("hazmat_helmet", "hazmat", HEAD, 64, null),
                    new Piece("hazmat_chestplate", "hazmat", CHEST, 64, null),
                    new Piece("hazmat_leggings", "hazmat", LEGS, 64, null),
                    new Piece("solar_helmet", "solar_helmet", HEAD, 0, null),
                    new Piece("static_boots", "static_boots", FEET, 0, null),
                    new Piece("alloy_chestplate", "alloy", CHEST, 800, null),
                    new Piece(
                            "night_vision_goggles",
                            "night_vision",
                            HEAD,
                            27,
                            PortableEquipment.NIGHT_VISION_GOGGLES),
                    powered(PortableEquipment.NANO_HELMET, "nano", HEAD),
                    powered(PortableEquipment.NANO_CHESTPLATE, "nano", CHEST),
                    powered(PortableEquipment.NANO_LEGGINGS, "nano", LEGS),
                    powered(PortableEquipment.NANO_BOOTS, "nano", FEET),
                    powered(PortableEquipment.QUANTUM_HELMET, "quantum", HEAD),
                    powered(PortableEquipment.QUANTUM_CHESTPLATE, "quantum", CHEST),
                    powered(PortableEquipment.QUANTUM_LEGGINGS, "quantum", LEGS),
                    powered(PortableEquipment.QUANTUM_BOOTS, "quantum", FEET));

    private static final Map<ServerPlayer, Walk> WALKS = new WeakHashMap<>();
    private static final List<EquipmentSlot> ARMOR = List.of(HEAD, CHEST, LEGS, FEET);

    private ArmorEffects() {}

    private static Piece powered(PortableEquipment equipment, String material, EquipmentSlot slot) {
        return new Piece(equipment.path, material, slot, 0, equipment);
    }

    /** Reports whether all four protective Hazmat pieces are worn. */
    public static boolean fullHazmat(LivingEntity wearer) {
        return wears(wearer, HEAD, "hazmat_helmet")
                && wears(wearer, CHEST, "hazmat_chestplate")
                && wears(wearer, LEGS, "hazmat_leggings")
                && wears(wearer, FEET, "rubber_boots");
    }

    private static boolean wears(LivingEntity wearer, EquipmentSlot slot, String path) {
        return wears(wearer.getItemBySlot(slot), path);
    }

    private static boolean wears(ItemStack stack, String path) {
        return stack.getItem() instanceof Wearable item && item.piece().path().equals(path);
    }

    static void tick(ServerPlayer player, ServerLevel level) {
        if (!player.isAlive() || player.isSpectator()) {
            ArmorPlatform.sprint(player, false);
            WALKS.remove(player);
            return;
        }
        var head = player.getItemBySlot(HEAD);
        var chest = player.getItemBySlot(CHEST);
        var legs = player.getItemBySlot(LEGS);
        if (wears(head, "quantum_helmet")) quantumHelmet(level, player, head);
        if (wears(chest, "quantum_chestplate") || fullHazmat(player)) player.clearFire();
        boolean boost =
                player.isSprinting()
                        && !player.isPassenger()
                        && !player.isUnderWater()
                        && wears(legs, "quantum_leggings")
                        && ArmorPlatform.spend(legs, 100);
        ArmorPlatform.sprint(player, boost);
        if (wears(head, "night_vision_goggles")
                && ArmorPlatform.dark(level, player.blockPosition())
                && !player.hasEffect(MobEffects.NIGHT_VISION)
                && ArmorPlatform.spend(head, 200)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
        }
        if (wears(head, "solar_helmet") && ArmorPlatform.sunlight(level, player)) {
            ArmorPlatform.charge(chest, 1);
        }
        if (wears(player, FEET, "static_boots")) {
            chargeByWalking(player, level, chest);
        } else {
            WALKS.remove(player);
        }
    }

    private static void quantumHelmet(ServerLevel level, ServerPlayer player, ItemStack helmet) {
        var eyes =
                new BlockPos(
                        Mth.floor(player.getX()),
                        Mth.floor(player.getEyeY()),
                        Mth.floor(player.getZ()));
        var fluid = level.getFluidState(eyes);
        if (fluid.is(FluidTags.WATER)
                && eyes.getY() + fluid.getHeight(level, eyes) > player.getEyeY()
                && player.getAirSupply() <= 100
                && ArmorPlatform.spend(helmet, 1_000)) {
            player.setAirSupply(player.getMaxAirSupply());
        }
        var poison = player.getEffect(MobEffects.POISON);
        if (poison != null && ArmorPlatform.spend(helmet, 10_000.0 * (poison.getAmplifier() + 1))) {
            player.removeEffect(MobEffects.POISON);
        }
        var wither = player.getEffect(MobEffects.WITHER);
        if (wither != null && ArmorPlatform.spend(helmet, 50_000.0 * (wither.getAmplifier() + 1))) {
            player.removeEffect(MobEffects.WITHER);
        }
        if (Radiation.has(player) && ArmorPlatform.spend(helmet, 10_000)) {
            Radiation.clear(player);
        }
        if (player.getFoodData().getFoodLevel() <= 16) ArmorPlatform.feed(player, helmet);
    }

    private static void chargeByWalking(ServerPlayer player, ServerLevel level, ItemStack chest) {
        var pos = player.position();
        var old = WALKS.get(player);
        double carried = 0;
        if (old != null
                && level == old.level()
                && ArmorPlatform.grounded(player)
                && !player.isPassenger()) {
            double stride = pos.subtract(old.position()).horizontalDistance();
            if (stride <= 3) {
                carried = old.distance() + stride;
                if (carried >= 10) {
                    ArmorPlatform.charge(chest, 2);
                    carried -= 10;
                }
            }
        }
        WALKS.put(player, new Walk(level, pos, carried));
    }

    static boolean absorbFall(ServerPlayer player, double distance) {
        var boots = player.getItemBySlot(FEET);
        int wear = (int) Math.ceil((distance - 3) / 2);
        if (distance > 3
                && distance <= 10
                && wears(boots, "rubber_boots")
                && boots.getMaxDamage() - boots.getDamageValue() >= wear) {
            ArmorPlatform.wearBoots(player, boots, wear);
            return true;
        }
        return false;
    }

    /** Reports whether the worn boots can respond to the quantum jump control. */
    public static boolean hasBoostBoots(LivingEntity wearer) {
        return wears(wearer, FEET, "quantum_boots");
    }

    /**
     * Boosts a real takeoff after the host confirms the jump and the player holds the boost key.
     */
    public static void boost(ServerPlayer player) {
        var boots = player.getItemBySlot(FEET);
        if (!player.isSpectator()
                && !player.isPassenger()
                && !player.isUnderWater()
                && hasBoostBoots(player)
                && player.getDeltaMovement().y > 0
                && ArmorPlatform.spend(boots, 4_000)) {
            var velocity = player.getDeltaMovement();
            player.setDeltaMovement(velocity.x, Math.max(velocity.y, 0.95), velocity.z);
            player.hurtMarked = true;
        }
    }

    /** Whether the four matching quantum pieces currently contain usable charge. */
    public static boolean poweredQuantum(LivingEntity wearer) {
        if (wearer == null || completeSuit(wearer) != Suit.QUANTUM) return false;
        for (var slot : ARMOR) {
            if (ArmorPlatform.energy(wearer.getItemBySlot(slot)) <= 0) return false;
        }
        return true;
    }

    private static Suit completeSuit(LivingEntity wearer) {
        Suit material = suit(wearer.getItemBySlot(HEAD));
        if (material == null) return null;
        for (var slot : ARMOR) {
            var stack = wearer.getItemBySlot(slot);
            if (suit(stack) != material
                    || !(stack.getItem() instanceof Wearable piece)
                    || piece.piece().slot() != slot) return null;
        }
        return material;
    }

    /** Charges only the proportion of the impact that the complete suit actually absorbs. */
    static float absorbImpact(ServerPlayer player, float incoming) {
        Suit material = completeSuit(player);
        if (material == null || !Float.isFinite(incoming) || incoming <= 0) return 0;
        double cost = Math.max(material.minimum(), incoming * (double) material.perDamage());
        double absorbed = 0;
        for (var slot : ARMOR) {
            var stack = player.getItemBySlot(slot);
            double charge = ArmorPlatform.energy(stack);
            double paid = Math.min(charge, cost);
            if (paid > 0) {
                ArmorPlatform.restoreEnergy(stack, charge - paid);
                absorbed += paid / cost / ARMOR.size();
            }
        }
        return (float) Math.min(1, absorbed);
    }

    static Defense protection(ItemStack stack) {
        var suit = suit(stack);
        if (suit == null || !(stack.getItem() instanceof Wearable wearable)) return null;
        var slot = wearable.piece().slot();
        int points =
                suit == Suit.QUANTUM
                        ? switch (slot) {
                            case HEAD, LEGS, FEET -> 2;
                            case CHEST -> 4;
                            default -> 0;
                        }
                        : 0;
        return new Defense(slot, points, 0);
    }

    private static Suit suit(ItemStack stack) {
        var type = electric(stack);
        if (type == null) return null;
        if (type.path.startsWith("quantum_")) return Suit.QUANTUM;
        return type.path.startsWith("nano_") ? Suit.NANO : null;
    }

    private record Suit(int minimum, int perDamage) {
        private static final Suit NANO = new Suit(5_000, 1_250);
        private static final Suit QUANTUM = new Suit(20_000, 5_000);
    }

    record Defense(EquipmentSlot slot, int armor, int toughness) {}

    private record Walk(ServerLevel level, Vec3 position, double distance) {}
}
