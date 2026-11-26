package com.industrialage.ia2.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Native calls whose signatures differ between supported Minecraft releases. */
final class WorldNative {
    private WorldNative() {}

    static Block.Properties properties() {
        return Block.Properties.of();
    }

    static Block door(Block.Properties properties, boolean iron) {
        return new DoorBlock(iron ? BlockSetType.IRON : BlockSetType.OAK, properties);
    }

    static Block button(Block.Properties properties) {
        return new ButtonBlock(BlockSetType.OAK, 30, properties);
    }

    static Block trapdoor(Block.Properties properties) {
        return new TrapDoorBlock(BlockSetType.OAK, properties);
    }

    static Block plate(Block.Properties properties) {
        return new PressurePlateBlock(BlockSetType.OAK, properties);
    }

    static Block gate(Block.Properties properties) {
        return new FenceGateBlock(WoodType.OAK, properties);
    }

    static Identifier id(String value) {
        return Identifier.parse(value);
    }

    static Identifier own(String name) {
        return Identifier.fromNamespaceAndPath("ic2", name);
    }

    static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(id(id));
    }

    static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(id(id));
    }

    static String blockId(Block block) {
        var id = BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? "" : id.toString();
    }

    static String itemId(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    static boolean sameStack(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameComponents(first, second);
    }

    static void reply(Player player, net.minecraft.network.chat.Component message) {
        if (player instanceof net.minecraft.server.level.ServerPlayer server)
            server.sendOverlayMessage(message);
    }

    static boolean client(Level level) {
        return level.isClientSide();
    }

    static int minY(Level level) {
        return level.getMinY();
    }

    static void damage(
            ItemStack stack, int amount, Player player, net.minecraft.world.InteractionHand hand) {
        stack.hurtAndBreak(amount, player, hand);
    }

    static Item.Properties packProperties(Item.Properties props) {
        return props.stacksTo(1).equippable(EquipmentSlot.CHEST);
    }

    static Block.Properties noCollision(Block.Properties properties) {
        return properties.noCollision();
    }

    static Block.Properties flammable(Block.Properties properties) {
        return properties.ignitedByLava();
    }

    static Block glass(Block.Properties properties) {
        return new TransparentBlock(properties);
    }

    static Block ironFence(Block.Properties properties) {
        return new IronBarsBlock(properties);
    }

    static net.minecraft.tags.TagKey<Block> oreTag(String name) {
        return net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.BLOCK, id(name));
    }

    static BlockState copySharedState(BlockState from, BlockState to) {
        for (var property : from.getProperties()) {
            if (to.hasProperty(property)) to = copyValue(from, to, property);
        }
        return to;
    }

    private static <T extends Comparable<T>> BlockState copyValue(
            BlockState from,
            BlockState to,
            net.minecraft.world.level.block.state.properties.Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    static boolean paintCable(Level level, BlockPos pos, BlockState state, int color) {
        return com.industrialage.ia2.gameplay.CableBlock.paint(level, pos, state, color);
    }

    static Item supply(String name, Item.Properties props) {
        if (name.endsWith("coffee_mug")) return coffee(props, CoffeeMug.Brew.named(name));
        if (name.equals("terra_wart")) {
            return new TerraWart(props.food(new FoodProperties.Builder().alwaysEdible().build()));
        }
        return new Item(props);
    }

    static void registerTicketType(net.neoforged.bus.api.IEventBus bus) {
        WorldChunkTickets.install(bus);
    }

    static boolean harmful(MobEffectInstance effect) {
        return effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
    }

    static MobEffectInstance coffeeEffect(LivingEntity consumer, CoffeeMug.Effect effect) {
        return consumer.getEffect(effectType(effect));
    }

    static void coffeeEffect(
            LivingEntity consumer, CoffeeMug.Effect effect, int ticks, int amplifier) {
        consumer.addEffect(new MobEffectInstance(effectType(effect), ticks, amplifier));
    }

    private static Holder<MobEffect> effectType(CoffeeMug.Effect effect) {
        return switch (effect) {
            case SPEED -> MobEffects.SPEED;
            case HASTE -> MobEffects.HASTE;
            case NAUSEA -> MobEffects.NAUSEA;
            case HARM -> MobEffects.INSTANT_DAMAGE;
        };
    }

    private static Item coffee(Item.Properties props, CoffeeMug.Brew brew) {
        var nutrition = new FoodProperties.Builder().alwaysEdible().build();
        return new CoffeeMug(
                props.food(nutrition, net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)
                        .stacksTo(1),
                brew);
    }

    static void registerTools(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener(
                net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent.class,
                event -> {
                    if (event.getItemAbility()
                            == net.neoforged.neoforge.common.ItemAbilities.AXE_STRIP) {
                        var result = WorldInteractions.stripped(event.getState());
                        if (result != null) event.setFinalState(result);
                    }
                });
    }

    static boolean spend(ItemStack stack, double eu) {
        return com.industrialage.ia2.gameplay.PortableGear.spend(stack, eu);
    }

    static double charge(ItemStack stack, double offer, int tier) {
        return com.industrialage.ia2.gameplay.PortableGear.charge(stack, offer, tier);
    }

    static double stored(ItemStack stack) {
        return com.industrialage.ia2.gameplay.PortableGear.stored(stack);
    }

    static boolean canDischarge(ItemStack stack, int tier) {
        return com.industrialage.ia2.gameplay.PortableGear.canDischarge(stack, tier);
    }

    static double discharge(ItemStack stack, double room, double capacity, int tier) {
        return com.industrialage.ia2.gameplay.PortableGear.discharge(stack, room, capacity, tier);
    }

    static double discharge(ItemStack stack, double available, int tier) {
        return com.industrialage.ia2.gameplay.PortableGear.discharge(stack, available, tier);
    }

    static String genome(ItemStack stack) {
        return WorldPlatform.genome(stack);
    }

    static void setGenome(ItemStack stack, String genome) {
        WorldPlatform.setGenome(stack, genome);
    }

    static String upgradeConfig(ItemStack stack) {
        return WorldPlatform.upgradeConfig(stack);
    }

    static void setUpgradeConfig(ItemStack stack, String config) {
        WorldPlatform.setUpgradeConfig(stack, config);
    }

    static String sample(ItemStack stack) {
        return WorldPlatform.sample(stack);
    }

    static void setSample(ItemStack stack, String sample) {
        WorldPlatform.setSample(stack, sample);
    }

    static int foam(ItemStack stack) {
        return WorldPlatform.foam(stack);
    }

    static void setFoam(ItemStack stack, int amount) {
        WorldPlatform.setFoam(stack, amount);
    }

    static List<ItemStack> drops(
            ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        return Block.getDrops(state, level, pos, level.getBlockEntity(pos), null, tool);
    }

    static <T extends BlockEntity> BlockEntityType<T> type(
            BlockEntityType.BlockEntitySupplier<T> factory, Block[] blocks) {
        return new BlockEntityType<>(factory, blocks);
    }

    abstract static class CropBase extends Block implements EntityBlock {
        CropBase(Properties settings) {
            super(settings);
        }

        @Override
        public BlockState updateShape(
                BlockState state,
                LevelReader level,
                net.minecraft.world.level.ScheduledTickAccess ticks,
                BlockPos pos,
                Direction side,
                BlockPos neighborPos,
                BlockState neighbor,
                RandomSource random) {
            if (side == Direction.DOWN && !neighbor.is(Blocks.FARMLAND))
                ticks.scheduleTick(pos, this, 1);
            return state;
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder loot) {
            var entity = loot.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            return CropPlot.broken(state, entity instanceof CropPlot plot ? plot : null);
        }
    }

    abstract static class MiningPipeBase extends Block implements SimpleWaterloggedBlock {
        MiningPipeBase(Properties settings) {
            super(settings);
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                LevelReader level,
                net.minecraft.world.level.ScheduledTickAccess ticks,
                BlockPos pos,
                Direction side,
                BlockPos neighborPos,
                BlockState neighbor,
                RandomSource random) {
            if (state.getValue(BlockStateProperties.WATERLOGGED)) {
                ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
            return super.updateShape(state, level, ticks, pos, side, neighborPos, neighbor, random);
        }
    }

    abstract static class MachineBase extends Block implements EntityBlock {
        MachineBase(Properties settings) {
            super(settings);
        }
    }
}
