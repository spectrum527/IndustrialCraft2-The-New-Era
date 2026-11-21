package com.industrialage.ia2.gametest;

import com.industrialage.ia2.industrial.PowerNode;
import com.industrialage.ia2.industrial.WorkshopMenu;
import com.industrialage.ia2.industrial.WorkshopNode;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;

/** Exercises the actual bottler job and the fluid reactor's dedicated coolant slots. */
final class FluidExtraChecks {
    private static final BlockPos BOTTLER = new BlockPos(29, 145, 20);
    private static final BlockPos IDLE_HEATER = new BlockPos(17, 145, 25);
    private static final BlockPos IDLE_FURNACE = new BlockPos(19, 145, 25);
    private static final BlockPos FULL_SEMI = new BlockPos(21, 145, 25);
    private static final BlockPos CORE = new BlockPos(27, 160, 27);
    private static final BlockPos PORT = CORE.above(2);
    private WorkshopNode bottler;
    private WorkshopMenu bottlerMenu;
    private WorkshopNode idleHeater;
    private WorkshopNode idleFurnace;
    private WorkshopNode fullSemi;
    private AbstractContainerMenu heaterMenu;
    private AbstractContainerMenu furnaceMenu;
    private AbstractContainerMenu fullSemiMenu;
    private FluidStack biogas;
    private PowerNode reactor;
    private AbstractContainerMenu reactorMenu;
    private int ticks;
    private int bottlerStage;
    private int bottlerWait;
    private int coolantStage;
    private int coolantWait;
    private FluidStack coolant;

    void setup(ServerLevel world) {
        var owner = FakePlayerFactory.getMinecraft(world);
        NativeGameplayFixture.place(world, owner, "fluid_bottler", BOTTLER);
        bottler = (WorkshopNode) world.getBlockEntity(BOTTLER);
        bottlerMenu = (WorkshopMenu) bottler.createMenu(40, owner.getInventory(), owner);
        bottlerMenu.getSlot(1).set(new ItemStack(NativeGameplayFixture.item("water_cell"), 7));
        bottler.receive(400);
        NativeGameplayFixture.place(world, owner, "fluid_heat_generator", IDLE_HEATER);
        NativeGameplayFixture.place(world, owner, "blast_furnace", IDLE_FURNACE);
        NativeGameplayFixture.place(world, owner, "semifluid_generator", FULL_SEMI);
        idleHeater = (WorkshopNode) world.getBlockEntity(IDLE_HEATER);
        idleFurnace = (WorkshopNode) world.getBlockEntity(IDLE_FURNACE);
        fullSemi = (WorkshopNode) world.getBlockEntity(FULL_SEMI);
        heaterMenu = idleHeater.createMenu(42, owner.getInventory(), owner);
        furnaceMenu = idleFurnace.createMenu(43, owner.getInventory(), owner);
        fullSemiMenu = fullSemi.createMenu(44, owner.getInventory(), owner);
        heaterMenu.getSlot(0).set(new ItemStack(NativeGameplayFixture.item("lava_cell"), 11));
        furnaceMenu.getSlot(4).set(new ItemStack(NativeGameplayFixture.item("air_cell"), 9));
        var gas =
                net.minecraft.core.registries.BuiltInRegistries.FLUID.stream()
                        .filter(
                                f ->
                                        net.minecraft.core.registries.BuiltInRegistries.FLUID
                                                .getKey(f)
                                                .toString()
                                                .equals("ic2:biogas"))
                        .findFirst()
                        .orElseThrow();
        biogas = new FluidStack(gas, 1000);
        if (NativeTransferAccess.fill(
                        world, FULL_SEMI, Direction.UP, new FluidStack(gas, 9000), false)
                != 9000) throw new AssertionError("Could not prefill 9000 mB biogas by pipe");
        world.getServer()
                .getCommands()
                .performPrefixedCommand(
                        world.getServer().createCommandSourceStack().withSuppressedOutput(),
                        "data merge block 21 145 25 {ic2_energy:16000.0d}");
        fullSemiMenu
                .getSlot(0)
                .set(
                        NativeTransferAccess.filledCell(
                                new ItemStack(NativeGameplayFixture.item("fluid_cell")), biogas));
        var shell = NativeGameplayFixture.block("reactor_vessel").defaultBlockState();
        for (int x = -2; x <= 2; x++)
            for (int y = -2; y <= 2; y++)
                for (int z = -2; z <= 2; z++)
                    if (Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) == 2)
                        world.setBlockAndUpdate(CORE.offset(x, y, z), shell);
        world.setBlockAndUpdate(
                CORE, NativeGameplayFixture.block("nuclear_reactor").defaultBlockState());
        for (var dir : Direction.values())
            world.setBlockAndUpdate(
                    CORE.relative(dir),
                    NativeGameplayFixture.block("reactor_chamber").defaultBlockState());
        world.setBlockAndUpdate(
                PORT, NativeGameplayFixture.block("reactor_fluid_port").defaultBlockState());
        reactor = (PowerNode) world.getBlockEntity(CORE);
        coolant =
                new FluidStack(
                        NativeGameplayFixture.block("coolant")
                                .defaultBlockState()
                                .getFluidState()
                                .getType(),
                        1000);
    }

    boolean tick(ServerLevel world, BiConsumer<String, Boolean> check) {
        ++ticks;
        if (ticks == 15) {
            check.accept(
                    "heater-no-consumer-fills-10000-keeps-eleventh",
                    idleHeater.fluidContents().getFirst().amount() == 10000
                            && heaterMenu
                                    .getSlot(0)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("lava_cell"))
                            && heaterMenu.getSlot(0).getItem().getCount() == 1
                            && heaterMenu.getSlot(9).getItem().getCount() == 10);
            check.accept(
                    "cold-furnace-fills-8000-keeps-ninth-air-cell",
                    idleFurnace.fluidContents().getFirst().amount() == 8000
                            && furnaceMenu
                                    .getSlot(4)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("air_cell"))
                            && furnaceMenu.getSlot(4).getItem().getCount() == 1
                            && furnaceMenu.getSlot(13).getItem().getCount() == 8);
            check.accept(
                    "semifluid-saturated-prefills-10000",
                    fullSemi.bufferedEu().stored() == 16000
                            && fullSemi.fluidContents().getFirst().amount() == 10000
                            && fullSemiMenu.getSlot(0).getItem().isEmpty()
                            && fullSemiMenu
                                    .getSlot(9)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("fluid_cell"))
                            && NativeTransferAccess.itemFluidVolume(
                                            fullSemiMenu.getSlot(9).getItem())
                                    == 0);
            fullSemiMenu.getSlot(9).set(ItemStack.EMPTY);
            fullSemiMenu
                    .getSlot(0)
                    .set(
                            NativeTransferAccess.filledCell(
                                    new ItemStack(NativeGameplayFixture.item("fluid_cell")),
                                    biogas));
        }
        if (ticks == 20) {
            check.accept(
                    "semifluid-full-10000-preserves-next-filled-cell",
                    fullSemi.fluidContents().getFirst().amount() == 10000
                            && NativeTransferAccess.itemFluidVolume(
                                            fullSemiMenu.getSlot(0).getItem())
                                    == 1000
                            && fullSemiMenu.getSlot(9).getItem().isEmpty());
        }
        if (ticks % 50 == 0) bottler.receive(400);
        int output = bottler.fluidContents().getFirst().amount();
        if (bottlerStage == 0 && output == 7000) {
            check.accept(
                    "bottler-sequential-emptying-made-7000",
                    bottlerMenu.getSlot(1).getItem().isEmpty());
            bottlerMenu.getSlot(9).set(ItemStack.EMPTY);
            bottlerMenu.getSlot(1).set(waterCell(751));
            bottlerStage = 1;
        } else if (bottlerStage == 1 && output == 7751) {
            check.accept(
                    "bottler-partial-cell-emptied-751", bottlerMenu.getSlot(1).getItem().isEmpty());
            bottlerMenu.getSlot(9).set(ItemStack.EMPTY);
            bottlerMenu.getSlot(1).set(waterCell(250));
            bottlerStage = 2;
        } else if (bottlerStage == 2 && ++bottlerWait == 20) {
            var held = bottlerMenu.getSlot(1).getItem();
            check.accept(
                    "bottler-gap-249-keeps-whole-250-cell",
                    output == 7751
                            && held.is(NativeGameplayFixture.item("fluid_cell"))
                            && NativeTransferAccess.itemFluidVolume(held) == 250);
            check.accept(
                    "bottler-output-drain-makes-gap-250",
                    NativeTransferAccess.drain(world, BOTTLER, Direction.NORTH, 1, false) == 1);
            bottlerStage = 3;
        } else if (bottlerStage == 3 && output == 8000) {
            check.accept(
                    "bottler-gap-250-completes-whole-emptying-job",
                    bottlerMenu.getSlot(1).getItem().isEmpty() && hasEmptyCell());
            bottlerStage = 4;
        }
        if (coolantStage == 0 && reactor.isFluidReactor()) {
            check.accept(
                    "reactor-fluid-mode-and-piped-9751-coolant",
                    NativeTransferAccess.fill(
                                    world,
                                    PORT,
                                    Direction.UP,
                                    new FluidStack(coolant.getFluid(), 9751),
                                    false)
                            == 9751);
            var owner = FakePlayerFactory.getMinecraft(world);
            reactorMenu = reactor.createMenu(41, owner.getInventory(), owner);
            reactorMenu.getSlot(54).set(coolantCell(250));
            coolantStage = 1;
            coolantWait = 0;
        } else if (coolantStage == 1 && ++coolantWait == 5) {
            var held = reactorMenu.getSlot(54).getItem();
            check.accept(
                    "reactor-coolant-gap-249-preserves-250-cell",
                    reactor.coolantAmount(false) == 9751
                            && NativeTransferAccess.itemFluidVolume(held) == 250
                            && reactorMenu.getSlot(55).getItem().isEmpty());
            check.accept(
                    "reactor-coolant-pipe-drains-one",
                    NativeTransferAccess.drain(world, PORT, Direction.UP, 1, false) == 0);
            // The external reactor fluid port intentionally drains hot coolant only.  Switch
            // the cold tank using its serialized state through the cross-version command below.
            setCoolant(world, 9750);
            coolantStage = 2;
            coolantWait = 0;
        } else if (coolantStage == 2 && ++coolantWait == 5) {
            check.accept(
                    "reactor-coolant-gap-250-accepts-whole-cell",
                    reactor.coolantAmount(false) == 10000
                            && reactorMenu.getSlot(54).getItem().isEmpty()
                            && reactorMenu
                                    .getSlot(55)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("fluid_cell"))
                            && NativeTransferAccess.itemFluidVolume(
                                            reactorMenu.getSlot(55).getItem())
                                    == 0);
            reactorMenu.getSlot(55).set(ItemStack.EMPTY);
            setCoolant(world, 9001);
            reactorMenu.getSlot(54).set(coolantCell(1000));
            coolantStage = 3;
            coolantWait = 0;
        } else if (coolantStage == 3 && ++coolantWait == 5) {
            check.accept(
                    "reactor-coolant-gap-999-preserves-1000-cell",
                    reactor.coolantAmount(false) == 9001
                            && NativeTransferAccess.itemFluidVolume(
                                            reactorMenu.getSlot(54).getItem())
                                    == 1000
                            && reactorMenu.getSlot(55).getItem().isEmpty());
            setCoolant(world, 9000);
            coolantStage = 4;
            coolantWait = 0;
        } else if (coolantStage == 4 && ++coolantWait == 5) {
            check.accept(
                    "reactor-coolant-gap-1000-accepts-whole-cell",
                    reactor.coolantAmount(false) == 10000
                            && reactorMenu.getSlot(54).getItem().isEmpty()
                            && reactorMenu
                                    .getSlot(55)
                                    .getItem()
                                    .is(NativeGameplayFixture.item("fluid_cell"))
                            && NativeTransferAccess.itemFluidVolume(
                                            reactorMenu.getSlot(55).getItem())
                                    == 0);
            coolantStage = 5;
        }
        return bottlerStage == 4 && coolantStage == 5;
    }

    private static void setCoolant(ServerLevel world, int amount) {
        world.getServer()
                .getCommands()
                .performPrefixedCommand(
                        world.getServer().createCommandSourceStack().withSuppressedOutput(),
                        "data merge block 27 160 27 {coolant:" + amount + "}");
    }

    private ItemStack coolantCell(int volume) {
        return NativeTransferAccess.filledCell(
                new ItemStack(NativeGameplayFixture.item("fluid_cell")),
                new FluidStack(coolant.getFluid(), volume));
    }

    private static ItemStack waterCell(int volume) {
        return NativeTransferAccess.filledCell(
                new ItemStack(NativeGameplayFixture.item("fluid_cell")),
                new FluidStack(Fluids.WATER, volume));
    }

    private boolean hasEmptyCell() {
        for (int i = 9; i <= 11; i++) {
            var stack = bottlerMenu.getSlot(i).getItem();
            if (stack.is(NativeGameplayFixture.item("fluid_cell"))
                    && NativeTransferAccess.itemFluidVolume(stack) == 0) return true;
        }
        return false;
    }
}
