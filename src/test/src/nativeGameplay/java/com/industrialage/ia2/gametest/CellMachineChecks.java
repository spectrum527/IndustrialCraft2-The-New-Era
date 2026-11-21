package com.industrialage.ia2.gametest;

import com.industrialage.ia2.gameplay.IndustrialMachine;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.logistics.TransportPlatform;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;

/** Cell families exercised through native processors, canner modes and a physical pump. */
final class CellMachineChecks {
    private static final BlockPos COMPRESSOR = new BlockPos(17, 152, 17);
    private static final BlockPos EXTRACTOR = COMPRESSOR.east(2);
    private static final BlockPos PUMP = EXTRACTOR.east(2);
    private static final BlockPos FILLER = PUMP.east(2);
    private static final BlockPos DRAINER = FILLER.east(2);
    private static final Component MARK = Component.literal("Air cell circuit");

    private IndustrialMachine extractor;
    private WorkshopNode pump;
    private WorkshopNode filler;
    private WorkshopNode drainer;
    private AbstractContainerMenu pumpMenu;
    private AbstractContainerMenu fillMenu;
    private AbstractContainerMenu drainMenu;
    private int ticks;

    void setup(ServerLevel world, BiConsumer<String, Boolean> check) {
        var owner = FakePlayerFactory.getMinecraft(world);
        NativeGameplayFixture.place(world, owner, "compressor", COMPRESSOR);
        NativeGameplayFixture.place(world, owner, "extractor", EXTRACTOR);
        var compressor = (IndustrialMachine) world.getBlockEntity(COMPRESSOR);
        extractor = (IndustrialMachine) world.getBlockEntity(EXTRACTOR);
        var filled = NativeTransferAccess.filledCell(cell("fluid_cell", false),
                new FluidStack(Fluids.WATER, 650));
        check.accept("cell-compressor-rejects-a-650mb-universal-shell",
                NativeTransferAccess.insert(world, COMPRESSOR, Direction.UP, filled, false) == 0
                        && NativeTransferAccess.cellVolume(filled) == 650);
        check.accept("cell-machine-input-accepts-empty-universal-and-full-basic",
                insert(world, COMPRESSOR, cell("fluid_cell", true))
                        && insert(world, EXTRACTOR, cell("basic_water_cell", true)));
        compressor.receive(600);
        extractor.receive(600);

        NativeGameplayFixture.place(world, owner, "pump", PUMP);
        world.setBlockAndUpdate(PUMP, world.getBlockState(PUMP)
                .setValue(BlockStateProperties.FACING, Direction.DOWN));
        world.setBlockAndUpdate(PUMP.below(), Blocks.WATER.defaultBlockState());
        pump = (WorkshopNode) world.getBlockEntity(PUMP);
        pumpMenu = pump.createMenu(50, owner.getInventory(), owner);
        pumpMenu.getSlot(0).set(cell("empty_cell", false));
        pumpMenu.getSlot(9).set(new ItemStack(Items.COBBLESTONE, 64));
        pump.receive(40);

        filler = canner(world, owner, FILLER, 1);
        drainer = canner(world, owner, DRAINER, 2);
        fillMenu = filler.createMenu(51, owner.getInventory(), owner);
        drainMenu = drainer.createMenu(52, owner.getInventory(), owner);
        check.accept("cell-canner-receives-one-piped-liter",
                NativeTransferAccess.fill(world, FILLER, Direction.UP, new FluidStack(Fluids.WATER, 1000), false) == 1000);
        fillMenu.getSlot(0).set(cell("fluid_cell", false));
        fillMenu.getSlot(9).set(new ItemStack(Items.COBBLESTONE, 64));
        drainMenu.getSlot(0).set(cell("basic_lava_cell", false));
    }

    boolean tick(ServerLevel world, BiConsumer<String, Boolean> check) {
        ++ticks;
        if (ticks == 30) {
            check.accept("cell-pump-blocked-output-conserves-one-basic-shell-and-liter",
                    !world.getFluidState(PUMP.below()).isSource()
                            && pump.fluidContents().getFirst().amount() == 1000
                            && is(pumpMenu.getSlot(0).getItem(), "empty_cell"));
            check.accept("cell-canner-blocked-output-conserves-one-universal-shell-and-liter",
                    filler.fluidContents().getFirst().amount() == 1000
                            && is(fillMenu.getSlot(0).getItem(), "fluid_cell"));
            pumpMenu.getSlot(9).set(ItemStack.EMPTY);
            fillMenu.getSlot(9).set(ItemStack.EMPTY);
        }
        if (ticks == 150) {
            check.accept("cell-pump-makes-one-basic-water-cell",
                    pumpMenu.getSlot(0).getItem().isEmpty()
                            && isFilled(pumpMenu.getSlot(9).getItem(), "basic_water_cell", 1000)
                            && pump.fluidContents().getFirst().amount() == 0);
            check.accept("cell-canner-fill-makes-one-universal-water-cell",
                    fillMenu.getSlot(0).getItem().isEmpty()
                            && isFilled(fillMenu.getSlot(9).getItem(), "water_cell", 1000)
                            && filler.fluidContents().getFirst().amount() == 0);
            var lava = drainer.fluidContents().get(1);
            check.accept("cell-canner-drain-produces-lava-and-returns-one-basic-shell",
                    drainMenu.getSlot(0).getItem().isEmpty()
                            && isFilled(drainMenu.getSlot(9).getItem(), "empty_cell", 0)
                            && lava.name().equals(Fluids.LAVA.getFluidType().getDescription())
                            && lava.amount() == 1000);
        }
        if (ticks == 315) {
            var air = NativeTransferAccess.cargo(world, COMPRESSOR, Direction.DOWN).copy();
            var basic = NativeTransferAccess.cargo(world, EXTRACTOR, Direction.DOWN);
            check.accept("cell-compressor-makes-one-marked-air-cell",
                    isFilled(air, "air_cell", 1000) && air.getHoverName().equals(MARK));
            check.accept("cell-extractor-returns-one-marked-basic-shell",
                    isFilled(basic, "empty_cell", 0) && basic.getHoverName().equals(MARK));
            check.accept("cell-air-continues-through-real-extractor-item-port",
                    NativeTransferAccess.extract(world, COMPRESSOR, Direction.DOWN, 1, false) == 1
                            && NativeTransferAccess.extract(world, EXTRACTOR, Direction.DOWN, 1, false) == 1
                            && insert(world, EXTRACTOR, air));
            extractor.receive(600);
        }
        if (ticks == 630) {
            var empty = NativeTransferAccess.cargo(world, EXTRACTOR, Direction.DOWN);
            check.accept("cell-extractor-returns-one-marked-universal-shell",
                    isFilled(empty, "fluid_cell", 0) && empty.getHoverName().equals(MARK));
        }
        return ticks >= 630;
    }

    private static WorkshopNode canner(ServerLevel world, FakePlayer owner, BlockPos pos, int mode) {
        NativeGameplayFixture.place(world, owner, "canner", pos);
        world.getServer().getCommands().performPrefixedCommand(
                world.getServer().createCommandSourceStack().withSuppressedOutput(),
                "data merge block " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                        + " {ic2_mode:" + mode + "}");
        var node = (WorkshopNode) world.getBlockEntity(pos);
        node.receive(400);
        return node;
    }

    private static boolean insert(ServerLevel world, BlockPos pos, ItemStack stack) {
        return NativeTransferAccess.insert(world, pos, Direction.UP, stack, false) == 1;
    }

    private static ItemStack cell(String id, boolean marked) {
        var stack = new ItemStack(NativeGameplayFixture.item(id));
        if (marked) stack.set(DataComponents.CUSTOM_NAME, MARK);
        return stack;
    }

    private static boolean is(ItemStack stack, String id) {
        return stack.is(NativeGameplayFixture.item(id)) && stack.getCount() == 1;
    }

    private static boolean isFilled(ItemStack stack, String id, int volume) {
        return is(stack, id) && TransportPlatform.cellContents(stack).amount() == volume;
    }
}
