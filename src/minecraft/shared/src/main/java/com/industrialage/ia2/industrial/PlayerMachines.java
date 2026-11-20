package com.industrialage.ia2.industrial;

import com.industrialage.ia2.power.WorldPowerStorage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/** Local player machines that spend EU or exchange items against a real adjacent inventory. */
final class PlayerMachines {
    private PlayerMachines() {}

    static boolean tick(PowerNode n, ServerLevel level) {
        return switch (n.kind) {
            case RCI_LZH, RCI_RSH -> inject(n, level);
            case TRADE_O_MAT, ENERGY_O_MAT -> trade(n, level);
            case TELEPORTER -> teleport(n, level);
            case TESLA_COIL -> shock(n, level);
            case MAGNETIZER -> lift(n, level);
            default -> false;
        };
    }

    private static boolean inject(PowerNode device, ServerLevel level) {
        if (device.energy < 1000 || device.inventory.getItem(0).isEmpty()) return false;
        String component = device.kind == PowerKind.RCI_LZH ? "lzh_condensator" : "rsh_condensator";
        int threshold = device.kind == PowerKind.RCI_LZH ? 85000 : 17000;
        if (!device.inventory.canPlaceItem(0, device.inventory.getItem(0))) return false;
        BlockPos pos =
                device.getBlockPos()
                        .relative(
                                device.getBlockState()
                                        .getValue(BlockStateProperties.FACING)
                                        .getOpposite());
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof PowerNode candidate))
            return false;
        PowerNode core =
                candidate.kind == PowerKind.REACTOR_CHAMBER
                        ? candidate.controller(level)
                        : candidate;
        if (core == null || core.kind != PowerKind.NUCLEAR_REACTOR) return false;
        for (int i = 0; i < core.inventory.getContainerSize(); i++) {
            if (core.inventory.part(i).equals(component) && core.inventory.wear(i) > threshold) {
                core.inventory.wear(i, 0);
                core.setChanged();
                device.inventory.removeItem(0, 1);
                device.energy -= 1000;
                device.setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean trade(PowerNode machine, ServerLevel level) {
        var inv = machine.inventory;
        var price = inv.getItem(0);
        var goods = inv.getItem(1);
        var payment = inv.getItem(2);
        boolean vendingEnergy = machine.kind == PowerKind.ENERGY_O_MAT;
        boolean traded = false;
        if (!price.isEmpty()
                && WorkshopPlatform.sameItem(price, payment)
                && payment.getCount() >= price.getCount()
                && (vendingEnergy
                        ? machine.credit == 0
                        : !goods.isEmpty() && outputFits(inv.getItem(3), goods))) {
            for (Direction dir : Direction.values()) {
                BlockPos p = machine.getBlockPos().relative(dir);
                if (!level.hasChunkAt(p)) continue;
                var be = level.getBlockEntity(p);
                Container stock =
                        be instanceof BoxNode b && b.kind.portable()
                                ? b.items
                                : be instanceof Container c ? c : null;
                if (stock == null) continue;
                if (exchange(
                        stock, dir.getOpposite(), price, vendingEnergy ? ItemStack.EMPTY : goods)) {
                    inv.removeItem(2, price.getCount());
                    if (vendingEnergy) machine.credit = machine.saleSize;
                    else if (inv.getItem(3).isEmpty()) inv.setItem(3, goods.copy());
                    else inv.getItem(3).grow(goods.getCount());
                    machine.trades++;
                    machine.setChanged();
                    traded = true;
                    break;
                }
            }
        }
        if (vendingEnergy && machine.credit > 0 && machine.energy > 0) {
            double charged =
                    PowerPlatform.charge(
                            inv.getItem(3), Math.min(machine.credit, machine.energy), 4);
            if (charged > 0) {
                machine.energy -= charged;
                machine.credit -= charged;
                machine.setChanged();
                traded = true;
            }
        }
        return traded;
    }

    static boolean outputFits(ItemStack slot, ItemStack item) {
        return slot.isEmpty()
                || WorkshopPlatform.sameItem(slot, item)
                        && item.getCount() <= slot.getMaxStackSize() - slot.getCount();
    }

    /**
     * Plan a complete exchange before touching the merchant's stock. Stock and payment may share a
     * slot.
     */
    private static boolean exchange(
            Container stock, Direction face, ItemStack price, ItemStack goods) {
        List<ItemStack> after = new ArrayList<>(stock.getContainerSize());
        for (int i = 0; i < stock.getContainerSize(); i++) after.add(stock.getItem(i).copy());
        net.minecraft.world.WorldlyContainer sided =
                stock instanceof net.minecraft.world.WorldlyContainer c ? c : null;
        int[] accessible =
                sided == null
                        ? java.util.stream.IntStream.range(0, after.size()).toArray()
                        : sided.getSlotsForFace(face);
        int owed = goods.getCount();
        for (int i : accessible) {
            ItemStack stack = after.get(i);
            if (owed > 0
                    && WorkshopPlatform.sameItem(stack, goods)
                    && (sided == null || sided.canTakeItemThroughFace(i, stack, face))) {
                int count = Math.min(owed, stack.getCount());
                stack.shrink(count);
                owed -= count;
            }
        }
        if (owed > 0) return false;
        int due = price.getCount();
        for (boolean emptySlots : new boolean[] {false, true}) {
            for (int i : accessible) {
                ItemStack stack = after.get(i);
                if (due <= 0
                        || stack.isEmpty() != emptySlots
                        || !stock.canPlaceItem(i, price)
                        || sided != null && !sided.canPlaceItemThroughFace(i, price, face))
                    continue;
                if (!emptySlots && !WorkshopPlatform.sameItem(stack, price)) continue;
                int count =
                        Math.min(
                                due,
                                Math.min(stock.getMaxStackSize(), price.getMaxStackSize())
                                        - stack.getCount());
                if (count <= 0) continue;
                if (emptySlots) {
                    stack = price.copy();
                    stack.setCount(count);
                    after.set(i, stack);
                } else stack.grow(count);
                due -= count;
            }
        }
        if (due != 0) return false;
        for (int i = 0; i < after.size(); i++) {
            var old = stock.getItem(i);
            var next = after.get(i);
            if (old.getCount() != next.getCount()
                    || !old.isEmpty() && !WorkshopPlatform.sameItem(old, next))
                stock.setItem(i, next);
        }
        stock.setChanged();
        return true;
    }

    static void link(PowerNode source, ItemStack link, Player player, ServerLevel level) {
        String dimension = PowerPlatform.dimension(level);
        String saved = PowerPlatform.link(link);
        String[] fields = saved.split(";");
        if (fields.length == 4 && fields[0].equals(dimension)) {
            BlockPos pos;
            try {
                pos =
                        new BlockPos(
                                Integer.parseInt(fields[1]),
                                Integer.parseInt(fields[2]),
                                Integer.parseInt(fields[3]));
            } catch (NumberFormatException malformed) {
                PowerPlatform.link(link, "");
                return;
            }
            if (!pos.equals(source.getBlockPos())
                    && level.hasChunkAt(pos)
                    && level.getBlockEntity(pos) instanceof PowerNode other
                    && other.kind == PowerKind.TELEPORTER) {
                source.target = pos;
                other.target = source.getBlockPos();
                source.setChanged();
                other.setChanged();
                PowerPlatform.link(link, "");
                return;
            }
        }
        BlockPos p = source.getBlockPos();
        PowerPlatform.link(link, dimension + ";" + p.getX() + ";" + p.getY() + ";" + p.getZ());
    }

    private static boolean teleport(PowerNode pad, ServerLevel level) {
        if (pad.target == null
                || !level.hasNeighborSignal(pad.getBlockPos())
                || !level.hasChunkAt(pad.target)) return false;
        if (!(level.getBlockEntity(pad.target) instanceof PowerNode dest)
                || dest.kind != PowerKind.TELEPORTER
                || !pad.getBlockPos().equals(dest.target)) return false;
        if (level.getGameTime() < pad.cooldownUntil) return false;
        for (Entity entity :
                level.getEntities(
                        (Entity) null,
                        new AABB(pad.getBlockPos()).inflate(.5, 1, .5),
                        e -> !e.isSpectator() && !e.isPassenger())) {
            int weight = travelWeight(entity);
            if (weight == 0) continue;
            int cost =
                    (int)
                            (weight
                                    * 5.0
                                    * Math.pow(
                                            Math.sqrt(pad.target.distSqr(pad.getBlockPos())) + 10,
                                            .7));
            List<WorldPowerStorage> stores = new ArrayList<>(6);
            for (Direction side : Direction.values()) {
                BlockPos source = pad.getBlockPos().relative(side);
                if (level.hasChunkAt(source)
                        && level.getBlockEntity(source) instanceof WorldPowerStorage storage
                        && storage.availableStoredEnergy() > 0) stores.add(storage);
            }
            if (!WorldPowerStorage.withdraw(stores, cost)) continue;
            entity.teleportTo(
                    pad.target.getX() + .5, pad.target.getY() + 1.02, pad.target.getZ() + .5);
            dest.cooldownUntil = level.getGameTime() + 20;
            pad.cooldownUntil = level.getGameTime() + 10;
            dest.setChanged();
            pad.setChanged();
            com.industrialage.ia2.audio.IndustrialSounds.play(
                    level, pad.getBlockPos(), "machine.teleporter.use");
            return true;
        }
        return false;
    }

    private static int stackWeight(ItemStack stack) {
        return stack.isEmpty() ? 0 : 100 * stack.getCount() / stack.getMaxStackSize();
    }

    private static int travelWeight(Entity e) {
        if (e instanceof ItemEntity item) return stackWeight(item.getItem());
        if (e instanceof Player player) {
            int total = 1000;
            if (PowerSettings.inventoryWeight())
                for (int i = 0; i < player.getInventory().getContainerSize(); i++)
                    total += stackWeight(player.getInventory().getItem(i));
            return total;
        }
        if (e instanceof LivingEntity) return 500;
        return PowerPlatform.vehicle(e) ? 100 : 0;
    }

    private static boolean shock(PowerNode coil, ServerLevel level) {
        if (!level.hasNeighborSignal(coil.getBlockPos())
                || coil.energy < 400
                || level.getGameTime() % 20 != 0) return false;
        for (LivingEntity victim :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(coil.getBlockPos()).inflate(4),
                        e -> e.isAlive() && !e.isSpectator())) {
            int damage = Math.min(8, (int) (coil.energy / 400));
            if (PowerPlatform.shock(level, victim, damage)) {
                coil.energy -= 400 * damage;
                coil.setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean lift(PowerNode magnet, ServerLevel level) {
        boolean active = false;
        var p = magnet.getBlockPos();
        for (Player user :
                level.getEntitiesOfClass(Player.class, new AABB(p).inflate(1.2, 12, 1.2))) {
            var boots = user.getItemBySlot(EquipmentSlot.FEET);
            if (boots.isEmpty()) continue;
            var userPos = user.blockPosition();
            boolean onPost = false;
            for (Direction side : Direction.values()) {
                var state = level.getBlockState(userPos.relative(side));
                String name = PowerPlatform.blockId(state);
                if (name.equals("ic2:iron_fence")) {
                    onPost = true;
                    break;
                }
            }
            if (!onPost && !userPos.below().equals(p)) continue;
            var motion = user.getDeltaMovement();
            boolean powered = magnet.energy >= 2;
            if (user.isShiftKeyDown() && motion.y < 0) {
                user.setDeltaMovement(motion.multiply(1, powered ? .8 : .9, 1));
            } else if (powered) {
                user.setDeltaMovement(motion.x, Math.min(.6, motion.y + .07725), motion.z);
                user.fallDistance = 0;
            }
            if (powered) {
                magnet.energy -= 2;
                active = true;
            }
        }
        if (active) magnet.setChanged();
        return active;
    }
}
