package com.industrialage.ia2.industrial;

import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

final class WorkshopInventory extends SimpleContainer implements WorldlyContainer {
    static final int SIZE = 27;
    static final int AUX_SLOT = 4;
    static final int WASHER_WATER_SLOT = 6;
    static final int WASHER_CONTAINER_RETURN_SLOT = 14;
    static final int OUTPUT_START = 9;
    static final int RETURN_SLOT = 13;
    static final int UPGRADE_START = 18;
    static final int COMPONENT_BAYS = 10;
    static final int UPGRADE_END = UPGRADE_START + 4;
    static final int POWER_ITEM_SLOT = 26;
    private final WorkshopNode node;

    static void dropOnReplacement(Level level, BlockPos pos, BlockState state, BlockState next) {
        if (state.is(next.getBlock()) || !(level instanceof ServerLevel server)) {
            return;
        }
        var entity = server.getBlockEntity(pos);
        if (entity instanceof WorkshopNode workshop) {
            Containers.dropContents(server, pos, workshop.inventory);
        } else if (entity instanceof PowerNode power) {
            Containers.dropContents(server, pos, power.inventory);
        } else if (entity instanceof BoxNode box) {
            box.releaseContents(server, pos);
        }
    }

    WorkshopInventory(WorkshopNode node) {
        super(SIZE);
        this.node = node;
    }

    boolean protectPattern(int slot) {
        return slot >= WorkshopMatter.FIRST_PATTERN
                && (node.kind == WorkshopKind.UU_SCANNER
                        || node.kind == WorkshopKind.PATTERN_STORAGE);
    }

    int inputCount() {
        return node.kind.inputs(node.mode);
    }

    static boolean componentBay(WorkshopKind kind, int index) {
        return index >= 0
                && index < COMPONENT_BAYS
                && (kind == WorkshopKind.ELECTRIC_HEAT || kind == WorkshopKind.ELECTRIC_KINETIC);
    }

    int installedComponents(String id) {
        int count = 0;
        for (int index = 0; index < COMPONENT_BAYS; index++) {
            var stack = getItem(index);
            if (WorkshopPlatform.itemId(stack).equals(id)) count += stack.getCount();
        }
        return Math.min(COMPONENT_BAYS, count);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == POWER_ITEM_SLOT) {
            return acceptsPowerItem(node.kind, stack, node.energyTier());
        }
        if (componentBay(node.kind, slot)) {
            String id = node.kind == WorkshopKind.ELECTRIC_HEAT ? "ic2:coil" : "ic2:electric_motor";
            return WorkshopPlatform.itemId(stack).equals(id);
        }
        if (node.kind == WorkshopKind.ORE_WASHER) {
            if (slot == WASHER_WATER_SLOT) return acceptsWasherWater(stack);
            if (slot < inputCount()) return WorkshopPlans.storedInput(node.kind, node.mode, stack);
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            return WorkshopAutomation.canInstall(node, stack);
        }
        if (slot == AUX_SLOT && node.kind.auxiliarySlot()) {
            String id = WorkshopPlatform.itemId(stack);
            return switch (node.kind) {
                case BLOCK_CUTTER ->
                        id.equals("ic2:iron_cutting_blade")
                                || id.equals("ic2:steel_cutting_blade")
                                || id.equals("ic2:diamond_cutting_blade");
                case ELECTRIC_HEAT -> id.equals("ic2:coil");
                case BLAST_FURNACE -> id.equals("ic2:air_cell");
                default -> false;
            };
        }
        if (node.kind == WorkshopKind.FLUID_BOTTLER && slot < 2) {
            return WorkshopPlatform.canSupplyContainer(stack, slot == 0);
        }
        if (node.kind == WorkshopKind.PUMP && slot == 0) {
            return WorkshopPlatform.canSupplyContainer(stack, true);
        }
        if (node.kind == WorkshopKind.CANNER || node.kind == WorkshopKind.SOLID_CANNER) {
            return WorkshopPlans.acceptsCanningInput(node.kind, node.mode, slot, stack);
        }
        if (slot < node.kind.inputs(node.mode)) {
            String id = WorkshopPlatform.itemId(stack);
            switch (node.kind) {
                case COKE_KILN, COKE_HATCH -> {
                    return WorkshopCokeKiln.suitable(stack);
                }
                case UU_ASSEMBLY -> {
                    return id.equals("ic2:uu_matter");
                }
                case RADIOISOTOPE_HEAT -> {
                    return id.equals("ic2:rtg_pellet");
                }
                case FLUID_HEAT, SEMIFLUID -> {
                    return WorkshopPlatform.canSupplyContainer(stack, false);
                }
                case CLASSIC_ELECTROLYZER -> {
                    return id.equals(
                            node.mode == 0 ? "ic2:water_cell" : "ic2:electrolyzed_water_cell");
                }
                case MASS_FABRICATOR, CLASSIC_MASS, MATTER_GENERATOR -> {
                    return WorkshopMatter.scrap(id) > 0;
                }
                case UU_SCANNER -> {
                    return WorkshopMatter.scannable(stack);
                }
                case ELECTRIC_KINETIC -> {
                    return id.equals("ic2:electric_motor");
                }
                case STEAM_TURBINE -> {
                    return id.equals("ic2:steam_turbine");
                }
                case WIND_KINETIC, WATER_KINETIC -> {
                    return WorkshopKinetic.ROTOR_RATINGS.containsKey(id);
                }
                default -> {}
            }
        }
        return slot < node.kind.inputs(node.mode)
                && (node.kind != WorkshopKind.BLAST_FURNACE
                        || !WorkshopPlatform.itemId(stack).equals("ic2:air_cell"))
                && (node.kind != WorkshopKind.SOLID_HEAT
                        || node.getLevel() instanceof net.minecraft.server.level.ServerLevel level
                                && WorkshopPlatform.solidFuelTime(level, stack) >= 4);
    }

    static boolean acceptsPowerItem(WorkshopKind kind, ItemStack stack, int tier) {
        return kind.chargesItem()
                ? PowerPlatform.acceptsCharge(stack, tier)
                : kind.dischargesItem() && PowerPlatform.canDischarge(stack, tier);
    }

    static boolean acceptsWasherWater(ItemStack stack) {
        return WorkshopCells.canDrain(
                stack, WorkshopKind.ORE_WASHER.inputVolume(), WorkshopThermal.WATER);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        var input =
                switch (node.kind) {
                    case BLAST_FURNACE -> IntStream.of(0, AUX_SLOT);
                    case ORE_WASHER -> IntStream.of(0, WASHER_WATER_SLOT);
                    case UU_ASSEMBLY -> IntStream.of(1);
                    default -> IntStream.range(0, node.kind.inputs(node.mode));
                };
        var output = IntStream.range(OUTPUT_START, OUTPUT_START + node.kind.outputSlots());
        if (node.kind == WorkshopKind.BLAST_FURNACE) {
            output = IntStream.concat(output, IntStream.of(RETURN_SLOT));
        } else if (node.kind == WorkshopKind.ORE_WASHER) {
            output = IntStream.concat(output, IntStream.of(WASHER_CONTAINER_RETURN_SLOT));
        }
        return (direction == Direction.UP
                        ? IntStream.concat(input, output)
                        : IntStream.concat(output, input))
                .toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return (slot < node.kind.inputs(node.mode)
                        || node.kind == WorkshopKind.BLAST_FURNACE && slot == AUX_SLOT
                        || node.kind == WorkshopKind.ORE_WASHER && slot == WASHER_WATER_SLOT)
                && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return (slot >= OUTPUT_START && slot < OUTPUT_START + node.kind.outputSlots())
                || (node.kind == WorkshopKind.BLAST_FURNACE && slot == RETURN_SLOT)
                || (node.kind == WorkshopKind.ORE_WASHER && slot == WASHER_CONTAINER_RETURN_SLOT);
    }

    @Override
    public boolean stillValid(Player player) {
        return WorkshopPlatform.canOpen(node, player);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (node.getLevel() != null) {
            node.setChanged();
        }
    }
}
