package com.industrialage.ia2.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;

/** Native calls whose signatures differ between supported Minecraft releases. */
final class WorldNative {
    private WorldNative() {}

    static Material materialFor(String name) {
        if (name.equals("reinforced_door")
                || name.equals("iron_fence")
                || name.equals("mining_pipe")
                || name.equals("mining_pipe_tip")) return Material.METAL;
        if (name.equals("reinforced_glass")) return Material.GLASS;
        if (name.equals("rubber_door")
                || name.equals("rubber_trapdoor")
                || name.startsWith("rubber_")) return Material.WOOD;
        if (name.endsWith("_ore") || name.endsWith("_block")) return Material.STONE;
        return Material.DECORATION;
    }

    static Block.Properties properties(String name) {
        return Block.Properties.of(materialFor(name));
    }

    static Block door(Block.Properties properties, boolean iron) {
        return new DoorBlock(properties);
    }

    static Block button(Block.Properties properties) {
        return new WoodButtonBlock(properties);
    }

    static Block trapdoor(Block.Properties properties) {
        return new TrapDoorBlock(properties);
    }

    static Block plate(Block.Properties properties) {
        return new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, properties);
    }

    static Block gate(Block.Properties properties) {
        return new FenceGateBlock(properties);
    }

    static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }

    static ResourceLocation own(String name) {
        return new ResourceLocation("ic2", name);
    }

    static Block block(String id) {
        return ForgeRegistries.BLOCKS.getValue(id(id));
    }

    static Item item(String id) {
        return ForgeRegistries.ITEMS.getValue(id(id));
    }

    static String blockId(Block block) {
        var id = ForgeRegistries.BLOCKS.getKey(block);
        return id == null ? "" : id.toString();
    }

    static String itemId(ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    static boolean sameStack(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameTags(first, second);
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
        stack.hurtAndBreak(amount, player, owner -> owner.broadcastBreakEvent(hand));
    }

    static Item.Properties packProperties(Item.Properties props) {
        return props.stacksTo(1);
    }

    static Block.Properties noCollision(Block.Properties properties) {
        return properties.noCollission();
    }

    static Block.Properties flammable(Block.Properties properties) {
        return properties;
    }

    static Block glass(Block.Properties properties) {
        return new GlassBlock(properties);
    }

    static Block ironFence(Block.Properties properties) {
        return new IronBarsBlock(properties);
    }

    static net.minecraft.tags.TagKey<Block> oreTag(String name) {
        return net.minecraft.tags.TagKey.create(
                net.minecraft.core.Registry.BLOCK_REGISTRY, id(name));
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
        return com.industrialage.ia2.forge.content.CableBlock.paint(level, pos, state, color);
    }

    static Item supply(String name, Item.Properties props) {
        if (name.endsWith("coffee_mug")) return coffee(props, CoffeeMug.Brew.named(name));
        if (name.equals("terra_wart")) {
            return new TerraWart(props.food(new FoodProperties.Builder().alwaysEat().build()));
        }
        return new Item(props);
    }

    static void registerTicketType(net.minecraftforge.eventbus.api.IEventBus bus) {}

    static boolean harmful(MobEffectInstance effect) {
        return effect.getEffect().getCategory() == MobEffectCategory.HARMFUL;
    }

    static MobEffectInstance coffeeEffect(LivingEntity consumer, CoffeeMug.Effect effect) {
        return consumer.getEffect(effectType(effect));
    }

    static void coffeeEffect(
            LivingEntity consumer, CoffeeMug.Effect effect, int ticks, int amplifier) {
        consumer.addEffect(new MobEffectInstance(effectType(effect), ticks, amplifier));
    }

    private static MobEffect effectType(CoffeeMug.Effect effect) {
        return switch (effect) {
            case SPEED -> MobEffects.MOVEMENT_SPEED;
            case HASTE -> MobEffects.DIG_SPEED;
            case NAUSEA -> MobEffects.CONFUSION;
            case HARM -> MobEffects.HARM;
        };
    }

    private static Item coffee(Item.Properties props, CoffeeMug.Brew brew) {
        var nutrition = new FoodProperties.Builder().alwaysEat().build();
        return new CoffeeMug(props.food(nutrition).stacksTo(1), brew) {
            @Override
            public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
                return net.minecraft.world.item.UseAnim.DRINK;
            }
        };
    }

    static void registerTools(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener(
                (net.minecraftforge.event.level.BlockEvent.BlockToolModificationEvent event) -> {
                    if (event.getToolAction() == net.minecraftforge.common.ToolActions.AXE_STRIP) {
                        var result = WorldInteractions.stripped(event.getState());
                        if (result != null) event.setFinalState(result);
                    }
                });
    }

    static boolean spend(ItemStack stack, double eu) {
        return com.industrialage.ia2.forge.equipment.ForgeGear.spend(stack, eu);
    }

    static double charge(ItemStack stack, double offer, int tier) {
        return com.industrialage.ia2.forge.equipment.ForgeGear.charge(stack, offer, tier);
    }

    static double stored(ItemStack stack) {
        return com.industrialage.ia2.forge.equipment.ForgeGear.stored(stack);
    }

    static boolean canDischarge(ItemStack stack, int tier) {
        return com.industrialage.ia2.forge.equipment.ForgeGear.canDischarge(stack, tier);
    }

    static double discharge(ItemStack stack, double room, double capacity, int tier) {
        return com.industrialage.ia2.forge.equipment.ForgeGear.discharge(
                stack, room, capacity, tier);
    }

    static double discharge(ItemStack stack, double available, int tier) {
        return com.industrialage.ia2.forge.equipment.ForgeGear.discharge(stack, available, tier);
    }

    static String genome(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getString("ic2_crop_genome") : "";
    }

    static void setGenome(ItemStack stack, String genome) {
        stack.getOrCreateTag().putString("ic2_crop_genome", genome);
    }

    static String upgradeConfig(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getString("ic2_upgrade_settings") : "";
    }

    static void setUpgradeConfig(ItemStack stack, String config) {
        stack.getOrCreateTag().putString("ic2_upgrade_settings", config);
    }

    static String sample(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getString("ic2_wall_sample") : "";
    }

    static void setSample(ItemStack stack, String sample) {
        stack.getOrCreateTag().putString("ic2_wall_sample", sample);
    }

    static int foam(ItemStack stack) {
        return stack.hasTag() ? Math.max(0, stack.getTag().getInt("ic2_foam_mb")) : 0;
    }

    static void setFoam(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt("ic2_foam_mb", amount);
    }

    static List<ItemStack> drops(
            ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        var loot =
                new LootContext.Builder(level)
                        .withParameter(
                                LootContextParams.ORIGIN,
                                net.minecraft.world.phys.Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.TOOL, tool);
        var tile = level.getBlockEntity(pos);
        if (tile != null) loot.withParameter(LootContextParams.BLOCK_ENTITY, tile);
        return state.getDrops(loot);
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
        public List<ItemStack> getDrops(BlockState state, LootContext.Builder loot) {
            var entity = loot.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            return CropPlot.broken(state, entity instanceof CropPlot plot ? plot : null);
        }
    }

    abstract static class MiningPipeBase extends Block implements SimpleWaterloggedBlock {
        MiningPipeBase(Properties settings) {
            super(settings);
        }

        @Override
        public BlockState updateShape(
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
