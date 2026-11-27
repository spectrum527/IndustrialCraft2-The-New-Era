package com.industrialage.ia2.compatqa;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.access.MachineAccessTarget;
import com.industrialage.ia2.gui.DeviceAccessMenu;
import com.industrialage.ia2.industrial.BoxNode;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;

/**
 * Opens the native server menu of every directly operated stationary device in the block registry.
 */
final class FixedMenuCensus {
    // These registered blocks open their formed controller or deliberately have no operating
    // screen.
    private static final Set<String> STRUCTURE_PARTS =
            Set.of(
                    "itnt",
                    "classic_nuke",
                    "reactor_vessel",
                    "reactor_chamber",
                    "reactor_access_hatch",
                    "reactor_redstone_port",
                    "coke_kiln_hatch",
                    "coke_kiln_grate");

    private FixedMenuCensus() {}

    static void run(ServerLevel level, ServerPlayer player, List<String> report) {
        var pos = new BlockPos(6, 148, 6);
        player.setPos(6.5, 148, 7.5);
        var opened = new TreeSet<String>();
        var previous = player.containerMenu;
        try {
            for (String name : MachineSideHost.registeredBlocks()) {
                if (STRUCTURE_PARTS.contains(name)) continue;
                var block = CompatHost.block(name);
                if (!(block instanceof EntityBlock factory)) continue;
                var state = block.defaultBlockState();
                var prototype = factory.newBlockEntity(pos, state);
                if (!(prototype instanceof MachineAccessTarget host)
                        || !host.exposesMachineAccess()) continue;
                level.setBlockAndUpdate(pos, state);
                var placed = level.getBlockEntity(pos);
                AbstractContainerMenu menu =
                        placed instanceof BoxNode box
                                ? box.menu(42, player.getInventory(), 0)
                                : placed instanceof MenuProvider provider
                                        ? provider.createMenu(42, player.getInventory(), player)
                                        : null;
                if (!(menu instanceof DeviceAccessMenu))
                    throw new AssertionError("Fixed machine lacks side menu: " + name);
                player.containerMenu = menu;
                if (!menu.clickMenuButton(player, DeviceAccessMenu.accessButton(4, 3))
                        || MachineAccess.packed(placed) != (3 << 8))
                    throw new AssertionError(
                            "Fixed machine rejects authorized side settings: " + name);
                opened.add(name);
                player.containerMenu = previous;
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        } finally {
            player.containerMenu = previous;
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        if (!opened.containsAll(
                List.of(
                        "macerator",
                        "ore_washing_plant",
                        "mfsu",
                        "metal_former",
                        "tank",
                        "personal_chest",
                        "industrial_workbench",
                        "coke_kiln"))) {
            throw new AssertionError("Registry census missing fixed devices: " + opened);
        }
        report.add(
                "Fixed machine menus: "
                        + opened.size()
                        + " registered devices accepted live native side settings: "
                        + String.join(", ", opened));
    }
}
