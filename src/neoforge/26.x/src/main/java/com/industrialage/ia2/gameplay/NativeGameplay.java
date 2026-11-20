package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.platform.NeoForgeVersion;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/** Minecraft 26.x gameplay calls that differ from 1.21.1. */
/** Native gameplay operations for this Minecraft generation. */
public final class NativeGameplay {
    private NativeGameplay() {}

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> packetType(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("ic2", path));
    }

    static boolean allowLaserBreak(
            ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        return !NeoForge.EVENT_BUS
                .post(new BreakBlockEvent(level, pos, state, player))
                .isCanceled();
    }

    static void registerFlightFuel(IEventBus bus) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event ->
                        event.registerItem(
                                Capabilities.Fluid.ITEM,
                                (stack, access) ->
                                        new ItemAccessFluidHandler(
                                                access,
                                                FuelJetpack.fluid.get(),
                                                FuelJetpack.CAPACITY) {
                                            @Override
                                            public boolean isValid(
                                                    int index, FluidResource liquid) {
                                                return super.isValid(index, liquid)
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
        return blocks.registerBlock(path, factory, properties);
    }

    static <T extends Item> DeferredItem<T> item(
            DeferredRegister.Items items,
            String path,
            Function<Item.Properties, T> factory,
            Supplier<Item.Properties> properties) {
        return items.registerItem(path, factory, properties);
    }

    static <T extends BlockEntity> BlockEntityType<T> machineType(
            BiFunction<BlockPos, BlockState, T> factory, Block[] blocks) {
        return new BlockEntityType<>(factory::apply, blocks);
    }

    static ItemStack smeltingResult(ServerLevel level, ItemStack input) {
        var single = new SingleRecipeInput(input);
        return level.recipeAccess()
                .getRecipeFor(RecipeType.SMELTING, single, level)
                .map(recipe -> recipe.value().assemble(single))
                .orElse(ItemStack.EMPTY);
    }

    static ItemStack craftingRemainder(ItemStack stack) {
        var template = stack.getCraftingRemainder();
        return template == null ? ItemStack.EMPTY : template.create();
    }

    static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    static ItemStack stack(String id, int count) {
        return BuiltInRegistries.ITEM
                .getOptional(Identifier.parse(id))
                .map(item -> new ItemStack(item, count))
                .orElse(ItemStack.EMPTY);
    }

    /** Sends one local feedback message above the player's hotbar. */
    public static void overlay(ServerPlayer player, Component message) {
        player.sendOverlayMessage(message);
    }

    static void notifySignal(ServerLevel level, BlockPos pos, Block block) {
        level.updateNeighborsAt(pos, block, null);
    }

    static void registerAutomation(
            IEventBus bus, Supplier<BlockEntityType<IndustrialMachine>> type) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event ->
                        event.registerBlockEntity(
                                Capabilities.Item.BLOCK,
                                type.get(),
                                (machine, side) ->
                                        new WorldlyContainerWrapper(
                                                machine, side == null ? Direction.UP : side)));
    }

    static Block rubberLeaves(Block.Properties properties) {
        return new TintedParticleLeavesBlock(0.01F, properties);
    }

    static TreeGrower rubberTree() {
        var id =
                ResourceKey.<ConfiguredFeature<?, ?>>create(
                        Registries.CONFIGURED_FEATURE,
                        Identifier.fromNamespaceAndPath("ic2", "rubber_tree"));
        return new TreeGrower(
                "ic2:rubber_tree",
                java.util.Optional.empty(),
                java.util.Optional.of(id),
                java.util.Optional.empty());
    }

    static void bronzeGear(IEventBus bus, DeferredRegister.Items items) {
        var repair =
                TagKey.create(
                        Registries.ITEM, Identifier.fromNamespaceAndPath("ic2", "bronze_ingots"));
        var asset =
                ResourceKey.create(
                        EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("ic2", "bronze"));
        var bronze =
                new ArmorMaterial(
                        15,
                        Map.of(
                                ArmorType.HELMET,
                                2,
                                ArmorType.CHESTPLATE,
                                6,
                                ArmorType.LEGGINGS,
                                5,
                                ArmorType.BOOTS,
                                2),
                        9,
                        SoundEvents.ARMOR_EQUIP_IRON,
                        0,
                        0,
                        repair,
                        asset);
        for (var type :
                List.of(
                        ArmorType.HELMET,
                        ArmorType.CHESTPLATE,
                        ArmorType.LEGGINGS,
                        ArmorType.BOOTS)) {
            items.registerItem(
                    "bronze_" + type.getName(),
                    properties -> new Item(properties.humanoidArmor(bronze, type)));
        }
        var tier = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 350, 6, 2, 14, repair);
        items.registerItem(
                "bronze_pickaxe", properties -> new Item(properties.pickaxe(tier, 1, -2.8F)));
        items.registerItem("bronze_axe", properties -> new AxeItem(tier, 6, -3.1F, properties));
        items.registerItem(
                "bronze_shovel", properties -> new ShovelItem(tier, 1.5F, -3.0F, properties));
        items.registerItem("bronze_hoe", properties -> new HoeItem(tier, -2, -1.0F, properties));
        items.registerItem(
                "bronze_sword", properties -> new Item(properties.sword(tier, 3, -2.4F)));
    }

    static final class CraftingTool extends Item {
        CraftingTool(Properties properties) {
            super(properties);
        }

        @Override
        public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
            int damage = instance.getOrDefault(DataComponents.DAMAGE, 0) + 1;
            if (damage >= instance.getOrDefault(DataComponents.MAX_DAMAGE, 0)) {
                return null;
            }
            var patch =
                    switch (instance) {
                        case ItemStack stack -> stack.getComponentsPatch();
                        case ItemStackTemplate base -> base.components();
                        default -> DataComponentPatch.EMPTY;
                    };
            var remaining = new ItemStack(instance.typeHolder(), 1, patch);
            remaining.setDamageValue(damage);
            return ItemStackTemplate.fromNonEmptyStack(remaining);
        }
    }

    abstract static class InventoryEntity extends NeoForgeVersion.MachineEntity<MachineSave> {
        InventoryEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, MachineSave.FORMAT);
        }

        abstract NonNullList<ItemStack> persistentItems();

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            ContainerHelper.saveAllItems(output, persistentItems());
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            persistentItems().clear();
            ContainerHelper.loadAllItems(input, persistentItems());
            super.loadAdditional(input);
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);
            if (getLevel() instanceof ServerLevel level) {
                Containers.dropContents(level, pos, persistentItems());
            }
        }
    }

    abstract static class MachineBlockBase extends Block implements EntityBlock {
        MachineBlockBase(Properties properties) {
            super(properties);
        }

        abstract int analogOutput(Level level, BlockPos pos);

        @Override
        protected int getAnalogOutputSignal(
                BlockState state, Level level, BlockPos pos, Direction side) {
            return analogOutput(level, pos);
        }
    }

    abstract static class CableBlockBase extends Block {
        CableBlockBase(Properties properties) {
            super(properties);
        }

        abstract BlockState updateConnection(BlockState state, Direction side, BlockState neighbor);

        abstract void updatePower(Level level, BlockPos pos, BlockState state);

        @Override
        protected BlockState updateShape(
                BlockState state,
                LevelReader level,
                ScheduledTickAccess ticks,
                BlockPos pos,
                Direction side,
                BlockPos neighborPos,
                BlockState neighbor,
                RandomSource random) {
            return updateConnection(state, side, neighbor);
        }

        @Override
        protected void neighborChanged(
                BlockState state,
                Level level,
                BlockPos pos,
                Block block,
                Orientation orientation,
                boolean moving) {
            updatePower(level, pos, state);
        }

        @Override
        protected void affectNeighborsAfterRemoval(
                BlockState state, ServerLevel level, BlockPos pos, boolean moving) {
            super.affectNeighborsAfterRemoval(state, level, pos, moving);
            EnergyNetwork.invalidate(level);
        }
    }
}
