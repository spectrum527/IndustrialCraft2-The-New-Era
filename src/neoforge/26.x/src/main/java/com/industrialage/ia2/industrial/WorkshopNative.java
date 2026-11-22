package com.industrialage.ia2.industrial;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.access.MachinePorts;
import com.industrialage.ia2.industrial.WorkshopPlans.ContainerResult;
import com.industrialage.ia2.industrial.WorkshopPlans.Liquid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

final class WorkshopNative {
    private WorkshopNative() {}

    static BlockEntityType<WorkshopNode> type(Block[] blocks) {
        return new BlockEntityType<>(WorkshopNode::new, blocks);
    }

    static DeferredBlock<WorkshopBlock> block(DeferredRegister.Blocks blocks, WorkshopKind kind) {
        return blocks.registerBlock(
                kind.path,
                props -> WorkshopBlock.create(props, kind),
                WorkshopPlatform::properties);
    }

    static DeferredBlock<Block> advancedCasing(DeferredRegister.Blocks blocks) {
        return blocks.registerBlock("advanced_machine", Block::new, WorkshopPlatform::properties);
    }

    static String crystalPattern(CompoundTag data) {
        return data.getStringOr("ic2_pattern", "");
    }

    static int fuelCanValue(CompoundTag data) {
        return Math.max(0, data.getIntOr("value", 0));
    }

    static void unstackable(DeferredRegister.Items items, String path) {
        items.registerSimpleItem(path, () -> new Item.Properties().stacksTo(1));
    }

    static void registerToolBox(DeferredRegister.Items items, WorkshopToolBox.Kind kind) {
        items.registerItem(
                kind.path,
                properties -> new ToolBoxItem(properties, kind),
                () -> new Item.Properties().stacksTo(1));
    }

    static ResourceKey<LootTable> scrapLootKey() {
        return ResourceKey.create(
                Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath("ic2", "gameplay/scrap_box"));
    }

    static Item scrapBox(Item.Properties properties) {
        return new Item(properties) {
            @Override
            public InteractionResult use(Level level, Player player, InteractionHand hand) {
                var box = player.getItemInHand(hand);
                ScrapBoxContents.open(level, player, box);
                return InteractionResult.SUCCESS;
            }
        };
    }

    static void filledCan(DeferredRegister.Items items) {
        items.registerItem(
                "filled_tin_can",
                properties ->
                        new net.minecraft.world.item.Item(properties) {
                            @Override
                            public net.minecraft.world.InteractionResult use(
                                    net.minecraft.world.level.Level level,
                                    net.minecraft.world.entity.player.Player player,
                                    net.minecraft.world.InteractionHand hand) {
                                return CannedFood.eat(level, player, player.getItemInHand(hand))
                                        ? net.minecraft.world.InteractionResult.SUCCESS
                                        : net.minecraft.world.InteractionResult.PASS;
                            }
                        },
                Item.Properties::new);
    }

    static void capabilities(IEventBus bus) {
        bus.addListener(
                RegisterCapabilitiesEvent.class,
                event -> {
                    event.registerBlockEntity(
                            Capabilities.Item.BLOCK,
                            WorkshopPlatform.type(),
                            (node, side) ->
                                    MachineAccess.connects(node, side)
                                            ? MachinePorts.item(
                                                    node,
                                                    side,
                                                    new WorldlyContainerWrapper(
                                                            node.inventory, side))
                                            : null);
                    event.registerBlockEntity(
                            Capabilities.Fluid.BLOCK,
                            WorkshopPlatform.type(),
                            (node, side) -> {
                                var access = new WorkshopFluidPort(node, side);
                                return access.size() > 0 && MachineAccess.connects(node, side)
                                        ? MachinePorts.fluid(node, side, new Port(access))
                                        : null;
                            });
                });
    }

    static ItemStack stack(String id, int count) {
        return BuiltInRegistries.ITEM
                .getOptional(Identifier.parse(id))
                .map(item -> new ItemStack(item, count))
                .orElse(ItemStack.EMPTY);
    }

    static FluidStack fluid(String id, int amount) {
        return BuiltInRegistries.FLUID
                .getOptional(Identifier.parse(id))
                .map(fluid -> new FluidStack(fluid, amount))
                .orElse(FluidStack.EMPTY);
    }

    static boolean tagged(ItemStack item, String tag) {
        return item.is(TagKey.create(Registries.ITEM, Identifier.parse(tag)));
    }

    static ItemStack fuelRemainder(ItemStack item) {
        var remaining = item.getCraftingRemainder();
        return remaining == null ? ItemStack.EMPTY : remaining.create();
    }

    static ItemStack smelt(ServerLevel level, ItemStack item) {
        var single = new SingleRecipeInput(item);
        return level.recipeAccess()
                .getRecipeFor(RecipeType.SMELTING, single, level)
                .map(recipe -> recipe.value().assemble(single))
                .orElse(ItemStack.EMPTY);
    }

    static void announce(Player player, Component message) {
        player.sendOverlayMessage(message);
    }

    private static ResourceHandler<FluidResource> neighborFluid(
            ServerLevel level, BlockPos pos, Direction side) {
        return level.hasChunkAt(pos)
                ? level.getCapability(Capabilities.Fluid.BLOCK, pos, side)
                : null;
    }

    static boolean hasExternalFluidPort(ServerLevel level, BlockPos pos, Direction side) {
        return neighborFluid(level, pos, side) != null;
    }

    static int pushFluid(
            ServerLevel level, BlockPos pos, Direction side, String fluid, int amount) {
        var handler = neighborFluid(level, pos, side);
        if (handler == null || amount <= 0) {
            return 0;
        }
        var resource = FluidResource.of(WorkshopPlatform.fluid(fluid, amount));
        if (resource.isEmpty()) {
            return 0;
        }
        try (var transaction = Transaction.openRoot()) {
            int accepted = handler.insert(resource, amount, transaction);
            transaction.commit();
            return accepted;
        }
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
        try (var transaction = Transaction.openRoot()) {
            var batch =
                    ResourceHandlerUtil.extractFirst(
                            handler,
                            resource ->
                                    resource.getComponentsPatch().isEmpty()
                                            && (fluid.isEmpty()
                                                    || fluid.equals(
                                                            WorkshopPlatform.fluidId(
                                                                    resource.toStack(1)))),
                            amount,
                            transaction);
            if (batch == null) {
                return Liquid.EMPTY;
            }
            if (!simulate) {
                transaction.commit();
            }
            return new Liquid(
                    WorkshopPlatform.fluidId(batch.resource().toStack(1)), batch.amount());
        }
    }

    static boolean canSupplyContainer(ItemStack item, boolean filling) {
        var scratch = new net.minecraft.world.SimpleContainer(item.copyWithCount(1));
        var access = ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(scratch), 0);
        var handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) {
            return false;
        }
        boolean full =
                java.util.stream.IntStream.range(0, handler.size())
                        .anyMatch(
                                slot ->
                                        !handler.getResource(slot).isEmpty()
                                                && handler.getAmountAsLong(slot) > 0);
        return full != filling;
    }

    static ContainerResult fillContainer(WorkshopNode.Tank tank, ItemStack item) {
        var current = WorkshopPlatform.fluid(tank.id, tank.amount);
        if (current.isEmpty()) return null;
        var scratch = new net.minecraft.world.SimpleContainer(item.copyWithCount(1));
        var access = ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(scratch), 0);
        var handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) return null;
        int accepted;
        try (var transaction = Transaction.openRoot()) {
            accepted = handler.insert(FluidResource.of(current), current.getAmount(), transaction);
            if (accepted <= 0) return null;
            transaction.commit();
        }
        return new ContainerResult(scratch.getItem(0).copy(), new Liquid(tank.id, accepted));
    }

    static ContainerResult drainWholeContainer(WorkshopNode.Tank tank, ItemStack item) {
        var scratch = new net.minecraft.world.SimpleContainer(item.copyWithCount(1));
        var access = ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(scratch), 0);
        var handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) return null;
        var contents =
                WorkshopCells.wholeContents(
                        tank,
                        handler.size(),
                        slot -> {
                            var resource = handler.getResource(slot);
                            if (resource.isEmpty()) return Liquid.EMPTY;
                            return resource.getComponentsPatch().isEmpty()
                                    ? new Liquid(
                                            WorkshopPlatform.fluidId(resource.toStack(1)),
                                            handler.getAmountAsInt(slot))
                                    : null;
                        });
        if (contents.amount() == 0) return null;
        var expected = FluidResource.of(WorkshopPlatform.fluid(contents.id(), contents.amount()));
        try (var transaction = Transaction.openRoot()) {
            if (handler.extract(expected, contents.amount(), transaction) != contents.amount())
                return null;
            transaction.commit();
        }
        return new ContainerResult(scratch.getItem(0).copy(), contents);
    }

    static boolean interact(
            WorkshopNode node, Player player, InteractionHand hand, Direction side) {
        var access = new WorkshopFluidPort(node, side);
        return access.size() > 0
                && ItemAccess.forPlayerInteraction(player, hand)
                                .getCapability(Capabilities.Fluid.ITEM)
                        != null
                && (!(node.getLevel() instanceof ServerLevel)
                        || FluidUtil.interactWithFluidHandler(
                                player, hand, node.getBlockPos(), new Port(access)));
    }

    private record Port(WorkshopFluidPort access) implements ResourceHandler<FluidResource> {
        @Override
        public int size() {
            return access.size();
        }

        @Override
        public FluidResource getResource(int slot) {
            var tank = access.tank(slot);
            return tank == null
                    ? FluidResource.EMPTY
                    : FluidResource.of(WorkshopPlatform.fluid(tank.id, tank.amount));
        }

        @Override
        public long getAmountAsLong(int slot) {
            var tank = access.tank(slot);
            return tank == null ? 0 : tank.amount;
        }

        @Override
        public long getCapacityAsLong(int slot, FluidResource resource) {
            var tank = access.tank(slot);
            return tank == null ? 0 : tank.capacity;
        }

        @Override
        public boolean isValid(int slot, FluidResource resource) {
            return !resource.isEmpty()
                    && resource.getComponentsPatch().isEmpty()
                    && access.canInsert(slot, WorkshopPlatform.fluidId(resource.toStack(1)));
        }

        @Override
        public int insert(
                int slot, FluidResource resource, int amount, TransactionContext transaction) {
            if (!isValid(slot, resource) || amount <= 0) {
                return 0;
            }
            String id = WorkshopPlatform.fluidId(resource.toStack(1));
            int accepted = access.insert(id, amount, true);
            if (accepted > 0) {
                access.node().fluidJournal.updateSnapshots(transaction);
                return access.insert(id, accepted, false);
            }
            return 0;
        }

        @Override
        public int extract(
                int slot, FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty()
                    || access.output() == null
                    || access.output() != access.tank(slot)
                    || !resource.equals(getResource(slot))) {
                return 0;
            }
            String id = WorkshopPlatform.fluidId(resource.toStack(1));
            int drained = access.extract(id, amount, true);
            if (drained > 0) {
                access.node().fluidJournal.updateSnapshots(transaction);
                return access.extract(id, drained, false);
            }
            return 0;
        }
    }

    private static final class ToolBoxItem extends Item {
        private final WorkshopToolBox.Kind kind;

        ToolBoxItem(Properties properties, WorkshopToolBox.Kind kind) {
            super(properties);
            this.kind = kind;
        }

        @Override
        public InteractionResult use(Level level, Player owner, InteractionHand hand) {
            if (level instanceof ServerLevel) {
                WorkshopToolBox.open(owner, hand, kind);
            }
            return InteractionResult.SUCCESS;
        }
    }
}
