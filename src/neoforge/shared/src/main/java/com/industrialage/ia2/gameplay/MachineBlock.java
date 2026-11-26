package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.wrench.WrenchTarget;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/** A placeable machine that opens its own inventory and ticks on the server. */
public class MachineBlock extends NativeGameplay.MachineBlockBase implements WrenchTarget {
    static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    final MachineKind kind;

    MachineBlock(Properties properties, MachineKind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(directionProperty(), Direction.NORTH)
                        .setValue(ACTIVE, false));
    }

    static MachineBlock create(Properties properties, MachineKind kind) {
        return kind.storage()
                ? new StorageBlock(properties, kind)
                : new MachineBlock(properties, kind);
    }

    static Direction facing(BlockState state) {
        return state.getValue(((MachineBlock) state.getBlock()).directionProperty());
    }

    EnumProperty<Direction> directionProperty() {
        return BlockStateProperties.HORIZONTAL_FACING;
    }

    Direction placementFacing(BlockPlaceContext context) {
        return context.getHorizontalDirection().getOpposite();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(directionProperty(), ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(directionProperty(), placementFacing(context));
    }

    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, LivingEntity player, ItemStack stack) {
        super.setPlacedBy(level, pos, state, player, stack);
        if (kind.storage()
                && level instanceof ServerLevel
                && level.getBlockEntity(pos) instanceof IndustrialMachine machine) {
            machine.receive(stack.getOrDefault(IndustrialContent.EU.get(), 0.0));
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(directionProperty(), rotation.rotate(facing(state)));
    }

    @Override
    protected void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (level instanceof ServerLevel server
                && kind != MachineKind.IRON_FURNACE
                && (old.getBlock() != this || facing(old) != facing(state))) {
            EnergyNetwork.invalidate(server);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IndustrialMachine(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel))
            return (w, p, s, e) -> com.industrialage.ia2.audio.IndustrialSounds.observe(w, p, s);
        if (type != IndustrialContent.MACHINE.get()) {
            return null;
        }
        return (world, pos, current, entity) -> ((IndustrialMachine) entity).tickMachine();
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server
                && level.getBlockEntity(pos) instanceof IndustrialMachine machine) {
            if (kind.storage() && player.isShiftKeyDown()) {
                machine.cycleRedstoneMode(server);
            } else {
                server.openMenu(machine, buffer -> buffer.writeUtf(kind.path));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return kind.storage();
    }

    @Override
    int analogOutput(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof IndustrialMachine machine
                ? machine.comparatorOutput()
                : 0;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return kind.storage();
    }

    @Override
    public ItemStack wrenchDrop(ServerLevel level, BlockPos pos, BlockState state) {
        var drop = WrenchTarget.super.wrenchDrop(level, pos, state);
        if (level.getBlockEntity(pos) instanceof IndustrialMachine machine) {
            IndustrialContent.retainOnDrop(machine, drop);
        }
        return drop;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        var drops = super.getDrops(state, context);
        if (kind.storage()
                && context.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                        instanceof IndustrialMachine machine) {
            for (var drop : drops) {
                if (drop.is(asItem())) {
                    IndustrialContent.retainOnDrop(machine, drop);
                }
            }
        }
        return drops;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction from) {
        return level.getBlockEntity(pos) instanceof IndustrialMachine machine
                ? machine.redstoneOutput()
                : 0;
    }

    private static final class StorageBlock extends MachineBlock {
        private StorageBlock(Properties properties, MachineKind kind) {
            super(properties, kind);
        }

        @Override
        EnumProperty<Direction> directionProperty() {
            return BlockStateProperties.FACING;
        }

        @Override
        Direction placementFacing(BlockPlaceContext context) {
            return context.getNearestLookingDirection().getOpposite();
        }
    }
}
