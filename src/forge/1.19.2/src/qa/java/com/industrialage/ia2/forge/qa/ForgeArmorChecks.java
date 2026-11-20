package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.equipment.ArmorEffects;
import com.industrialage.ia2.equipment.JetpackMount;
import com.industrialage.ia2.forge.equipment.ForgeGear;
import com.industrialage.ia2.industrial.Radiation;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Native equip, player tick, damage and durability checks in the packaged mod. */
final class ForgeArmorChecks {
    private ForgeArmorChecks() {}

    static void run(ServerLevel level, BiConsumer<String, Boolean> check) {
        var profile = new GameProfile(UUID.randomUUID(), "IC2ArmorQA");
        var player = new ServerPlayer(level.getServer(), level, profile, null);
        player.connection = new FakePlayer(level, profile).connection;
        player.setPos(20.5, 128, 21.5);
        for (int tick = 0; tick <= 60; tick++) player.tick();
        var boots = equip(player, EquipmentSlot.FEET, "rubber_boots", 0);
        player.setHealth(20);
        player.invulnerableTime = 0;
        player.causeFallDamage(10, 1, DamageSource.FALL);
        check.accept(
                "rubber-boots-fall-ten", player.getHealth() == 20 && boots.getDamageValue() == 4);
        player.causeFallDamage(11, 1, DamageSource.FALL);
        check.accept(
                "rubber-boots-fall-eleven", player.getHealth() < 20 && boots.getDamageValue() == 4);
        player.invulnerableTime = 0;
        player.setHealth(20);
        boots.setDamageValue(60);
        player.causeFallDamage(10, 1, DamageSource.FALL);
        check.accept(
                "rubber-boots-break-absorbing-fall", player.getHealth() == 20 && boots.isEmpty());
        var worn = equip(player, EquipmentSlot.FEET, "rubber_boots", 0);
        worn.setDamageValue(61);
        player.causeFallDamage(10, 1, DamageSource.FALL);
        check.accept(
                "rubber-boots-need-sufficient-durability",
                player.getHealth() < 20 && worn.getDamageValue() == 61);
        player.invulnerableTime = 0;
        player.setHealth(20);

        equip(player, EquipmentSlot.FEET, "rubber_boots", 0);
        equip(player, EquipmentSlot.HEAD, "hazmat_helmet", 0);
        equip(player, EquipmentSlot.CHEST, "hazmat_chestplate", 0);
        equip(player, EquipmentSlot.LEGS, "hazmat_leggings", 0);
        player.setRemainingFireTicks(60);
        player.hurt(DamageSource.IN_FIRE, 2);
        Radiation.expose(player, 120, 1);
        check.accept("full-hazmat-recognized", ArmorEffects.fullHazmat(player));
        check.accept("complete-hazmat-blocks-radiation", !Radiation.has(player));
        check.accept("complete-hazmat-blocks-fire-damage", player.getHealth() == 20);
        check.accept("complete-hazmat-extinguishes-active-fire", !player.isOnFire());
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.invulnerableTime = 0;
        player.hurt(DamageSource.IN_FIRE, 2);
        Radiation.expose(player, 120, 1);
        check.accept(
                "partial-hazmat-leaves-exposure", player.getHealth() < 20 && Radiation.has(player));

        var helmet = equip(player, EquipmentSlot.HEAD, "quantum_helmet", 31_000);
        equip(player, EquipmentSlot.CHEST, "quantum_chestplate", 0);
        player.setRemainingFireTicks(60);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, 120));
        player.getFoodData().setFoodLevel(15);
        player.getInventory().setItem(10, new ItemStack(item("filled_tin_can")));
        var leggings = equip(player, EquipmentSlot.LEGS, "quantum_leggings", 2_000);
        double base = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        player.setSprinting(false);
        tick(player);
        check.accept(
                "quantum-helmet-clears-poison-and-radiation",
                !Radiation.has(player)
                        && !player.hasEffect(MobEffects.POISON)
                        && player.hasEffect(MobEffects.WITHER));
        check.accept(
                "quantum-helmet-eats-and-returns-can",
                player.getFoodData().getFoodLevel() == 16
                        && player.getInventory().contains(new ItemStack(item("tin_can")))
                        && ForgeGear.stored(helmet) == 0);
        check.accept("quantum-chest-extinguishes", !player.isOnFire());
        check.accept(
                "quantum-walk-costs-zero",
                ForgeGear.stored(leggings) == 2_000
                        && player.getAttributeValue(Attributes.MOVEMENT_SPEED) == base);
        player.setSprinting(true);
        for (int tick = 0; tick < 10; tick++) tick(player);
        check.accept(
                "quantum-sprints-at-100-eu-per-tick",
                player.getAttributeValue(Attributes.MOVEMENT_SPEED) > base
                        && ForgeGear.stored(leggings) == 1_000);
        player.setSprinting(false);
        player.removeEffect(MobEffects.WITHER);
        tick(player);

        var quantumBoots = equip(player, EquipmentSlot.FEET, "quantum_boots", 9_000);
        player.setDeltaMovement(0, 0, 0);
        ArmorEffects.boost(player);
        check.accept("grounded-quantum-boost-costs-zero", ForgeGear.stored(quantumBoots) == 9_000);
        player.setDeltaMovement(0, 0.42, 0);
        ArmorEffects.boost(player);
        check.accept(
                "boosted-takeoff-costs-four-thousand",
                ForgeGear.stored(quantumBoots) == 5_000 && player.getDeltaMovement().y > 0.42);
        var at = new BlockPos(21, 125, 21);
        player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        var water = new BlockPos(player.getEyePosition());
        level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        player.setAirSupply(5);
        var poweredHelmet = equip(player, EquipmentSlot.HEAD, "quantum_helmet", 4_000);
        tick(player);
        check.accept(
                "submerged-quantum-breath-costs-one-thousand",
                player.getAirSupply() == player.getMaxAirSupply()
                        && ForgeGear.stored(poweredHelmet) == 3_000);
        player.setAirSupply(5);
        equip(player, EquipmentSlot.HEAD, "quantum_helmet", 0);
        tick(player);
        check.accept("empty-quantum-helmet-leaves-breath-alone", player.getAirSupply() == 5);
        level.setBlockAndUpdate(water, Blocks.AIR.defaultBlockState());

        equip(player, EquipmentSlot.HEAD, "solar_helmet", 0);
        var pack = equip(player, EquipmentSlot.CHEST, "batpack", 0);
        player.setPos(20.5, 125, 21.5);
        tick(player);
        check.accept("sky-exposed-solar-helmet-charges-one-eu", ForgeGear.stored(pack) == 1);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        equip(player, EquipmentSlot.FEET, "static_boots", 0);
        player.setOnGround(true);
        tick(player);
        for (int stride = 0; stride < 10; stride++) {
            player.setPos(player.getX() + 1, player.getY(), player.getZ());
            tick(player);
        }
        check.accept("static-boots-charge-two-eu-on-ten-blocks", ForgeGear.stored(pack) == 3);
        for (var slot :
                new EquipmentSlot[] {
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
                }) {
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
        player.getFoodData().setFoodLevel(20);
        player.setHealth(20);
        mountedPack(level, player, check);
        poweredImpact(player, "nano", 12_500, 4_999, check);
        poweredImpact(player, "quantum", 50_000, 19_999, check);
    }

    private static void mountedPack(
            ServerLevel level, ServerPlayer player, BiConsumer<String, Boolean> check) {
        var electric = item("electric_jetpack");
        var plate = new ItemStack(item("jetpack_attachment_plate"));
        var charge = new ItemStack(electric);
        ForgeGear.restoreEnergy(charge, 155.5);
        var armor = new ItemStack(Items.LEATHER_CHESTPLATE);
        armor.setDamageValue(20);
        var craft = new CraftingContainer(player.inventoryMenu, 2, 2);
        craft.setItem(0, armor);
        craft.setItem(1, plate);
        craft.setItem(2, charge);
        var chest =
                level.getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, craft, level)
                        .orElseThrow()
                        .assemble(craft);
        check.accept(
                "native-jetpack-attachment-preserves-armor-and-eu",
                chest.is(Items.LEATHER_CHESTPLATE)
                        && chest.getDamageValue() == 20
                        && JetpackMount.energy(chest) == 155.5);
        ForgeGear.charge(chest, 30, 1);
        check.accept(
                "attached-pack-independently-charges-and-flies",
                JetpackMount.thrust(chest, true, false) && JetpackMount.energy(chest) == 178.5);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.invulnerableTime = 0;
        player.hurt(DamageSource.CACTUS, 12);
        check.accept(
                "intact-armor-keeps-pack",
                !chest.isEmpty()
                        && JetpackMount.energy(chest) == 178.5
                        && !player.getInventory().contains(new ItemStack(electric)));
        chest.setDamageValue(chest.getMaxDamage() - 2);
        player.setHealth(20);
        player.invulnerableTime = 0;
        player.hurt(DamageSource.CACTUS, 12);
        var inventory = player.getInventory();
        int count = 0;
        double recovered = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.is(electric)) {
                count += stack.getCount();
                recovered = ForgeGear.stored(stack);
            }
        }
        if (!chest.isEmpty() || count != 1 || recovered != 178.5) {
            throw new AssertionError(
                    "Broken armor state="
                            + chest
                            + " damage="
                            + chest.getDamageValue()
                            + ", recovered packs="
                            + count
                            + " EU="
                            + recovered);
        }
        check.accept("broken-armor-returns-one-original-pack", true);
        tick(player);
        check.accept("recovery-clears-mounted-identity", !JetpackMount.installed(chest));
    }

    private static ItemStack equip(ServerPlayer player, EquipmentSlot slot, String id, int charge) {
        var equipment = new ItemStack(item(id));
        if (charge > 0) ForgeGear.restoreEnergy(equipment, charge);
        player.setItemSlot(slot, equipment);
        return equipment;
    }

    private static Item item(String path) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation("ic2", path));
    }

    private static void poweredImpact(
            ServerPlayer player,
            String family,
            int cost,
            int tooLow,
            BiConsumer<String, Boolean> check) {
        var slots =
                new EquipmentSlot[] {
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
                };
        var parts = new String[] {"helmet", "chestplate", "leggings", "boots"};
        for (int trial = 0; trial < 2; trial++) {
            int charge = trial == 0 ? 90_000 : tooLow;
            var suit = new java.util.ArrayList<ItemStack>(4);
            for (int index = 0; index < slots.length; index++) {
                suit.add(equip(player, slots[index], family + "_" + parts[index], charge));
            }
            player.doTick();
            int points = player.getArmorValue();
            player.setHealth(20);
            player.invulnerableTime = 0;
            player.hurt(DamageSource.CACTUS, 10);
            if (trial == 0) {
                check.accept(
                        family + "-powered-impact-parity",
                        points == 20
                                && player.getHealth() == 16
                                && suit.stream()
                                        .allMatch(
                                                piece -> ForgeGear.stored(piece) == charge - cost));
            } else {
                check.accept(
                        family + "-low-energy-impact-parity",
                        points == 0
                                && player.getHealth() == 10
                                && suit.stream().allMatch(piece -> ForgeGear.stored(piece) == 0));
            }
        }
    }

    private static void tick(ServerPlayer player) {
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }
}
