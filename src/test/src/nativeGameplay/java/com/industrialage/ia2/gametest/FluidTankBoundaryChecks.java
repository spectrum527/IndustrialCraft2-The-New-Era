package com.industrialage.ia2.gametest;

import com.industrialage.ia2.industrial.WorkshopMenu;
import com.industrialage.ia2.industrial.WorkshopNode;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;

/** Exact capacity boundaries in real ticking machines and their native fluid ports. */
final class FluidTankBoundaryChecks {
    private static final BlockPos[] POS = {
        new BlockPos(17, 145, 20), new BlockPos(19, 145, 20),
        new BlockPos(21, 145, 20), new BlockPos(23, 145, 20),
        new BlockPos(25, 145, 20)
    };
    private final WorkshopNode[] nodes = new WorkshopNode[POS.length];
    private final WorkshopMenu[] menus = new WorkshopMenu[POS.length];
    private final FluidExtraChecks extras = new FluidExtraChecks();
    private int elapsed;
    private int afterProduct;
    private boolean boundariesChecked;
    private boolean crossed;

    void setup(ServerLevel world, BiConsumer<String, Boolean> check) {
        extras.setup(world);
        var owner = FakePlayerFactory.getMinecraft(world);
        int[] volumes = {7001, 7000, 7751, 7750, 7100};
        for (int i = 0; i < POS.length; i++) {
            NativeGameplayFixture.place(world, owner, "ore_washing_plant", POS[i]);
            nodes[i] = (WorkshopNode) world.getBlockEntity(POS[i]);
            menus[i] = (WorkshopMenu) nodes[i].createMenu(20 + i, owner.getInventory(), owner);
            check.accept(
                    "washer-boundary-setup-" + volumes[i],
                    NativeTransferAccess.fill(
                                    world,
                                    POS[i],
                                    Direction.UP,
                                    new FluidStack(Fluids.WATER, volumes[i]),
                                    false)
                            == volumes[i]);
            var stack =
                    i == 2 || i == 3
                            ? NativeTransferAccess.filledCell(
                                    new ItemStack(NativeGameplayFixture.item("fluid_cell")),
                                    new FluidStack(Fluids.WATER, 250))
                            : new ItemStack(NativeGameplayFixture.item("water_cell"));
            menus[i].getSlot(6).set(stack);
        }
        menus[4].getSlot(0).set(new ItemStack(NativeGameplayFixture.item("crushed_copper")));
        nodes[4].receive(16_000);
    }

    boolean tick(ServerLevel world, BiConsumer<String, Boolean> check) {
        boolean extrasDone = extras.tick(world, check);
        int ticks = ++elapsed;
        if (ticks == 15) {
            check.accept(
                    "washer-7001-gap-999-preserves-whole-1000-cell",
                    amount(0) == 7001
                            && is(0, 6, "water_cell")
                            && menus[0].getSlot(14).getItem().isEmpty());
            check.accept(
                    "washer-pipe-partially-fills-999-gap",
                    NativeTransferAccess.fill(
                                            world,
                                            POS[0],
                                            Direction.UP,
                                            new FluidStack(Fluids.WATER, 1000),
                                            false)
                                    == 999
                            && amount(0) == 8000
                            && is(0, 6, "water_cell"));
            check.accept(
                    "washer-7000-gap-1000-accepts-whole-cell",
                    amount(1) == 8000
                            && menus[1].getSlot(6).getItem().isEmpty()
                            && is(1, 14, "empty_cell"));
            check.accept(
                    "washer-7751-gap-249-preserves-250-cell",
                    amount(2) == 7751
                            && is(2, 6, "fluid_cell")
                            && NativeTransferAccess.itemFluidVolume(menus[2].getSlot(6).getItem())
                                    == 250
                            && menus[2].getSlot(14).getItem().isEmpty());
            check.accept(
                    "washer-7750-gap-250-accepts-entire-250-cell",
                    amount(3) == 8000
                            && menus[3].getSlot(6).getItem().isEmpty()
                            && is(3, 14, "fluid_cell")
                            && NativeTransferAccess.itemFluidVolume(menus[3].getSlot(14).getItem())
                                    == 0);
            check.accept(
                    "washer-7100-keeps-cell-until-first-completion",
                    amount(4) == 7100
                            && is(4, 6, "water_cell")
                            && menus[4].getSlot(14).getItem().isEmpty());
            boundariesChecked = true;
        }
        if (ticks % 50 == 0) nodes[4].receive(16_000);
        if (!crossed && !menus[4].getSlot(9).getItem().isEmpty()) {
            if (afterProduct++ == 0) {
                check.accept(
                        "washer-7100-first-work-consumes-100",
                        is(4, 9, "purified_copper") && (amount(4) == 7000 || amount(4) == 8000));
            }
            if (afterProduct == 3) {
                check.accept(
                        "washer-7000-after-work-refills-next-full-cell",
                        amount(4) == 8000
                                && menus[4].getSlot(6).getItem().isEmpty()
                                && is(4, 14, "empty_cell"));
                crossed = true;
            }
        }
        return boundariesChecked && crossed && extrasDone;
    }

    private int amount(int i) {
        return nodes[i].fluidContents().getFirst().amount();
    }

    private boolean is(int i, int slot, String id) {
        return menus[i].getSlot(slot).getItem().is(NativeGameplayFixture.item(id));
    }
}
