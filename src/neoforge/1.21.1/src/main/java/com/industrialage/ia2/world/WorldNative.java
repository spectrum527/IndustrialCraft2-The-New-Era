package com.industrialage.ia2.world;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
    private static final Map<String, Optional<TagKey<Block>>> BLOCK_TAGS =
            new ConcurrentHashMap<>();

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

    static ResourceLocation id(String value) {
        return ResourceLocation.parse(value);
    }

    static ResourceLocation own(String name) {
        return ResourceLocation.fromNamespaceAndPath("ic2", name);
    }

    static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(id(id));
    }

    static Item item(String id) {
        return BuiltInRegistries.ITEM.get(id(id));
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
        player.displayClientMessage(message, true);
    }

    static boolean client(Level level) {
        return level.isClientSide;
    }

    static int minY(Level level) {
        return level.getMinBuildHeight();
    }

    static void damage(
            ItemStack stack, int amount, Player player, net.minecraft.world.InteractionHand hand) {
        stack.hurtAndBreak(
                amount,
                player,
                hand == net.minecraft.world.InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND);
    }

    static Item.Properties packProperties(Item.Properties props) {
        return props.stacksTo(1);
    }

    static Block.Properties noCollision(Block.Properties properties) {
        return properties.noCollission();
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

    static TagKey<Block> oreTag(String name) {
        return blockTag(name)
                .orElseThrow(() -> new IllegalArgumentException("Invalid block tag: " + name));
    }

    private static Optional<TagKey<Block>> blockTag(String name) {
        return BLOCK_TAGS.computeIfAbsent(
                name,
                raw -> {
                    var loc = ResourceLocation.tryParse(raw);
                    return loc == null
                            ? Optional.empty()
                            : Optional.of(TagKey.create(Registries.BLOCK, loc));
                });
    }

    static boolean blockTagged(BlockState state, String tag) {
        return com.industrialage.ia2.content.ProcessingMachine.matchesTagAlias(
                tag, candidate -> blockTag(candidate).map(state::is).orElse(false));
    }

    static String customName(ItemStack stack) {
        var name = stack.get(DataComponents.CUSTOM_NAME);
        return name == null ? "" : name.getString();
    }

    static ItemStack namedTagVisual(String label) {
        var stack = new ItemStack(Items.NAME_TAG);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        return stack;
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

    static void registerTicketType(net.neoforged.bus.api.IEventBus bus) {}

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
            case SPEED -> MobEffects.MOVEMENT_SPEED;
            case HASTE -> MobEffects.DIG_SPEED;
            case NAUSEA -> MobEffects.CONFUSION;
            case HARM -> MobEffects.HARM;
        };
    }

    private static Item coffee(Item.Properties props, CoffeeMug.Brew brew) {
        var nutrition = new FoodProperties.Builder().alwaysEdible().build();
        return new CoffeeMug(props.food(nutrition).stacksTo(1), brew) {
            @Override
            public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
                return net.minecraft.world.item.UseAnim.DRINK;
            }
        };
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

    static void copyEnchantments(ItemStack from, ItemStack to) {
        var enchantments = from.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            to.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, enchantments);
        }
    }

    static <T extends BlockEntity> BlockEntityType<T> type(
            BlockEntityType.BlockEntitySupplier<T> factory, Block[] blocks) {
        return BlockEntityType.Builder.of(factory, blocks).build(null);
    }

    abstract static class CropBase extends Block implements EntityBlock {
        CropBase(Properties settings) {
            super(settings);
        }

        @Override
        public BlockState updateShape(
                BlockState state,
                Direction side,
                BlockState neighbor,
                LevelAccessor ticks,
                BlockPos pos,
                BlockPos neighborPos) {
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
                Direction side,
                BlockState neighbor,
                LevelAccessor level,
                BlockPos pos,
                BlockPos neighborPos) {
            if (state.getValue(BlockStateProperties.WATERLOGGED)) {
                level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
            return super.updateShape(state, side, neighbor, level, pos, neighborPos);
        }
    }

    abstract static class MachineBase extends Block implements EntityBlock {
        MachineBase(Properties settings) {
            super(settings);
        }

        @Override
        public void onRemove(
                BlockState state,
                Level level,
                BlockPos pos,
                BlockState replacement,
                boolean moving) {
            if (state.getBlock() != replacement.getBlock()
                    && level instanceof ServerLevel server
                    && level.getBlockEntity(pos) instanceof WorldMachine device) {
                net.minecraft.world.Containers.dropContents(server, pos, device.savedInventory());
            }
            super.onRemove(state, level, pos, replacement, moving);
        }
    }
}
