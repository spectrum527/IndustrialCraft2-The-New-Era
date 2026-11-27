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
        check.accept("quantum-chest-empty-leaves-fire", player.isOnFire());
        var chest = equip(player, EquipmentSlot.CHEST, "quantum_chestplate", 200);
        tick(player);
        check.accept(
                "quantum-chest-extinguishes", !player.isOnFire() && ForgeGear.stored(chest) == 0);
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
        var priorDifficulty = level.getDifficulty();
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        poweredImpact(level, player, "nano", 12_500, check);
        poweredImpact(level, player, "quantum", 50_000, check);
        level.getServer().setDifficulty(priorDifficulty, true);
        miningAndNightVision(level, player, check);
        unchargedQuantumPieces(player, check);
        killBoundary(level, check);
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

    private static final EquipmentSlot[] SUIT_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final String[] SUIT_PARTS = {"helmet", "chestplate", "leggings", "boots"};

    private static java.util.List<ItemStack> suit(ServerPlayer player, String family, int charge) {
        var stacks = new java.util.ArrayList<ItemStack>(4);
        for (int i = 0; i < SUIT_SLOTS.length; i++)
            stacks.add(equip(player, SUIT_SLOTS[i], family + "_" + SUIT_PARTS[i], charge));
        player.doTick();
        return stacks;
    }

    private static void poweredImpact(
            ServerLevel level,
            ServerPlayer player,
            String family,
            int cost,
            BiConsumer<String, Boolean> check) {
        suit(player, family, 0);
        check.accept(
                family + "-empty-native-armor",
                player.getArmorValue() == (family.equals("quantum") ? 10 : 0));
        for (String source : new String[] {"melee", "fire", "fall", "magic", "void"}) {
            var stacks = suit(player, family, 200_000);
            player.setHealth(20);
            player.invulnerableTime = 0;
            impact(player, source, 10);
            check.accept(
                    family + "-blocks-" + source,
                    player.getHealth() == 20
                            && stacks.stream()
                                    .allMatch(piece -> ForgeGear.stored(piece) == 200_000 - cost));
        }
        var stacks = suit(player, family, 200_000);
        player.setHealth(20);
        player.invulnerableTime = 0;
        impact(player, "magic", 25);
        check.accept(family + "-blocks-lethal-damage", player.getHealth() == 20);

        stacks = suit(player, family, cost / 2);
        player.setHealth(20);
        player.invulnerableTime = 0;
        impact(player, "magic", 10);
        check.accept(
                family + "-low-energy-leaves-damage",
                player.getHealth() < 20
                        && player.getHealth() > 10
                        && stacks.stream().allMatch(piece -> ForgeGear.stored(piece) == 0));

        stacks = suit(player, family, 200_000);
        ForgeGear.restoreEnergy(stacks.get(0), 0);
        player.setHealth(20);
        player.invulnerableTime = 0;
        impact(player, "magic", 10);
        check.accept(
                family + "-one-empty-piece-leaves-one-quarter-damage",
                Math.abs(player.getHealth() - 17.5F) < 0.0001F
                        && ForgeGear.stored(stacks.get(0)) == 0
                        && stacks.subList(1, 4).stream()
                                .allMatch(piece -> ForgeGear.stored(piece) == 200_000 - cost));

        stacks = suit(player, family, 200_000);
        var mixed =
                equip(
                        player,
                        EquipmentSlot.HEAD,
                        (family.equals("nano") ? "quantum" : "nano") + "_helmet",
                        200_000);
        player.setHealth(20);
        player.invulnerableTime = 0;
        impact(player, "magic", 10);
        int otherCost = family.equals("nano") ? 50_000 : 12_500;
        check.accept(
                family + "-mixed-suit-absorbs-per-piece",
                player.getHealth() == 20
                        && ForgeGear.stored(mixed) == 200_000 - otherCost
                        && stacks.subList(1, 4).stream()
                                .allMatch(piece -> ForgeGear.stored(piece) == 200_000 - cost));

        stacks = suit(player, family, 200_000);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.setHealth(20);
        player.invulnerableTime = 0;
        impact(player, "magic", 10);
        check.accept(
                family + "-incomplete-suit-absorbs-three-quarters",
                Math.abs(player.getHealth() - 17.5F) < 0.0001F
                        && stacks.subList(1, 4).stream()
                                .allMatch(piece -> ForgeGear.stored(piece) == 200_000 - cost));

        stacks = suit(player, family, 200_000);
        player.setHealth(20);
        player.invulnerableTime = 0;
        impact(player, "melee", 10);
        double spent = ForgeGear.stored(stacks.get(0));
        impact(player, "melee", 10);
        check.accept(
                family + "-hurt-cooldown-does-not-charge-twice",
                stacks.stream().allMatch(piece -> ForgeGear.stored(piece) == spent));
    }

    private static void miningAndNightVision(
            ServerLevel level, ServerPlayer player, BiConsumer<String, Boolean> check) {
        suit(player, "quantum", 50_000);
        check.accept(
                "quantum-vision-no-potion",
                ArmorEffects.poweredQuantum(player) && !player.hasEffect(MobEffects.NIGHT_VISION));
        float boosted =
                player.getDigSpeed(Blocks.STONE.defaultBlockState(), player.blockPosition());
        float boostedProgress =
                Blocks.STONE
                        .defaultBlockState()
                        .getDestroyProgress(player, level, player.blockPosition());
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        float normal = player.getDigSpeed(Blocks.STONE.defaultBlockState(), player.blockPosition());
        float normalProgress =
                Blocks.STONE
                        .defaultBlockState()
                        .getDestroyProgress(player, level, player.blockPosition());
        check.accept(
                "quantum-native-break-progress-one-point-four",
                normalProgress > 0 && Math.abs(boostedProgress - normalProgress * 1.4F) < 0.0001F);
        check.accept(
                "quantum-mining-speed-one-point-four", Math.abs(boosted - normal * 1.4F) < 0.0001F);
        check.accept("quantum-effect-stops-on-removal", !ArmorEffects.poweredQuantum(player));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200));
        suit(player, "quantum", 50_000);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        tick(player);
        check.accept("player-night-vision-is-preserved", player.hasEffect(MobEffects.NIGHT_VISION));
        player.removeEffect(MobEffects.NIGHT_VISION);
    }

    private static void unchargedQuantumPieces(
            ServerPlayer player, BiConsumer<String, Boolean> check) {
        int[] expected = {2, 4, 2, 2};
        for (int i = 0; i < SUIT_SLOTS.length; i++) {
            for (var slot : SUIT_SLOTS) player.setItemSlot(slot, ItemStack.EMPTY);
            equip(player, SUIT_SLOTS[i], "quantum_" + SUIT_PARTS[i], 0);
            player.doTick();
            check.accept(
                    "quantum-empty-" + SUIT_PARTS[i] + "-armor-points",
                    player.getArmorValue() == expected[i]);
        }
    }

    private static void killBoundary(ServerLevel level, BiConsumer<String, Boolean> check) {
        var profile = new GameProfile(UUID.randomUUID(), "IC2KillQA");
        var player = new ServerPlayer(level.getServer(), level, profile, null);
        player.connection = new FakePlayer(level, profile).connection;
        suit(player, "quantum", 200_000);
        player.kill();
        check.accept("administrative-native-kill-still-kills", !player.isAlive());
    }

    private static void impact(ServerPlayer player, String kind, float amount) {
        var source =
                switch (kind) {
                    case "melee" ->
                            DamageSource.mobAttack(
                                    new net.minecraft.world.entity.monster.Zombie(
                                            player.getLevel()));
                    case "fire" -> DamageSource.IN_FIRE;
                    case "fall" -> DamageSource.FALL;
                    case "magic" -> DamageSource.MAGIC;
                    case "void" -> DamageSource.OUT_OF_WORLD;
                    default -> DamageSource.CACTUS;
                };
        player.hurt(source, amount);
    }

    private static void tick(ServerPlayer player) {
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }
}
