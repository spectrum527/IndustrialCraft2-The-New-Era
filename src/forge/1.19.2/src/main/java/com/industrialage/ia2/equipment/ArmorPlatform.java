package com.industrialage.ia2.equipment;

import com.industrialage.ia2.equipment.ArmorEffects.Piece;
import com.industrialage.ia2.forge.content.ForgeContent;
import com.industrialage.ia2.forge.equipment.ForgeGear;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Industrial wearable items and server hooks for Forge 1.19.2. */
public final class ArmorPlatform {
    private static final AttributeModifier SPRINT =
            new AttributeModifier(
                    UUID.fromString("b6c3e2b3-1326-4dfd-8ba5-367ac9dc730f"),
                    "Industrial quantum sprint",
                    0.45,
                    AttributeModifier.Operation.MULTIPLY_TOTAL);

    private ArmorPlatform() {}

    /** Installs the item and protection bindings for the Forge loader. */
    public static void install(DeferredRegister<Item> items, IEventBus gameBus) {
        for (var piece : ArmorEffects.PIECES) {
            items.register(piece.path(), () -> new SuitItem(piece));
        }
        gameBus.addListener(
                (TickEvent.PlayerTickEvent event) -> {
                    if (event.phase == TickEvent.Phase.END
                            && event.player instanceof ServerPlayer player) {
                        ArmorEffects.tick(player, player.getLevel());
                    }
                });
        gameBus.addListener(
                (LivingFallEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player
                            && ArmorEffects.absorbFall(player, event.getDistance())) {
                        event.setCanceled(true);
                    }
                });
        gameBus.addListener(ArmorPlatform::protect);
        gameBus.addListener(ArmorPlatform::protectiveAttributes);
    }

    private static void protectiveAttributes(ItemAttributeModifierEvent event) {
        var defense = ArmorEffects.protection(event.getItemStack());
        if (defense == null || event.getSlotType() != defense.slot()) return;
        String id = "ic2:powered_armor_" + defense.slot().getName();
        UUID uuid = UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));
        if (defense.armor() > 0)
            event.addModifier(
                    Attributes.ARMOR,
                    new AttributeModifier(
                            uuid, id, defense.armor(), AttributeModifier.Operation.ADDITION));
        else
            event.addModifier(
                    Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(
                            uuid, id, defense.toughness(), AttributeModifier.Operation.ADDITION));
    }

    private static void protect(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) return;
        var source = event.getSource();
        if ((source.isFire()
                        || source == DamageSource.HOT_FLOOR
                        || source == DamageSource.LIGHTNING_BOLT
                        || source.getMsgId().equals("electricity"))
                && ArmorEffects.fullHazmat(player)) {
            player.clearFire();
            event.setCanceled(true);
        } else if (!source.isBypassArmor()) {
            ArmorEffects.absorbImpact(player, event.getAmount());
        }
    }

    static boolean grounded(ServerPlayer player) {
        return player.isOnGround();
    }

    static boolean sunlight(ServerLevel level, ServerPlayer player) {
        var head = player.blockPosition().above();
        return level.getSkyDarken() < 4 && !level.isRaining() && level.canSeeSky(head);
    }

    static boolean dark(ServerLevel level, BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos) <= 7;
    }

    static void wearBoots(ServerPlayer player, ItemStack boots, int wear) {
        boots.hurtAndBreak(wear, player, wearer -> wearer.broadcastBreakEvent(EquipmentSlot.FEET));
    }

    static boolean spend(ItemStack item, double amount) {
        return ForgeGear.spend(item, amount);
    }

    static double energy(ItemStack item) {
        return ForgeGear.stored(item);
    }

    static void restoreEnergy(ItemStack item, double amount) {
        ForgeGear.restoreEnergy(item, amount);
    }

    static void charge(ItemStack item, double amount) {
        ForgeGear.charge(item, amount, Integer.MAX_VALUE);
    }

    static void sprint(ServerPlayer player, boolean boosted) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null || boosted == speed.hasModifier(SPRINT)) return;
        if (boosted) speed.addTransientModifier(SPRINT);
        else speed.removeModifier(SPRINT);
    }

    static void feed(ServerPlayer player, ItemStack helmet) {
        var inventory = player.getInventory();
        var filled = item("filled_tin_can");
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var can = inventory.getItem(slot);
            if (!can.is(filled)) continue;
            if (!spend(helmet, 1_000)) return;
            can.shrink(1);
            player.getFoodData().eat(1, 0.6F);
            var tin = new ItemStack(item("tin_can"));
            if (!inventory.add(tin)) player.drop(tin, false);
            return;
        }
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation("ic2", name);
    }

    private static Item item(String path) {
        return ForgeRegistries.ITEMS.getValue(id(path));
    }

    private record Material(Piece piece) implements ArmorMaterial {
        private boolean alloy() {
            return piece.material().equals("alloy");
        }

        @Override
        public int getDurabilityForSlot(EquipmentSlot slot) {
            return piece.durability();
        }

        @Override
        public int getDefenseForSlot(EquipmentSlot slot) {
            return alloy() ? 6 : 0;
        }

        @Override
        public int getEnchantmentValue() {
            return alloy() ? 12 : 0;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return alloy() ? Ingredient.of(item("alloy")) : Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return id(piece.material()).toString();
        }

        @Override
        public float getToughness() {
            return alloy() ? 2 : 0;
        }

        @Override
        public float getKnockbackResistance() {
            return 0;
        }
    }

    private static final class SuitItem extends ArmorItem implements ArmorEffects.Wearable {
        private final Piece piece;

        SuitItem(Piece piece) {
            super(new Material(piece), piece.slot(), ForgeContent.itemProperties().stacksTo(1));
            this.piece = piece;
        }

        @Override
        public Piece piece() {
            return piece;
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return piece.electric() == null
                    ? super.isBarVisible(stack)
                    : ForgeGear.stored(stack) > 0;
        }

        @Override
        public int getBarWidth(ItemStack stack) {
            return piece.electric() == null
                    ? super.getBarWidth(stack)
                    : (int) Math.round(13 * ForgeGear.stored(stack) / piece.electric().capacity);
        }

        @Override
        public int getBarColor(ItemStack stack) {
            return piece.electric() == null ? super.getBarColor(stack) : 0x58C7D5;
        }
    }
}
