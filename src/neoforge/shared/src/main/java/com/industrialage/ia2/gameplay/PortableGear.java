package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.audio.IndustrialSounds;
import com.industrialage.ia2.compat.CuriosBack;
import com.industrialage.ia2.content.PortableEquipment;
import com.industrialage.ia2.content.PortableEquipment.Kind;
import com.industrialage.ia2.equipment.ArmorEffects;
import com.industrialage.ia2.equipment.JetpackMount;
import com.industrialage.ia2.equipment.WindMeter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** EU-bearing items, native mining, and direct charge transfers. */
public final class PortableGear {
    private PortableGear() {}

    static void install(DeferredRegister.Items items, IEventBus gameBus) {
        for (var device : PortableEquipment.CATALOG) {
            if (device.kind == Kind.ARMOR) continue;
            items.registerItem(
                    device.path,
                    properties ->
                            new PoweredItem(
                                    device,
                                    device.kind == Kind.TOOL
                                            ? properties.durability(
                                                    device == PortableEquipment.NANO_SABER
                                                            ? 1_561
                                                            : 250)
                                            : properties.stacksTo(device.emptyStackSize())));
        }
        gameBus.addListener(
                PlayerTickEvent.Post.class,
                event -> {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        transferFromPack(player);
                        var inv = player.getInventory();
                        for (int i = 0; i < inv.getContainerSize(); i++) {
                            var item = inv.getItem(i);
                            if (NeoSaber.active(item)) NeoSaber.tick(player, item);
                        }
                    }
                });
        ToolMelee.install(gameBus);
        gameBus.addListener(
                net.neoforged.neoforge.event.entity.player.ItemTooltipEvent.class,
                NeoLaser::tooltip);
        gameBus.addListener(PlayerInteractEvent.RightClickItem.class, PortableGear::manualTransfer);
    }

    private static void manualTransfer(PlayerInteractEvent.RightClickItem event) {
        var source = event.getItemStack();
        if (type(source) == PortableEquipment.NANO_SABER) {
            NeoSaber.use(event);
            return;
        }
        if (type(source) == PortableEquipment.MINING_LASER) {
            NeoLaser.use(event);
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
                player.setItemSlot(EquipmentSlot.CHEST, source.copyWithCount(1));
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
        if (player.level() instanceof ServerLevel level) {
            double before = stored(target);
            transfer(source, target);
            if (stored(target) > before)
                IndustrialSounds.play(level, player.blockPosition(), "item.battery.use");
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void transferFromPack(ServerPlayer player) {
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

    private static void transfer(ItemStack source, ItemStack target) {
        var provider = type(source);
        var consumer = type(target);
        if (provider != null && consumer != null && !consumer.canProvide()) {
            if (provider.kind == Kind.DISPOSABLE) {
                double room = consumer.capacity - stored(target);
                if (provider.tier >= consumer.tier && room >= provider.capacity) {
                    setStored(target, stored(target) + provider.capacity);
                    source.shrink(1);
                }
                return;
            }
            double accepted =
                    charge(target, Math.min(provider.rate, stored(source)), provider.tier);
            if (accepted > 0) {
                setStored(source, stored(source) - accepted);
            }
        }
    }

    /** Stack limit in a powered equipment slot, including stacks of single-use batteries. */
    public static int powerSlotLimit(ItemStack stack) {
        return type(stack) == PortableEquipment.SINGLE_USE_BATTERY || !electricalItem(stack)
                ? stack.getMaxStackSize()
                : 1;
    }

    /** Whether a charging slot of this tier can charge this live stack. */
    public static boolean acceptsCharge(ItemStack stack, int tier) {
        var device = type(stack);
        return (device != null && device.tier <= tier && device.kind != Kind.DISPOSABLE)
                || tier >= 1 && JetpackMount.installed(stack);
    }

    /** Whether a discharge slot of this tier can take power from the stack. */
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

    /** Supplies the platform electrical stack adapter for charge. */
    public static double charge(ItemStack stack, double offer, int tier) {
        var device = type(stack);
        if (stack.getCount() != 1) {
            return 0;
        }
        if (device == null) return JetpackMount.charge(stack, offer, tier);
        double prior = stored(stack);
        double accepted = device.chargeAmount(offer, prior, tier);
        if (accepted > 0) {
            setStored(stack, prior + accepted);
        }
        return accepted + JetpackMount.charge(stack, offer - accepted, tier);
    }

    /** Supplies the platform electrical stack adapter for discharge. */
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
        if (device == null || stack.getCount() != 1) {
            return 0;
        }
        double prior = stored(stack);
        double extracted = device.dischargeAmount(requested, prior, tier);
        if (extracted > 0) {
            setStored(stack, prior - extracted);
        }
        return extracted;
    }

    static PortableEquipment type(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return stack.getItem() instanceof PoweredItem item
                ? item.device
                : ArmorEffects.electric(stack);
    }

    /** Supplies the platform electrical stack adapter for stored. */
    public static double stored(ItemStack stack) {
        var device = type(stack);
        if (device != null && device.kind == Kind.DISPOSABLE) return device.capacity;
        return device == null
                ? JetpackMount.energy(stack)
                : device.stored(stack.getOrDefault(IndustrialContent.EU.get(), 0.0));
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
        if (amount > 0) {
            stack.set(IndustrialContent.EU.get(), amount);
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        } else {
            stack.remove(IndustrialContent.EU.get());
            stack.set(DataComponents.MAX_STACK_SIZE, type(stack).emptyStackSize());
        }
    }

    /** Restores a transferred portable item's original charge. */
    public static void restoreEnergy(ItemStack stack, double amount) {
        var device = type(stack);
        if (device != null) setStored(stack, device.stored(amount));
    }

    static boolean electricalItem(ItemStack stack) {
        return type(stack) != null || JetpackMount.installed(stack);
    }

    private static boolean isPack(ItemStack stack) {
        var device = type(stack);
        return device != null && device.kind == Kind.PACK;
    }

    /** Live pack providing electric-boat thrust, or the fitted pack when its charge is low. */
    public static ItemStack boatPack(LivingEntity rider) {
        var working = CuriosBack.preferChest(rider, stack -> isPack(stack) && stored(stack) >= 4);
        return working.isEmpty() ? CuriosBack.preferChest(rider, PortableGear::isPack) : working;
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
        if (player instanceof ServerPlayer server && player.level() instanceof ServerLevel level) {
            var reading = WindMeter.read(level, pos);
            NativeGameplay.overlay(server, reading.message());
            if (!reading.available()) return InteractionResult.FAIL;
            if (!player.isCreative()) spend(meter, cost);
        }
        return InteractionResult.SUCCESS;
    }

    private static final class PoweredItem extends Item {
        private final PortableEquipment device;

        PoweredItem(PortableEquipment device, Properties properties) {
            super(properties);
            this.device = device;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            return device == PortableEquipment.ELECTRIC_TREETAP
                    ? RubberLogBlock.Treetap.tap(context, device.cost)
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
            var incorrect =
                    switch (device) {
                        case DRILL -> BlockTags.INCORRECT_FOR_IRON_TOOL;
                        case IRIDIUM_DRILL -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
                        default -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
                    };
            return !state.is(incorrect);
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
