package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.access.MachinePorts;
import com.industrialage.ia2.access.ProcessingItemPort;
import com.industrialage.ia2.platform.NeoForgeVersion;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Minecraft 1.21.1 gameplay calls that differ from 26.x. */
/** Native gameplay operations for this Minecraft generation. */
public final class NativeGameplay {
    private NativeGameplay() {}

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> packetType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("ic2", path));
    }

    static boolean allowLaserBreak(
            ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        return !NeoForge.EVENT_BUS
                .post(new BlockEvent.BreakEvent(level, pos, state, player))
                .isCanceled();
    }

    static void registerFlightFuel(IEventBus bus) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event ->
                        event.registerItem(
                                Capabilities.FluidHandler.ITEM,
                                (stack, ignored) ->
                                        new FluidHandlerItemStack(
                                                FuelJetpack.fluid, stack, FuelJetpack.CAPACITY) {
                                            @Override
                                            public boolean canFillFluidType(FluidStack liquid) {
                                                return super.canFillFluidType(liquid)
                                                        && FuelJetpack.accepts(liquid.getFluid());
                                            }
                                        },
                                FuelJetpack.item.get()));
    }

    static <T extends Block> DeferredBlock<T> block(
            DeferredRegister.Blocks blocks,
            String path,
            Function<Block.Properties, T> factory,
            Supplier<Block.Properties> properties) {
        return blocks.registerBlock(path, factory, properties.get());
    }

    static <T extends Item> DeferredItem<T> item(
            DeferredRegister.Items items,
            String path,
            Function<Item.Properties, T> factory,
            Supplier<Item.Properties> properties) {
        return items.registerItem(path, factory, properties.get());
    }

    static <T extends BlockEntity> BlockEntityType<T> machineType(
            BiFunction<BlockPos, BlockState, T> factory, Block[] blocks) {
        return BlockEntityType.Builder.of(factory::apply, blocks).build(null);
    }

    static ItemStack smeltingResult(ServerLevel level, ItemStack input) {
        var single = new SingleRecipeInput(input);
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, single, level)
                .map(recipe -> recipe.value().assemble(single, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    static ItemStack craftingRemainder(ItemStack stack) {
        return stack.getCraftingRemainingItem();
    }

    static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    static ItemStack stack(String id, int count) {
        return BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.parse(id))
                .map(item -> new ItemStack(item, count))
                .orElse(ItemStack.EMPTY);
    }

    /** Sends one local feedback message above the player's hotbar. */
    public static void overlay(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }

    static void notifySignal(ServerLevel level, BlockPos pos, Block block) {
        level.updateNeighborsAt(pos, block);
    }

    static void registerAutomation(
            IEventBus bus, Supplier<BlockEntityType<IndustrialMachine>> type) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event ->
                        event.registerBlockEntity(
                                Capabilities.ItemHandler.BLOCK,
                                type.get(),
                                (machine, side) ->
                                        MachineAccess.connects(machine, side)
                                                ? MachinePorts.item(
                                                        machine,
                                                        side,
                                                        new ProcessingItemPort(machine, side))
                                                : null));
    }

    static Block rubberLeaves(Block.Properties properties) {
        return new LeavesBlock(properties);
    }

    static TreeGrower rubberTree() {
        var id =
                ResourceKey.<ConfiguredFeature<?, ?>>create(
                        Registries.CONFIGURED_FEATURE,
                        ResourceLocation.fromNamespaceAndPath("ic2", "rubber_tree"));
        return new TreeGrower(
                "ic2:rubber_tree",
                java.util.Optional.empty(),
                java.util.Optional.of(id),
                java.util.Optional.empty());
    }

    static void bronzeGear(IEventBus bus, DeferredRegister.Items items) {
        var materials = DeferredRegister.create(Registries.ARMOR_MATERIAL, "ic2");
        var bronze =
                materials.register(
                        "bronze",
                        () ->
                                new ArmorMaterial(
                                        Map.of(
                                                ArmorItem.Type.HELMET,
                                                2,
                                                ArmorItem.Type.CHESTPLATE,
                                                6,
                                                ArmorItem.Type.LEGGINGS,
                                                5,
                                                ArmorItem.Type.BOOTS,
                                                2),
                                        9,
                                        SoundEvents.ARMOR_EQUIP_IRON,
                                        () -> Ingredient.of(stack("ic2:bronze_ingot", 1)),
                                        List.of(
                                                new ArmorMaterial.Layer(
                                                        ResourceLocation.fromNamespaceAndPath(
                                                                "ic2", "bronze"))),
                                        0,
                                        0));
        materials.register(bus);
        for (var type :
                List.of(
                        ArmorItem.Type.HELMET,
                        ArmorItem.Type.CHESTPLATE,
                        ArmorItem.Type.LEGGINGS,
                        ArmorItem.Type.BOOTS)) {
            items.registerItem(
                    "bronze_" + type.getName(),
                    properties -> new ArmorItem(bronze, type, properties),
                    new Item.Properties().durability(type.getDurability(15)));
        }
        Tier tier =
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
                    public net.minecraft.tags.TagKey<Block> getIncorrectBlocksForDrops() {
                        return BlockTags.INCORRECT_FOR_IRON_TOOL;
                    }

                    @Override
                    public int getEnchantmentValue() {
                        return 14;
                    }

                    @Override
                    public Ingredient getRepairIngredient() {
                        return Ingredient.of(stack("ic2:bronze_ingot", 1));
                    }
                };
        items.registerItem(
                "bronze_pickaxe",
                properties ->
                        new PickaxeItem(
                                tier,
                                properties.attributes(
                                        PickaxeItem.createAttributes(tier, 1, -2.8F))));
        items.registerItem(
                "bronze_axe",
                properties ->
                        new AxeItem(
                                tier,
                                properties.attributes(
                                        PickaxeItem.createAttributes(tier, 6, -3.1F))));
        items.registerItem(
                "bronze_shovel",
                properties ->
                        new ShovelItem(
                                tier,
                                properties.attributes(
                                        PickaxeItem.createAttributes(tier, 1.5F, -3.0F))));
        items.registerItem(
                "bronze_hoe",
                properties ->
                        new HoeItem(
                                tier,
                                properties.attributes(
                                        PickaxeItem.createAttributes(tier, -2, -1.0F))));
        items.registerItem(
                "bronze_sword",
                properties ->
                        new SwordItem(
                                tier,
                                properties.attributes(SwordItem.createAttributes(tier, 3, -2.4F))));
    }

    static final class CraftingTool extends Item {
        CraftingTool(Properties properties) {
            super(properties);
        }

        @Override
        public boolean hasCraftingRemainingItem(ItemStack stack) {
            return stack.getDamageValue() + 1 < stack.getMaxDamage();
        }

        @Override
        public ItemStack getCraftingRemainingItem(ItemStack stack) {
            if (!hasCraftingRemainingItem(stack)) {
                return ItemStack.EMPTY;
            }
            ItemStack remaining = stack.copyWithCount(1);
            remaining.setDamageValue(stack.getDamageValue() + 1);
            return remaining;
        }
    }

    abstract static class InventoryEntity extends NeoForgeVersion.MachineEntity<MachineSave> {
        InventoryEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, MachineSave.FORMAT);
        }

        abstract NonNullList<ItemStack> persistentItems();

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            ContainerHelper.saveAllItems(tag, persistentItems(), registries);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            persistentItems().clear();
            ContainerHelper.loadAllItems(tag, persistentItems(), registries);
            super.loadAdditional(tag, registries);
        }
    }

    abstract static class MachineBlockBase extends Block implements EntityBlock {
        MachineBlockBase(Properties properties) {
            super(properties);
        }

        abstract int analogOutput(Level level, BlockPos pos);

        @Override
        protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
            return analogOutput(level, pos);
        }

        @Override
        protected void onRemove(
                BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
            if (state.getBlock() != next.getBlock()
                    && level instanceof ServerLevel
                    && level.getBlockEntity(pos) instanceof IndustrialMachine machine) {
                Containers.dropContents(level, pos, machine);
            }
            super.onRemove(state, level, pos, next, moving);
        }
    }

    static float healthDamage(
            net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        return event.getNewDamage();
    }

    abstract static class CableBlockBase extends Block {
        CableBlockBase(Properties properties) {
            super(properties);
        }

        abstract BlockState updateConnection(BlockState state, Direction side, BlockState neighbor);

        BlockState updateWorldConnection(
                net.minecraft.world.level.BlockGetter level,
                BlockPos pos,
                BlockState state,
                Direction side,
                BlockPos neighborPos,
                BlockState neighbor) {
            return updateConnection(state, side, neighbor);
        }

        abstract void updatePower(Level level, BlockPos pos, BlockState state);

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
            return updateWorldConnection(level, pos, state, side, neighborPos, neighbor);
        }

        @Override
        protected void neighborChanged(
                BlockState state,
                Level level,
                BlockPos pos,
                Block block,
                BlockPos neighbor,
                boolean moving) {
            updatePower(level, pos, state);
        }

        @Override
        protected void onRemove(
                BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
            super.onRemove(state, level, pos, next, moving);
            if (level instanceof ServerLevel server && state.getBlock() != next.getBlock()) {
                EnergyNetwork.invalidate(server);
            }
        }
    }
}
