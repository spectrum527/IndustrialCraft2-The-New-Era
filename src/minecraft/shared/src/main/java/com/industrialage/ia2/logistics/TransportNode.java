package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec.Kind;
import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Physical contents, pump covers and side valves belonging to one placed transport block. */
public final class TransportNode extends TransportSaveBase
        implements net.minecraft.world.MenuProvider {
    static final Direction[] SIDES = Direction.values();
    final TransportSpec spec;
    ItemStack cargo = ItemStack.EMPTY;
    final net.minecraft.world.SimpleContainer upgrades;
    FluidKey liquid;
    int amount;
    int closed;
    int lv;
    int mv;
    long lastFluidTick = Long.MIN_VALUE;
    long receivedItemAt = Long.MIN_VALUE;

    /** Constructs an empty node. Native load restores its complete contents and sides together. */
    public TransportNode(BlockPos pos, BlockState state) {
        super(TransportPlatform.type(), pos, state);
        spec = ((TransportBlock) state.getBlock()).spec;
        upgrades =
                new net.minecraft.world.SimpleContainer(spec.path().equals("tank") ? 4 : 0) {
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        TransportNode.this.setChanged();
                    }
                };
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
            int id,
            net.minecraft.world.entity.player.Inventory player,
            net.minecraft.world.entity.player.Player who) {
        return spec.kind() == Kind.TANK
                ? new TankMenu(id, player, this, upgrades.getContainerSize() > 0)
                : null;
    }

    /** Live capacity and fluid name for a tank or a fluid pipe, if present. */
    public java.util.Optional<FluidContents> fluidContents() {
        if (spec.kind() == Kind.ITEM_PIPE) return java.util.Optional.empty();
        return java.util.Optional.of(
                liquid == null
                        ? FluidContents.empty(spec.capacity())
                        : new FluidContents(
                                WorkshopPlatform.previewFluid(liquid.registryId()),
                                amount,
                                spec.capacity()));
    }

    void tick(ServerLevel world) {
        if (upgrades.getContainerSize() > 0) {
            if (world.getGameTime() % 8 == 0 && !upgrades.isEmpty()) runUpgrades(world);
            return;
        }
        if (spec.kind() == Kind.ITEM_PIPE) {
            tickItems(world);
        } else if (spec.kind() == Kind.FLUID_PIPE && lastFluidTick != world.getGameTime()) {
            LiquidNetwork.tick(world, this);
        }
    }

    private void tickItems(ServerLevel world) {
        Direction output = getBlockState().getValue(TransportBlock.OUT);
        BlockPos target = worldPosition.relative(output);
        if (!cargo.isEmpty() && receivedItemAt != world.getGameTime() && world.hasChunkAt(target)) {
            int sent;
            if (world.getBlockEntity(target) instanceof TransportNode next) {
                sent = next.insertItem(output.getOpposite(), cargo, false, world.getGameTime());
            } else {
                sent = TransportPlatform.offerItem(world, target, output.getOpposite(), cargo);
            }
            if (sent > 0) {
                cargo.shrink(sent);
                if (cargo.isEmpty()) cargo = ItemStack.EMPTY;
                setChanged();
            }
        }
        BlockPos source = worldPosition.relative(output.getOpposite());
        if (cargo.isEmpty()
                && world.hasChunkAt(source)
                && !(world.getBlockEntity(source) instanceof TransportNode)) {
            ItemStack extracted =
                    TransportPlatform.takeItem(world, source, output, spec.capacity());
            if (!extracted.isEmpty()) {
                cargo = extracted;
                receivedItemAt = world.getGameTime();
                setChanged();
            }
        }
    }

    int insertItem(Direction side, ItemStack incoming, boolean simulate, long time) {
        if (spec.kind() != Kind.ITEM_PIPE
                || incoming.isEmpty()
                || side == getBlockState().getValue(TransportBlock.OUT)
                || (!cargo.isEmpty() && !TransportPlatform.sameItem(cargo, incoming))) return 0;
        int space = Math.min(spec.capacity(), incoming.getMaxStackSize()) - cargo.getCount();
        int accepted = Math.min(incoming.getCount(), Math.max(0, space));
        if (!simulate && accepted > 0) {
            if (cargo.isEmpty()) {
                cargo = incoming.copy();
                cargo.setCount(accepted);
            } else {
                cargo.grow(accepted);
            }
            receivedItemAt = time;
            setChanged();
        }
        return accepted;
    }

    int pump(Direction side) {
        if (side == null) return 0;
        int mask = 1 << side.ordinal();
        return (lv & mask) != 0 ? 32 : (mv & mask) != 0 ? 128 : 0;
    }

    boolean open(Direction side) {
        return side == null || (closed & (1 << side.ordinal())) == 0;
    }

    int fill(Direction side, FluidKey key, int offered, boolean simulate) {
        if (offered <= 0
                || spec.kind() == Kind.ITEM_PIPE
                || !open(side)
                || (liquid != null && !liquid.equals(key))) return 0;
        int accepted = Math.min(offered, spec.capacity() - amount);
        if (!simulate && accepted > 0) {
            liquid = key;
            amount += accepted;
            setChanged();
        }
        return accepted;
    }

    int drain(Direction side, FluidKey key, int requested, boolean simulate) {
        if (requested <= 0
                || liquid == null
                || !open(side)
                || pump(side) != 0
                || (key != null && !liquid.equals(key))) return 0;
        int drained = Math.min(amount, requested);
        if (!simulate) debit(drained);
        return drained;
    }

    void debit(int transferred) {
        if (transferred == 0) return;
        amount -= transferred;
        if (amount == 0) liquid = null;
        setChanged();
    }

    boolean cover(Direction face, int rate) {
        if (spec.kind() != Kind.FLUID_PIPE || pump(face) != 0 || !open(face)) return false;
        int mask = 1 << face.ordinal();
        if (rate == 32) lv |= mask;
        else if (rate == 128) mv |= mask;
        else return false;
        setChanged();
        return true;
    }

    int uncover(Direction side) {
        int rate = pump(side);
        if (rate != 0) {
            int mask = ~(1 << side.ordinal());
            lv &= mask;
            mv &= mask;
            setChanged();
        }
        return rate;
    }

    boolean toggle(Direction face) {
        if (spec.kind() != Kind.FLUID_PIPE) return false;
        closed ^= 1 << face.ordinal();
        setChanged();
        return true;
    }

    static boolean isFluidUpgrade(ItemStack item) {
        String id = TransportPlatform.itemId(item);
        return id.equals("ic2:fluid_ejector_upgrade") || id.equals("ic2:fluid_pulling_upgrade");
    }

    private void runUpgrades(ServerLevel level) {
        for (int i = 0; i < upgrades.getContainerSize(); i++) {
            var upgrade = upgrades.getItem(i);
            if (!isFluidUpgrade(upgrade)) continue;
            boolean eject = TransportPlatform.itemId(upgrade).equals("ic2:fluid_ejector_upgrade");
            int remaining =
                    Math.min(
                            1000,
                            100
                                    * com.industrialage.ia2.content.ProcessingUpgrades
                                            .transferLimit(upgrade.getCount()));
            for (var side : com.industrialage.ia2.world.MachineAutomation.faces(upgrade)) {
                var neighbor = worldPosition.relative(side);
                if (remaining == 0) break;
                if (!level.hasChunkAt(neighbor)) continue;
                if (eject && liquid != null) {
                    int moved =
                            TransportPlatform.offerFluid(
                                    level,
                                    neighbor,
                                    side.getOpposite(),
                                    liquid,
                                    Math.min(amount, remaining),
                                    false);
                    debit(moved);
                    remaining -= moved;
                } else if (!eject && amount < spec.capacity()) {
                    var batch =
                            TransportPlatform.takeFluid(
                                    level,
                                    neighbor,
                                    side.getOpposite(),
                                    liquid,
                                    Math.min(remaining, spec.capacity() - amount));
                    if (batch != null) remaining -= fill(null, batch.key(), batch.amount(), false);
                }
            }
        }
    }

    void dropContents(ServerLevel level) {
        if (!upgrades.isEmpty())
            net.minecraft.world.Containers.dropContents(level, worldPosition, upgrades);
        if (!cargo.isEmpty()) {
            Block.popResource(level, worldPosition, cargo);
            cargo = ItemStack.EMPTY;
        }
        for (Direction side : SIDES) {
            int rate = uncover(side);
            if (rate > 0)
                Block.popResource(level, worldPosition, TransportPlatform.coverItem(rate));
        }
    }
}
