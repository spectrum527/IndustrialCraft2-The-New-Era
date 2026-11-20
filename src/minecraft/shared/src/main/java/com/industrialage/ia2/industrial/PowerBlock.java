package com.industrialage.ia2.industrial;

import com.industrialage.ia2.power.WorldEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Native player block, including the output face of each storage block or transformer. */
public final class PowerBlock extends WorkshopBlockBase implements EntityBlock {
    static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    final PowerKind kind;

    PowerBlock(Properties props, PowerKind kind) {
        super(props);
        this.kind = kind;
        registerDefaultState(
                defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH)
                        .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var side =
                kind.lowTier() > 0 || kind.storage()
                        ? context.getNearestLookingDirection()
                        : context.getHorizontalDirection();
        return defaultBlockState().setValue(BlockStateProperties.FACING, side.getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind == PowerKind.REACTOR_VESSEL || kind == PowerKind.REACTOR_REDSTONE_PORT
                ? null
                : new PowerNode(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (kind.vesselPart() && kind != PowerKind.REACTOR_FLUID_PORT) return null;
        if (!(level instanceof ServerLevel))
            return (w, p, s, e) -> com.industrialage.ia2.audio.IndustrialSounds.observe(w, p, s);
        if (!(level instanceof ServerLevel server) || type != PowerPlatform.type()) return null;
        return (world, pos, now, entity) -> ((PowerNode) entity).tick(server);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return kind.storage();
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof PowerNode node ? node.redstoneSignal() : 0;
    }

    @Override
    public float getDestroyProgress(
            BlockState state,
            Player player,
            net.minecraft.world.level.BlockGetter world,
            BlockPos pos) {
        if (kind.trader()
                && world.getBlockEntity(pos) instanceof PowerNode device
                && device.owner != null
                && !device.owner.equals(player.getUUID())) return 0;
        return super.getDestroyProgress(state, player, world, pos);
    }

    @Override
    boolean interact(
            Level world, BlockPos pos, Player player, InteractionHand hand, Direction side) {
        if (!(world.getBlockEntity(pos) instanceof PowerNode device)) return false;
        if (!(world instanceof ServerLevel server)) return true;
        var stack = player.getItemInHand(hand);
        if (kind.chargeBlock()) {
            if (stack.is(net.minecraft.world.item.Items.FLINT_AND_STEEL)
                    || stack.is(net.minecraft.world.item.Items.FIRE_CHARGE)) {
                if (GameCharges.prime(server, device) && !player.isCreative())
                    ChargeNative.consumeIgniter(player, hand, stack);
            } else if (kind == PowerKind.NUKE) player.openMenu(device);
            return true;
        }
        if (kind == PowerKind.TELEPORTER
                && PowerPlatform.path(stack).equals("frequency_transmitter")) {
            PlayerMachines.link(device, stack, player, server);
            return true;
        }
        if (kind == PowerKind.ENERGY_O_MAT
                && player.isShiftKeyDown()
                && stack.isEmpty()
                && (device.owner == null || device.owner.equals(player.getUUID()))) {
            device.saleSize =
                    switch (device.saleSize) {
                        case 1000 -> 5000;
                        case 5000 -> 10_000;
                        default -> 1000;
                    };
            device.setChanged();
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "ic2.trade.offer", device.saleSize));
            return true;
        }
        if (PowerPlatform.path(stack).equals("wrench")) {
            if (player.isShiftKeyDown()) {
                if (kind.trader() && device.owner != null && !device.owner.equals(player.getUUID()))
                    return true;
                if (world.destroyBlock(pos, false))
                    Block.popResource(world, pos, asItem().getDefaultInstance());
            } else {
                world.setBlockAndUpdate(
                        pos, device.getBlockState().setValue(BlockStateProperties.FACING, side));
                WorldEnergy.connect(device);
            }
            return true;
        }
        if ((kind.lowTier() > 0 || kind.storage()) && player.isShiftKeyDown())
            device.changeMode(server);
        else if (kind == PowerKind.REACTOR_CHAMBER) {
            PowerNode core = device.controller(server);
            if (core != null) player.openMenu(core);
        } else if (kind.vesselPart()) {
            if (kind == PowerKind.REACTOR_FLUID_PORT) player.openMenu(device);
            else if (kind == PowerKind.REACTOR_ACCESS_HATCH) {
                var core = ReactorVessel.core(device, server);
                if (core != null) player.openMenu(core);
            }
        } else player.openMenu(device);
        return true;
    }
}
