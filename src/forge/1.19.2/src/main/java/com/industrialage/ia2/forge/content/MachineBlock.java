package com.industrialage.ia2.forge.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

class MachineBlock extends BaseEntityBlock {
    private static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    static final DirectionProperty OUTPUT_FACE = BlockStateProperties.FACING;
    static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    final MachineKind kind;

    MachineBlock(MachineKind kind) {
        super(
                Properties.of(Material.METAL)
                        .strength(2, 10)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops());
        this.kind = kind;
        var state = defaultBlockState().setValue(facingProperty(), Direction.NORTH);
        registerDefaultState(state.hasProperty(ACTIVE) ? state.setValue(ACTIVE, false) : state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private DirectionProperty facingProperty() {
        return kind.storesEnergy() ? OUTPUT_FACE : FACING;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        var property = facingProperty();
        return state.setValue(property, rotation.rotate(state.getValue(property)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(facingProperty())));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case GENERATOR -> new GeneratorDevice(pos, state);
            case BATBOX, CESU, MFE, MFSU -> new StorageDevice(pos, state);
            default -> new ProcessingDevice(pos, state);
        };
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? (w, p, s, e) -> com.industrialage.ia2.audio.IndustrialSounds.observe(w, p, s)
                : MachineBlock::tick;
    }

    private static void tick(Level level, BlockPos pos, BlockState state, BlockEntity entity) {
        if (entity instanceof GeneratorDevice generator) {
            generator.tick();
        } else if (entity instanceof ProcessingDevice processing) {
            processing.tick();
        } else if (entity instanceof StorageDevice storage) {
            storage.tick();
        }

        if (entity instanceof ForgeDevice device
                && state.hasProperty(ACTIVE)
                && state.getValue(ACTIVE) != device.active()) {
            level.setBlock(pos, state.setValue(ACTIVE, device.active()), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (ForgeContent.id("wrench")
                .equals(ForgeRegistries.ITEMS.getKey(player.getItemInHand(hand).getItem()))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer server
                && level.getBlockEntity(pos) instanceof ForgeDevice device) {
            NetworkHooks.openScreen(server, device, buffer -> buffer.writeEnum(kind));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    InteractionResult wrench(BlockState state, UseOnContext context, boolean mechanical) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if (player == null
                || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.PASS;
        }

        var side =
                !mechanical && player.isShiftKeyDown()
                        ? context.getClickedFace().getOpposite()
                        : context.getClickedFace();
        var property = facingProperty();
        boolean dismantle = mechanical ? player.isShiftKeyDown() : state.getValue(property) == side;
        boolean rotate =
                (kind.storesEnergy() || side.getAxis().isHorizontal())
                        && state.getValue(property) != side;
        if (!dismantle && !rotate) {
            return InteractionResult.PASS;
        }
        var tool = context.getItemInHand();
        int cost =
                dismantle
                        ? 1_000
                        : com.industrialage.ia2.content.PortableEquipment.ELECTRIC_WRENCH.cost;
        if (!mechanical
                && !player.isCreative()
                && com.industrialage.ia2.forge.equipment.ForgeGear.stored(tool) < cost) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            if (dismantle) {
                if (player instanceof ServerPlayer server
                        && net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(
                                        level, server.gameMode.getGameModeForPlayer(), server, pos)
                                < 0) {
                    return InteractionResult.FAIL;
                }
                var drop = new ItemStack(this);
                if (level.getBlockEntity(pos) instanceof StorageDevice storage) {
                    storage.retainIn(drop);
                }
                if (!level.removeBlock(pos, false)) return InteractionResult.FAIL;
                if (!player.isCreative()) {
                    Block.popResource(level, pos, drop);
                }
            } else if (!level.setBlockAndUpdate(pos, state.setValue(property, side))) {
                return InteractionResult.FAIL;
            }
            if (mechanical) {
                context.getItemInHand()
                        .hurtAndBreak(
                                1, player, holder -> holder.broadcastBreakEvent(context.getHand()));
            } else if (!player.isCreative()) {
                com.industrialage.ia2.forge.equipment.ForgeGear.spend(tool, cost);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof ForgeDevice device) {
            Containers.dropContents(level, pos, device.inventory());
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ForgeDevice device) {
            if (kind.storesEnergy()) {
                return Math.min(15, (int) (15 * device.energy() / device.capacity()));
            }
            if (device.duration() > 0) {
                return 15 * device.progress() / device.duration();
            }
        }
        return 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return kind.storesEnergy();
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof StorageDevice storage
                ? storage.redstoneSignal()
                : 0;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        var drops = super.getDrops(state, builder);
        if (!(builder.getParameter(LootContextParams.BLOCK_ENTITY)
                instanceof StorageDevice storage)) {
            return drops;
        }
        var tool = builder.getParameter(LootContextParams.TOOL);
        if (tool == null || tool.isEmpty()) {
            return drops;
        }
        drops.stream().filter(drop -> drop.is(asItem())).forEach(storage::retainIn);
        return drops;
    }

    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.hasTag()
                && level.getBlockEntity(pos) instanceof StorageDevice device
                && !level.isClientSide) {
            device.receive(stack.getTag().getDouble(StorageDevice.STORED_ENERGY));
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (kind.storesEnergy() && stack.hasTag()) {
            double stored = stack.getTag().getDouble(StorageDevice.STORED_ENERGY);
            double capacity =
                    com.industrialage.ia2.content.PowerGrid.storage(kind.storagePath()).capacity();
            if (stored > 0) {
                tooltip.add(Component.translatable("gui.ic2.energy", (int) stored, (int) capacity));
            }
        }
    }

    static class Storage extends MachineBlock {
        Storage(MachineKind kind) {
            super(kind);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(OUTPUT_FACE);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState()
                    .setValue(OUTPUT_FACE, context.getNearestLookingDirection().getOpposite());
        }
    }

    static final class ChargePad extends Storage {
        ChargePad(MachineKind kind) {
            super(kind);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(ACTIVE);
        }
    }

    static final class Working extends MachineBlock {
        Working(MachineKind kind) {
            super(kind);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(ACTIVE);
        }
    }
}
