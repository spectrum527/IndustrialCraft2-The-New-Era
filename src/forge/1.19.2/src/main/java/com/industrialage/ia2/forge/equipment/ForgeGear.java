package com.industrialage.ia2.forge.equipment;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.industrialage.ia2.audio.IndustrialSounds;
import com.industrialage.ia2.compat.CuriosBack;
import com.industrialage.ia2.content.PortableEquipment;
import com.industrialage.ia2.content.PortableEquipment.Kind;
import com.industrialage.ia2.equipment.ArmorEffects;
import com.industrialage.ia2.equipment.ArmorPlatform;
import com.industrialage.ia2.equipment.JetpackMount;
import com.industrialage.ia2.equipment.MountPlatform;
import com.industrialage.ia2.equipment.WindMeter;
import com.industrialage.ia2.forge.content.ForgeContent;
import com.industrialage.ia2.progress.IndustrialProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Portable electrical equipment and ordinary bronze tools on Forge 1.19.2. */
public final class ForgeGear {
    private static final String ENERGY = "ic2_energy";
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "ic2");

    private ForgeGear() {}

    /** Registers the tools and bridges held or equipped energy transfers. */
    public static void install(IEventBus modBus, IEventBus gameBus) {
        for (var entry : PortableEquipment.CATALOG) {
            if (entry != PortableEquipment.RE_BATTERY && entry.kind != Kind.ARMOR) {
                ITEMS.register(
                        entry.path,
                        () ->
                                new PoweredItem(
                                        entry,
                                        entry.kind == Kind.TOOL
                                                ? ForgeContent.itemProperties(entry.path)
                                                        .durability(
                                                                entry
                                                                                == PortableEquipment
                                                                                        .NANO_SABER
                                                                        ? 1_561
                                                                        : 250)
                                                : ForgeContent.itemProperties(entry.path)
                                                        .stacksTo(entry.emptyStackSize())));
            }
        }
        var bronze =
                new Tier() {
                    @Override
                    public int getUses() {
                        return 350;
                    }

                    @Override
                    public float getSpeed() {
                        return 6;
                    }

                    @Override
                    public float getAttackDamageBonus() {
                        return 2;
                    }

                    @Override
                    public int getLevel() {
                        return Tiers.IRON.getLevel();
                    }

                    @Override
                    public int getEnchantmentValue() {
                        return 14;
                    }

                    @Override
                    public Ingredient getRepairIngredient() {
                        return Ingredient.of(
                                TagKey.create(
                                        Registry.ITEM_REGISTRY,
                                        new ResourceLocation("ic2", "bronze_ingots")));
                    }
                };
        ITEMS.register(
                "bronze_pickaxe",
                () ->
                        new PickaxeItem(
                                bronze, 1, -2.8F, ForgeContent.itemProperties("bronze_pickaxe")));
        ITEMS.register(
                "bronze_axe",
                () -> new AxeItem(bronze, 6, -3.1F, ForgeContent.itemProperties("bronze_axe")));
        ITEMS.register(
                "bronze_shovel",
                () ->
                        new ShovelItem(
                                bronze, 1.5F, -3.0F, ForgeContent.itemProperties("bronze_shovel")));
        ITEMS.register(
                "bronze_hoe",
                () -> new HoeItem(bronze, -2, -1.0F, ForgeContent.itemProperties("bronze_hoe")));
        ITEMS.register(
                "bronze_sword",
                () -> new SwordItem(bronze, 3, -2.4F, ForgeContent.itemProperties("bronze_sword")));
        ITEMS.register("jetpack", FuelJetpack::new);
        ArmorPlatform.install(ITEMS, gameBus);
        MountPlatform.install(ITEMS, modBus, gameBus);
        ITEMS.register(modBus);
        ForgeBoats.install(modBus);
        ForgeLaser.install(modBus);
        gameBus.addListener(ForgeLaser::tooltip);
        ForgeSaber.install(modBus, gameBus);
        JetpackControls.install(modBus, gameBus);
        gameBus.addListener(ForgeGear::tickPlayer);
        gameBus.addListener(ForgeGear::manualTransfer);
        gameBus.addListener(EventPriority.LOWEST, ForgeGear::paidToolHit);
        gameBus.addListener(
                (AttackEntityEvent event) -> {
                    var player = event.getEntity();
                    var stack = player.getMainHandItem();
                    if (player.level instanceof ServerLevel
                            && stack.getItem() instanceof PoweredItem item)
                        item.refreshMelee(player, stack);
                });
    }

    private static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        var pack = CuriosBack.preferChest(player, stack -> isPack(stack) && stored(stack) > 0);
        if (!pack.isEmpty()) {
            transfer(pack, player.getMainHandItem());
            transfer(pack, player.getOffhandItem());
        }
        var inv = player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            chargeFromHotbarCharger(inv.getItem(slot), player);
        }
        chargeFromHotbarCharger(player.getOffhandItem(), player);
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var item = inv.getItem(i);
            if (ForgeSaber.active(item)) ForgeSaber.tick(player, item);
        }
    }

    private static void chargeFromHotbarCharger(ItemStack stack, ServerPlayer player) {
        var device = type(stack);
        if (device == null || device.kind != Kind.CHARGER || stored(stack) <= 0) return;
        var inv = player.getInventory();
        for (int slot = 0; slot < 9 && stored(stack) > 0; slot++) {
            var candidate = inv.getItem(slot);
            if (candidate != stack) transfer(stack, candidate);
        }
        var offhand = player.getOffhandItem();
        if (offhand != stack && stored(stack) > 0) transfer(stack, offhand);
    }

    private static void paidToolHit(LivingDamageEvent event) {
        var source = event.getSource();
        if (!(event.getEntity().level instanceof ServerLevel)
                || event.getAmount() <= 0
                || !(source.getEntity() instanceof LivingEntity attacker)
                || source.getDirectEntity() != attacker
                || !("player".equals(source.getMsgId()) || "mob".equals(source.getMsgId()))
                || attacker instanceof ServerPlayer player && player.isCreative()) return;
        var stack = attacker.getMainHandItem();
        var tool = type(stack);
        if (tool == null || tool.poweredMeleeDamage() == 0 || !spend(stack, tool.cost)) return;
        if (stored(stack) < tool.cost && stack.getItem() instanceof PoweredItem item)
            item.refreshMelee(attacker, stack);
        if (attacker instanceof ServerPlayer player)
            IndustrialProgress.poweredToolHit(player, stack);
    }

    private static void manualTransfer(PlayerInteractEvent.RightClickItem event) {
        var source = event.getItemStack();
        if (type(source) == PortableEquipment.NANO_SABER) {
            ForgeSaber.use(event);
            return;
        }
        if (type(source) == PortableEquipment.MINING_LASER) {
            ForgeLaser.use(event);
            return;
        }
        if (type(source) == PortableEquipment.WIND_METER) {
            var player = event.getEntity();
            event.setCancellationResult(windReading(player, source, player.blockPosition()));
            event.setCanceled(true);
            return;
        }
        var target =
                event.getEntity()
                        .getItemInHand(
                                event.getHand() == InteractionHand.MAIN_HAND
                                        ? InteractionHand.OFF_HAND
                                        : InteractionHand.MAIN_HAND);
        var device = type(source);
        var consumer = type(target);
        var player = event.getEntity();
        if ((device != null && device.wearable() || source.getItem() instanceof FuelJetpack)
                && player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                && !player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer) {
                var worn = source.copy();
                worn.setCount(1);
                player.setItemSlot(EquipmentSlot.CHEST, worn);
                if (!player.isCreative()) source.shrink(1);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (device == null
                || consumer == null
                || !device.canProvide()
                || consumer.canProvide()
                || device.tier < consumer.tier
                || stored(source) <= 0
                || stored(target) >= consumer.capacity) {
            return;
        }
        if (player.getLevel() instanceof ServerLevel level) {
            double before = stored(target);
            transfer(source, target);
            if (stored(target) > before) {
                IndustrialSounds.play(level, player.blockPosition(), "item.battery.use");
            }
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void transfer(ItemStack source, ItemStack target) {
        var provider = type(source);
        var consumer = type(target);
        if (provider != null && consumer != null && !consumer.canProvide()) {
            if (provider.kind == Kind.DISPOSABLE) {
                if (provider.tier >= consumer.tier
                        && consumer.capacity - stored(target) >= provider.capacity) {
                    setStored(target, stored(target) + provider.capacity);
                    source.shrink(1);
                }
                return;
            }
            double accepted =
                    charge(target, Math.min(provider.rate, stored(source)), provider.tier);
            if (accepted > 0) setStored(source, stored(source) - accepted);
        }
    }

    /** Stack limit in a powered equipment slot, including stacks of single-use batteries. */
    public static int powerSlotLimit(ItemStack stack) {
        return type(stack) == PortableEquipment.SINGLE_USE_BATTERY || !electricalItem(stack)
                ? stack.getMaxStackSize()
                : 1;
    }

    /** Whether this equipment fits a charging slot of the given tier. */
    public static boolean acceptsCharge(ItemStack stack, int tier) {
        var device = type(stack);
        return (device != null && device.tier <= tier && device.kind != Kind.DISPOSABLE)
                || tier >= 1 && JetpackMount.installed(stack);
    }

    /** Whether this equipment supplies a machine's discharging slot. */
    public static boolean canDischarge(ItemStack stack, int tier) {
        var device = type(stack);
        return device != null && device.canProvide() && device.tier <= tier;
    }

    /** Whether this stack can deliver energy to an automation-fed discharge slot now. */
    public static boolean canSupplyCharge(ItemStack stack, int tier) {
        return canDischarge(stack, tier)
                && (type(stack).kind == Kind.DISPOSABLE || stored(stack) > 0);
    }

    /** Whether every chargeable energy reservoir of this item has reached its capacity. */
    public static boolean chargeComplete(ItemStack stack, int tier) {
        var device = type(stack);
        boolean main = device != null && device.tier <= tier && device.kind != Kind.DISPOSABLE;
        var pack = PortableEquipment.ELECTRIC_JETPACK;
        boolean mount = tier >= pack.tier && JetpackMount.installed(stack);
        return (main || mount)
                && (!main || stored(stack) >= device.capacity)
                && (!mount || JetpackMount.energy(stack) >= pack.capacity);
    }

    /** Charges the equipment at its transfer rate and returns the accepted EU. */
    public static double charge(ItemStack stack, double offered, int tier) {
        var device = type(stack);
        if (stack.getCount() != 1) return 0;
        if (device == null) return JetpackMount.charge(stack, offered, tier);
        double before = stored(stack);
        double moved = device.chargeAmount(offered, before, tier);
        if (moved > 0) setStored(stack, before + moved);
        return moved + JetpackMount.charge(stack, offered - moved, tier);
    }

    /** Draws up to the item's permitted EU output and returns the accepted debit. */
    public static double discharge(ItemStack stack, double requested, int tier) {
        return discharge(stack, requested, PortableEquipment.SINGLE_USE_BATTERY.capacity, tier);
    }

    /** Debits one battery only for EU the machine can retain under its buffer rules. */
    public static double discharge(
            ItemStack stack, double requested, double bufferCapacity, int tier) {
        var device = type(stack);
        if (device != null && device.kind == Kind.DISPOSABLE) {
            double accepted = device.disposableCharge(requested, bufferCapacity, tier);
            if (accepted > 0) stack.shrink(1);
            return accepted;
        }
        if (device == null || stack.getCount() != 1) return 0;
        double before = stored(stack);
        double moved = device.dischargeAmount(requested, before, tier);
        if (moved > 0) setStored(stack, before - moved);
        return moved;
    }

    static PortableEquipment type(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return stack.getItem() instanceof PoweredItem item
                ? item.device
                : ArmorEffects.electric(stack);
    }

    /** Exact stored EU read from this version's item save. */
    public static double stored(ItemStack stack) {
        var device = type(stack);
        if (device != null && device.kind == Kind.DISPOSABLE) return device.capacity;
        if (device == null) return JetpackMount.energy(stack);
        return !stack.hasTag() ? 0 : device.stored(stack.getTag().getDouble(ENERGY));
    }

    /** Recognizes a stack with electrical behavior, charged or empty. */
    public static boolean electricalItem(ItemStack stack) {
        return type(stack) != null || JetpackMount.installed(stack);
    }

    /** Pays the exact EU cost of using a powered item. */
    public static boolean spend(ItemStack stack, double amount) {
        var device = type(stack);
        if (device == null || device.canProvide() || !Double.isFinite(amount) || amount <= 0)
            return false;
        double prior = stored(stack);
        if (prior < amount) return false;
        setStored(stack, prior - amount);
        return true;
    }

    static void setStored(ItemStack stack, double amount) {
        if (amount > 0) stack.getOrCreateTag().putDouble(ENERGY, amount);
        else stack.removeTagKey(ENERGY);
    }

    /** Restores a transferred portable item's original charge. */
    public static void restoreEnergy(ItemStack stack, double amount) {
        var device = type(stack);
        if (device != null) setStored(stack, device.stored(amount));
    }

    /** Adds the normal item and, for rechargeable equipment, its fully charged counterpart. */
    public static void addCreativeStacks(Item item, NonNullList<ItemStack> output) {
        var ordinary = new ItemStack(item);
        output.add(ordinary);
        var device = type(ordinary);
        if (device != null && device.kind != Kind.DISPOSABLE) {
            var charged = ordinary.copy();
            restoreEnergy(charged, device.capacity);
            output.add(charged);
        }
    }

    private static boolean isPack(ItemStack stack) {
        var device = type(stack);
        return device != null && device.kind == Kind.PACK;
    }

    /** Live pack providing electric-boat thrust, or the fitted pack when its charge is low. */
    public static ItemStack boatPack(LivingEntity rider) {
        var working = CuriosBack.preferChest(rider, stack -> isPack(stack) && stored(stack) >= 4);
        return working.isEmpty() ? CuriosBack.preferChest(rider, ForgeGear::isPack) : working;
    }

    /** Nameplate EU capacity of a native portable electrical device. */
    public static double capacity(ItemStack stack) {
        var device = type(stack);
        return device == null ? 0 : device.capacity;
    }

    static boolean boatPower(LivingEntity rider, boolean draw) {
        var pack = boatPack(rider);
        if (stored(pack) < 4) return false;
        if (draw) setStored(pack, stored(pack) - 4);
        return true;
    }

    private static ItemStack jetpack(LivingEntity player) {
        return CuriosBack.preferChest(
                player,
                stack ->
                        type(stack) == PortableEquipment.ELECTRIC_JETPACK
                                || stack.getItem() instanceof FuelJetpack
                                || JetpackMount.installed(stack));
    }

    static boolean hasJetpack(LivingEntity player) {
        return !jetpack(player).isEmpty();
    }

    static boolean payForFlight(ServerPlayer player, boolean hover) {
        var stack = jetpack(player);
        if (JetpackMount.installed(stack))
            return JetpackMount.thrust(stack, hover, player.isCreative());
        if (stack.getItem() instanceof FuelJetpack)
            return FuelJetpack.consume(stack, hover, player.isCreative());
        int cost = hover ? 7 : 8;
        if (stack.isEmpty() || stored(stack) < cost) return false;
        if (!player.isCreative()) setStored(stack, stored(stack) - cost);
        return true;
    }

    private static InteractionResult windReading(
            net.minecraft.world.entity.player.Player player, ItemStack meter, BlockPos pos) {
        int cost = PortableEquipment.WIND_METER.cost;
        if (player == null || !player.isCreative() && stored(meter) < cost) {
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer server) {
            var reading = WindMeter.read(server.getLevel(), pos);
            server.displayClientMessage(reading.message(), true);
            if (!reading.available()) return InteractionResult.FAIL;
            if (!player.isCreative()) spend(meter, cost);
        }
        return InteractionResult.SUCCESS;
    }

    /** Native item surface shared by portable items with an electrical store. */
    public static class PoweredItem extends Item {
        private final PortableEquipment device;

        /** Constructs one native electrical item. */
        public PoweredItem(PortableEquipment device, Properties properties) {
            super(properties);
            this.device = device;
        }

        @Override
        public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> output) {
            if (allowedIn(tab)) addCreativeStacks(this, output);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            return device == PortableEquipment.ELECTRIC_TREETAP
                    ? ForgeContent.electricTap(context, device.cost)
                    : super.useOn(context);
        }

        @Override
        public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
            if (device == PortableEquipment.WIND_METER) {
                return windReading(context.getPlayer(), stack, context.getClickedPos());
            }
            return InteractionResult.PASS;
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            return supports(state) && stored(stack) >= device.cost ? device.speed : 1;
        }

        @Override
        public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
            if (!supports(state) || stored(stack) < device.cost) return false;
            var tier =
                    switch (device) {
                        case DRILL -> Tiers.IRON;
                        case IRIDIUM_DRILL -> Tiers.NETHERITE;
                        default -> Tiers.DIAMOND;
                    };
            return TierSortingRegistry.isCorrectTierForDrops(tier, state);
        }

        @Override
        public boolean mineBlock(
                ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
            if (level instanceof ServerLevel server
                    && supports(state)
                    && device.cost > 0
                    && state.getDestroySpeed(level, pos) > 0
                    && !(miner instanceof ServerPlayer player && player.isCreative())) {
                double charge = stored(stack);
                if (charge >= device.cost) {
                    setStored(stack, charge - device.cost);
                    String sound =
                            device.kind == Kind.SAW
                                    ? (server.getRandom().nextBoolean()
                                            ? "item.chainsaw.use1"
                                            : "item.chainsaw.use2")
                                    : (state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                                            ? "item.drill.soft"
                                            : "item.drill.hard");
                    IndustrialSounds.play(server, pos, sound);
                }
            }
            return supports(state);
        }

        private static final AttributeModifier TOOL_SPEED =
                new AttributeModifier(
                        BASE_ATTACK_SPEED_UUID,
                        "Tool attack speed",
                        -2.4,
                        AttributeModifier.Operation.ADDITION);

        private AttributeModifier meleeDamage() {
            return new AttributeModifier(
                    BASE_ATTACK_DAMAGE_UUID,
                    "Tool damage",
                    device.poweredMeleeDamage() - 1,
                    AttributeModifier.Operation.ADDITION);
        }

        // The held tool can run out of charge before vanilla's next equipment refresh.
        private void refreshMelee(LivingEntity attacker, ItemStack stack) {
            if (device.poweredMeleeDamage() == 0) return;
            boolean charged = stored(stack) >= device.cost;
            refresh(attacker.getAttribute(Attributes.ATTACK_DAMAGE), meleeDamage(), charged);
            refresh(attacker.getAttribute(Attributes.ATTACK_SPEED), TOOL_SPEED, charged);
        }

        private static void refresh(
                AttributeInstance attribute, AttributeModifier desired, boolean charged) {
            if (attribute == null) return;
            var old = attribute.getModifier(desired.getId());
            if (old != null && charged && old.getAmount() == desired.getAmount()) return;
            if (old != null) attribute.removeModifier(old);
            if (charged) attribute.addTransientModifier(desired);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
                EquipmentSlot slot, ItemStack stack) {
            var base = super.getAttributeModifiers(slot, stack);
            if (slot != EquipmentSlot.MAINHAND
                    || device.poweredMeleeDamage() == 0
                    || stored(stack) < device.cost) return base;
            return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .putAll(base)
                    .put(Attributes.ATTACK_DAMAGE, meleeDamage())
                    .put(Attributes.ATTACK_SPEED, TOOL_SPEED)
                    .build();
        }

        private boolean supports(BlockState state) {
            return switch (device.kind) {
                case DRILL ->
                        state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                                || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
                case SAW -> state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.LEAVES);
                default -> false;
            };
        }

        @Override
        public EquipmentSlot getEquipmentSlot(ItemStack stack) {
            return device.wearable() ? EquipmentSlot.CHEST : super.getEquipmentSlot(stack);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return device.kind != Kind.DISPOSABLE && stored(stack) > 0
                    ? 1
                    : device.emptyStackSize();
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return device.kind != Kind.DISPOSABLE && stored(stack) > 0;
        }

        @Override
        public int getBarWidth(ItemStack stack) {
            return (int) Math.round(13 * stored(stack) / device.capacity);
        }

        @Override
        public int getBarColor(ItemStack stack) {
            return 0x58C7D5;
        }
    }
}
