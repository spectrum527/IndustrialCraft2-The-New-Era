package com.industrialage.ia2.industrial;

import com.industrialage.ia2.industrial.WorkshopPlans.ContainerResult;
import com.industrialage.ia2.industrial.WorkshopPlans.Liquid;
import com.industrialage.ia2.logistics.TransportPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStackSimple;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

final class WorkshopNative {
    private WorkshopNative() {}

    static BlockEntityType<WorkshopNode> type(Block[] blocks) {
        return BlockEntityType.Builder.of(WorkshopNode::new, blocks).build(null);
    }

    static DeferredBlock<WorkshopBlock> block(DeferredRegister.Blocks blocks, WorkshopKind kind) {
        return blocks.registerBlock(
                kind.path,
                props -> WorkshopBlock.create(props, kind),
                WorkshopPlatform.properties());
    }

    static DeferredBlock<Block> advancedCasing(DeferredRegister.Blocks blocks) {
        return blocks.registerBlock("advanced_machine", Block::new, WorkshopPlatform.properties());
    }

    static String crystalPattern(CompoundTag data) {
        return data.getString("ic2_pattern");
    }

    static int fuelCanValue(CompoundTag data) {
        return Math.max(0, data.getInt("value"));
    }

    static void unstackable(DeferredRegister.Items items, String path) {
        items.registerSimpleItem(path, new Item.Properties().stacksTo(1));
    }

    static void registerToolBox(DeferredRegister.Items items, WorkshopToolBox.Kind kind) {
        items.registerItem(
                kind.path,
                properties -> new ToolBoxItem(properties, kind),
                new Item.Properties().stacksTo(1));
    }

    static ResourceKey<LootTable> scrapLootKey() {
        return ResourceKey.create(
                Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath("ic2", "gameplay/scrap_box"));
    }

    static Item scrapBox(Item.Properties properties) {
        return new Item(properties) {
            @Override
            public InteractionResultHolder<ItemStack> use(
                    Level level, Player player, InteractionHand hand) {
                var box = player.getItemInHand(hand);
                ScrapBoxContents.open(level, player, box);
                return InteractionResultHolder.sidedSuccess(box, level.isClientSide);
            }
        };
    }

    static void filledCan(DeferredRegister.Items items) {
        items.registerItem(
                "filled_tin_can",
                properties ->
                        new net.minecraft.world.item.Item(properties) {
                            @Override
                            public net.minecraft.world.InteractionResultHolder<
                                            net.minecraft.world.item.ItemStack>
                                    use(
                                            net.minecraft.world.level.Level level,
                                            net.minecraft.world.entity.player.Player player,
                                            net.minecraft.world.InteractionHand hand) {
                                var held = player.getItemInHand(hand);
                                return CannedFood.eat(level, player, held)
                                        ? net.minecraft.world.InteractionResultHolder.sidedSuccess(
                                                held, level.isClientSide)
                                        : net.minecraft.world.InteractionResultHolder.pass(held);
                            }
                        },
                new net.minecraft.world.item.Item.Properties());
    }

    static void capabilities(IEventBus bus) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event -> {
                    event.registerBlockEntity(
                            Capabilities.ItemHandler.BLOCK,
                            WorkshopPlatform.type(),
                            (node, side) -> new SidedInvWrapper(node.inventory, side));
                    event.registerBlockEntity(
                            Capabilities.FluidHandler.BLOCK,
                            WorkshopPlatform.type(),
                            (node, side) -> {
                                var access = new WorkshopFluidPort(node, side);
                                return access.size() > 0 ? new Port(access) : null;
                            });
                    WorkshopCells.FILLED.forEach(
                            (path, liquid) ->
                                    event.registerItem(
                                            Capabilities.FluidHandler.ITEM,
                                            (stack, ignored) ->
                                                    new FluidHandlerItemStackSimple.SwapEmpty(
                                                            TransportPlatform::cellFluidComponent,
                                                            stack,
                                                            WorkshopPlatform.stack(
                                                                    "ic2:empty_cell", 1),
                                                            WorkshopCells.VOLUME) {
                                                        @Override
                                                        public FluidStack getFluid() {
                                                            return WorkshopPlatform.fluid(
                                                                    liquid, WorkshopCells.VOLUME);
                                                        }

                                                        @Override
                                                        public boolean canFillFluidType(
                                                                FluidStack resource) {
                                                            return false;
                                                        }
                                                    },
                                            BuiltInRegistries.ITEM
                                                    .getOptional(
                                                            ResourceLocation.parse("ic2:" + path))
                                                    .orElseThrow()));
                });
    }

    static ItemStack stack(String id, int count) {
        return BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.parse(id))
                .map(item -> new ItemStack(item, count))
                .orElse(ItemStack.EMPTY);
    }

    static FluidStack fluid(String id, int amount) {
        return BuiltInRegistries.FLUID
                .getOptional(ResourceLocation.parse(id))
                .map(fluid -> new FluidStack(fluid, amount))
                .orElse(FluidStack.EMPTY);
    }

    static boolean tagged(ItemStack item, String tag) {
        return item.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag)));
    }

    static ItemStack fuelRemainder(ItemStack item) {
        return item.getCraftingRemainingItem();
    }

    static ItemStack smelt(ServerLevel level, ItemStack item) {
        var single = new SingleRecipeInput(item);
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, single, level)
                .map(recipe -> recipe.value().assemble(single, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    static void announce(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    private static IFluidHandler neighborFluid(ServerLevel level, BlockPos pos, Direction side) {
        return level.hasChunkAt(pos)
                ? level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side)
                : null;
    }

    static boolean hasExternalFluidPort(ServerLevel level, BlockPos pos, Direction side) {
        return neighborFluid(level, pos, side) != null;
    }

    static int pushFluid(
            ServerLevel level, BlockPos pos, Direction side, String fluid, int amount) {
        var handler = neighborFluid(level, pos, side);
        return handler == null
                ? 0
                : handler.fill(
                        WorkshopPlatform.fluid(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
    }

    static Liquid pullFluid(
            ServerLevel level,
            BlockPos pos,
            Direction side,
            String fluid,
            int amount,
            boolean simulate) {
        var handler = neighborFluid(level, pos, side);
        if (handler == null || amount <= 0) {
            return Liquid.EMPTY;
        }
        var stack =
                fluid.isEmpty()
                        ? handler.drain(amount, IFluidHandler.FluidAction.SIMULATE)
                        : handler.drain(
                                WorkshopPlatform.fluid(fluid, amount),
                                IFluidHandler.FluidAction.SIMULATE);
        if (stack.isEmpty() || !stack.getComponentsPatch().isEmpty()) {
            return Liquid.EMPTY;
        }
        if (!simulate) {
            stack = handler.drain(stack, IFluidHandler.FluidAction.EXECUTE);
        }
        return stack.isEmpty()
                ? Liquid.EMPTY
                : new Liquid(WorkshopPlatform.fluidId(stack), stack.getAmount());
    }

    static boolean canSupplyContainer(ItemStack item, boolean filling) {
        var handler = item.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
        return handler != null
                && handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE).isEmpty()
                        == filling;
    }

    static ContainerResult containerRecipe(
            WorkshopNode.Tank tank, ItemStack item, boolean filling) {
        var stack = item.copyWithCount(1);
        var handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return null;
        }
        var liquid =
                filling
                        ? WorkshopPlatform.fluid(tank.id, tank.amount)
                        : handler.drain(
                                tank.capacity - tank.amount, IFluidHandler.FluidAction.EXECUTE);
        if (liquid.isEmpty() || !liquid.getComponentsPatch().isEmpty()) {
            return null;
        }
        int amount =
                filling
                        ? handler.fill(liquid, IFluidHandler.FluidAction.EXECUTE)
                        : liquid.getAmount();
        return amount > 0
                ? new ContainerResult(
                        handler.getContainer(),
                        new Liquid(WorkshopPlatform.fluidId(liquid), amount))
                : null;
    }

    static boolean interact(
            WorkshopNode node, Player player, InteractionHand hand, Direction side) {
        var access = new WorkshopFluidPort(node, side);
        return access.size() > 0
                && player.getItemInHand(hand).getCapability(Capabilities.FluidHandler.ITEM) != null
                && (!(node.getLevel() instanceof ServerLevel)
                        || FluidUtil.interactWithFluidHandler(player, hand, new Port(access)));
    }

    private record Port(WorkshopFluidPort access) implements IFluidHandler {
        @Override
        public int getTanks() {
            return access.size();
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            var tank = access.tank(index);
            return tank == null ? FluidStack.EMPTY : WorkshopPlatform.fluid(tank.id, tank.amount);
        }

        @Override
        public int getTankCapacity(int index) {
            var tank = access.tank(index);
            return tank == null ? 0 : tank.capacity;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack resource) {
            return !resource.isEmpty()
                    && resource.getComponentsPatch().isEmpty()
                    && access.canInsert(index, WorkshopPlatform.fluidId(resource));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return isFluidValid(0, resource)
                    ? access.insert(
                            WorkshopPlatform.fluidId(resource),
                            resource.getAmount(),
                            action.simulate())
                    : 0;
        }

        @Override
        public FluidStack drain(FluidStack requested, FluidAction action) {
            return requested.isEmpty() || !requested.getComponentsPatch().isEmpty()
                    ? FluidStack.EMPTY
                    : drain(WorkshopPlatform.fluidId(requested), requested.getAmount(), action);
        }

        @Override
        public FluidStack drain(int requested, FluidAction action) {
            var output = access.output();
            return output == null ? FluidStack.EMPTY : drain(output.id, requested, action);
        }

        private FluidStack drain(String id, int requested, FluidAction action) {
            return WorkshopPlatform.fluid(id, access.extract(id, requested, action.simulate()));
        }
    }

    private static final class ToolBoxItem extends Item {
        private final WorkshopToolBox.Kind kind;

        ToolBoxItem(Properties properties, WorkshopToolBox.Kind kind) {
            super(properties);
            this.kind = kind;
        }

        @Override
        public net.minecraft.world.InteractionResultHolder<ItemStack> use(
                Level level, Player owner, InteractionHand hand) {
            if (level instanceof ServerLevel) {
                WorkshopToolBox.open(owner, hand, kind);
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(
                    owner.getItemInHand(hand), !(level instanceof ServerLevel));
        }
    }
}
