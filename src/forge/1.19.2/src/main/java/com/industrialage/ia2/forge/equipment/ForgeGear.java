package com.industrialage.ia2.forge.equipment;

import com.industrialage.ia2.audio.IndustrialSounds;
import com.industrialage.ia2.content.PortableEquipment;
import com.industrialage.ia2.content.PortableEquipment.Kind;
import com.industrialage.ia2.equipment.ArmorEffects;
import com.industrialage.ia2.equipment.ArmorPlatform;
import com.industrialage.ia2.equipment.JetpackMount;
import com.industrialage.ia2.equipment.MountPlatform;
import com.industrialage.ia2.equipment.WindMeter;
import com.industrialage.ia2.forge.content.ForgeContent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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
                                                ? ForgeContent.itemProperties()
                                                        .durability(
                                                                entry
                                                                                == PortableEquipment
                                                                                        .NANO_SABER
                                                                        ? 1_561
                                                                        : 250)
                                                : ForgeContent.itemProperties()
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
                                ForgeRegistries.ITEMS.getValue(
                                        new ResourceLocation("ic2", "bronze_ingot")));
                    }
                };
        ITEMS.register(
                "bronze_pickaxe",
                () -> new PickaxeItem(bronze, 1, -2.8F, ForgeContent.itemProperties()));
        ITEMS.register(
                "bronze_axe", () -> new AxeItem(bronze, 6, -3.1F, ForgeContent.itemProperties()));
        ITEMS.register(
                "bronze_shovel",
                () -> new ShovelItem(bronze, 1.5F, -3.0F, ForgeContent.itemProperties()));
        ITEMS.register(
                "bronze_hoe", () -> new HoeItem(bronze, -2, -1.0F, ForgeContent.itemProperties()));
        ITEMS.register(
                "bronze_sword",
                () -> new SwordItem(bronze, 3, -2.4F, ForgeContent.itemProperties()));
        ITEMS.register("jetpack", FuelJetpack::new);
        ArmorPlatform.install(ITEMS, gameBus);
        MountPlatform.install(ITEMS, modBus, gameBus);
        ITEMS.register(modBus);
        ForgeBoats.install(modBus);
        ForgeLaser.install(modBus);
        ForgeSaber.install(modBus, gameBus);
        JetpackControls.install(modBus, gameBus);
        gameBus.addListener(ForgeGear::tickPlayer);
        gameBus.addListener(ForgeGear::manualTransfer);
    }

    private static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        var equipment = type(chest);
        if (equipment != null && equipment.kind == Kind.PACK) {
            transfer(chest, player.getMainHandItem());
            transfer(chest, player.getOffhandItem());
        }
        var main = player.getMainHandItem();
        var off = player.getOffhandItem();
        if (type(main) != null && type(main).kind == Kind.CHARGER) transfer(main, off);
        if (type(off) != null && type(off).kind == Kind.CHARGER) transfer(off, main);
        ForgeSaber.tick(player, main);
        ForgeSaber.tick(player, off);
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
        var device = type(stack);
        if (device != null && device.kind == Kind.DISPOSABLE) {
            if (tier < device.tier || requested < device.capacity) return 0;
            stack.shrink(1);
            return device.capacity;
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

    static boolean boatPower(LivingEntity rider, boolean draw) {
        var pack = rider.getItemBySlot(EquipmentSlot.CHEST);
        var entry = type(pack);
        if (entry == null || entry.kind != Kind.PACK || stored(pack) < 4) return false;
        if (draw) setStored(pack, stored(pack) - 4);
        return true;
    }

    static boolean hasJetpack(LivingEntity player) {
        var pack = player.getItemBySlot(EquipmentSlot.CHEST);
        return type(pack) == PortableEquipment.ELECTRIC_JETPACK
                || pack.getItem() instanceof FuelJetpack
                || JetpackMount.installed(pack);
    }

    static boolean payForFlight(ServerPlayer player, boolean hover) {
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (JetpackMount.installed(stack))
            return JetpackMount.thrust(stack, hover, player.isCreative());
        if (stack.getItem() instanceof FuelJetpack)
            return FuelJetpack.consume(stack, hover, player.isCreative());
        int cost = hover ? 7 : 8;
        if (!hasJetpack(player) || stored(stack) < cost) return false;
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
            if (device != PortableEquipment.ELECTRIC_WRENCH) {
                return InteractionResult.PASS;
            }
            return ForgeContent.wrenchBlock(context, false);
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            return supports(state) && stored(stack) >= device.cost ? device.speed : 1;
        }

        @Override
        public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
            return supports(state)
                    && TierSortingRegistry.isCorrectTierForDrops(
                            device == PortableEquipment.DRILL ? Tiers.IRON : Tiers.DIAMOND, state);
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
