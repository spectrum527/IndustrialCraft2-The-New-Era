package com.industrialage.ia2.industrial;

import com.industrialage.ia2.forge.content.ForgeContent;
import com.industrialage.ia2.platform.ConfigFacade;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registers the electrical generating devices and the parts fitted by players. */
public final class PowerPlatform {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "ic2");
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "ic2");
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "ic2");
    private static final List<RegistryObject<PowerBlock>> DEVICES = new ArrayList<>();
    private static final RegistryObject<BlockEntityType<PowerNode>> TYPE =
            TYPES.register(
                    "power_device",
                    () ->
                            BlockEntityType.Builder.of(
                                            PowerNode::new,
                                            DEVICES.stream()
                                                    .map(RegistryObject::get)
                                                    .toArray(Block[]::new))
                                    .build(null));

    private PowerPlatform() {}

    /** Installs the physical blocks and item types for this game version. */
    public static void install(IEventBus bus, ConfigFacade config) {
        PowerSettings.config = config;
        for (var kind : PowerKind.values()) {
            var block =
                    BLOCKS.register(
                            kind.path,
                            () ->
                                    new PowerBlock(
                                            BlockBehaviour.Properties.of(Material.METAL)
                                                    .strength(2, 10)
                                                    .sound(SoundType.METAL)
                                                    .requiresCorrectToolForDrops(),
                                            kind));
            DEVICES.add(block);
            ITEMS.register(
                    kind.path, () -> new BlockItem(block.get(), ForgeContent.itemProperties()));
        }
        for (String path : PowerKind.PARTS)
            ITEMS.register(
                    path,
                    () ->
                            new Item(
                                    path.equals("rtg_pellet")
                                            ? ForgeContent.itemProperties().stacksTo(1)
                                            : ForgeContent.itemProperties()));
        ITEMS.register("debug_item", () -> new DebugItem(ForgeContent.itemProperties()));
        PowerMenus.install(bus);
        ChargeTypes.install(bus);
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist
                == net.minecraftforge.api.distmarker.Dist.CLIENT) ChargeTypes.installRenderer(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TYPES.register(bus);
    }

    static boolean acceptsCharge(ItemStack stack, int tier) {
        return com.industrialage.ia2.forge.equipment.ForgeGear.acceptsCharge(stack, tier);
    }

    static boolean canDischarge(ItemStack stack, int tier) {
        return com.industrialage.ia2.forge.equipment.ForgeGear.canDischarge(stack, tier);
    }

    static double discharge(ItemStack stack, double energy, int tier) {
        return com.industrialage.ia2.forge.equipment.ForgeGear.discharge(stack, energy, tier);
    }

    static double charge(ItemStack stack, double energy, int tier) {
        return com.industrialage.ia2.forge.equipment.ForgeGear.charge(stack, energy, tier);
    }

    static String blockId(net.minecraft.world.level.block.state.BlockState state) {
        return ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
    }

    static String dimension(ServerLevel level) {
        return level.dimension().location().toString();
    }

    static String link(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getString("ic2_frequency_link") : "";
    }

    static void link(ItemStack stack, String value) {
        if (value.isEmpty()) stack.removeTagKey("ic2_frequency_link");
        else stack.getOrCreateTag().putString("ic2_frequency_link", value);
    }

    static boolean vehicle(net.minecraft.world.entity.Entity entity) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        String path = key == null ? "" : key.getPath();
        return path.contains("minecart") || path.endsWith("boat") || path.endsWith("raft");
    }

    static boolean shock(
            ServerLevel level, net.minecraft.world.entity.LivingEntity victim, int damage) {
        return victim.hurt(net.minecraft.world.damagesource.DamageSource.MAGIC, damage);
    }

    static String path(ItemStack stack) {
        String id = WorkshopPlatform.itemId(stack);
        return id.startsWith("ic2:") ? id.substring(4) : "";
    }

    static ItemStack item(String path) {
        return WorkshopPlatform.stack("ic2:" + path, 1);
    }

    static int use(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("ic2_reactor_use") : 0;
    }

    static void setUse(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt("ic2_reactor_use", value);
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
                Explosion.BlockInteraction.DESTROY);
    }
}
