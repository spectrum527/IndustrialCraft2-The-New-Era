package com.industrialage.ia2.industrial;

import com.industrialage.ia2.platform.ConfigFacade;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the electrical generating devices and the parts fitted by players. */
public final class PowerPlatform {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("ic2");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ic2");
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "ic2");
    private static final DeferredRegister<DataComponentType<?>> DATA =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "ic2");
    private static final java.util.function.Supplier<DataComponentType<Integer>> USE =
            DATA.register(
                    "reactor_use",
                    () ->
                            DataComponentType.<Integer>builder()
                                    .persistent(Codec.intRange(0, Integer.MAX_VALUE))
                                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                                    .build());
    private static final List<DeferredBlock<PowerBlock>> DEVICES = new ArrayList<>();
    private static final java.util.function.Supplier<BlockEntityType<PowerNode>> TYPE =
            TYPES.register(
                    "power_device",
                    () ->
                            PowerNative.type(
                                    DEVICES.stream()
                                            .map(DeferredBlock::get)
                                            .toArray(Block[]::new)));

    private static final java.util.function.Supplier<DataComponentType<String>> LINK =
            DATA.register(
                    "frequency_link",
                    () ->
                            DataComponentType.<String>builder()
                                    .persistent(Codec.STRING)
                                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                                    .build());

    private PowerPlatform() {}

    /** Installs the physical blocks and item types for this game version. */
    public static void install(IEventBus bus, ConfigFacade config) {
        PowerSettings.config = config;
        for (var kind : PowerKind.values()) {
            var block =
                    BLOCKS.registerBlock(
                            kind.path,
                            p ->
                                    new PowerBlock(
                                            p.strength(2, 10)
                                                    .sound(SoundType.METAL)
                                                    .requiresCorrectToolForDrops(),
                                            kind));
            DEVICES.add(block);
            ITEMS.registerSimpleBlockItem(block);
        }
        for (String path : PowerKind.PARTS)
            ITEMS.registerItem(path, p -> new Item(path.equals("rtg_pellet") ? p.stacksTo(1) : p));
        ITEMS.registerItem("debug_item", DebugItem::new);
        PowerMenus.install(bus);
        ChargeTypes.install(bus);
        DATA.register(bus);
        PowerNative.automation(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TYPES.register(bus);
    }

    static boolean acceptsCharge(ItemStack stack, int tier) {
        return com.industrialage.ia2.gameplay.PortableGear.acceptsCharge(stack, tier);
    }

    static boolean canDischarge(ItemStack stack, int tier) {
        return com.industrialage.ia2.gameplay.PortableGear.canDischarge(stack, tier);
    }

    static double discharge(ItemStack stack, double room, double capacity, int tier) {
        return com.industrialage.ia2.gameplay.PortableGear.discharge(stack, room, capacity, tier);
    }

    static int powerSlotLimit(ItemStack stack) {
        return com.industrialage.ia2.gameplay.PortableGear.powerSlotLimit(stack);
    }

    static double charge(ItemStack stack, double energy, int tier) {
        return com.industrialage.ia2.gameplay.PortableGear.charge(stack, energy, tier);
    }

    static String blockId(net.minecraft.world.level.block.state.BlockState state) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(state.getBlock())
                .toString();
    }

    static String dimension(ServerLevel level) {
        return com.industrialage.ia2.platform.NeoForgeVersion.dimension(level);
    }

    static String link(ItemStack stack) {
        return stack.getOrDefault(LINK.get(), "");
    }

    static void link(ItemStack stack, String save) {
        if (save.isEmpty()) stack.remove(LINK.get());
        else stack.set(LINK.get(), save);
    }

    static boolean vehicle(net.minecraft.world.entity.Entity entity) {
        var key =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(
                        entity.getType());
        String path = key == null ? "" : key.getPath();
        return path.contains("minecart") || path.endsWith("boat") || path.endsWith("raft");
    }

    static boolean shock(
            ServerLevel level, net.minecraft.world.entity.LivingEntity victim, int damage) {
        return PowerNative.shock(level, victim, damage);
    }

    static String path(ItemStack stack) {
        String id = WorkshopPlatform.itemId(stack);
        return id.startsWith("ic2:") ? id.substring(4) : "";
    }

    static ItemStack item(String path) {
        return WorkshopPlatform.stack("ic2:" + path, 1);
    }

    static int use(ItemStack stack) {
        return stack.getOrDefault(USE.get(), 0);
    }

    static void setUse(ItemStack stack, int value) {
        stack.set(USE.get(), value);
    }

    static BlockEntityType<PowerNode> type() {
        return TYPE.get();
    }

    static void meltdown(ServerLevel level, BlockPos pos, float power) {
        float limit = PowerSettings.explosionLimit();
        if (limit <= 0) return;
        level.removeBlock(pos, false);
        level.explode(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                Math.min(power, limit),
                Level.ExplosionInteraction.BLOCK);
    }
}
