package com.industrialage.ia2.compatqa;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.logistics.TransportNode;
import com.industrialage.ia2.world.MachineAutomation;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Exercises opposing upgrades against a physically fitted pipe and the native transfer APIs. */
final class PipeUpgradeChecks {
    private PipeUpgradeChecks() {}

    static void run(ServerLevel level, ServerPlayer player, List<String> report) {
        var pos = new BlockPos(6, 100, 6);
        var adjacent = pos.east();
        level.setBlockAndUpdate(pos, CompatHost.block("metal_former").defaultBlockState());
        var node = level.getBlockEntity(pos);
        var menu = ((MenuProvider) node).createMenu(23, player.getInventory(), player);
        Container storage = menu.getSlot(0).container;
        for (boolean eject : List.of(false, true)) {
            for (String prefix : List.of("", "advanced_")) {
                String upgradeName = prefix + (eject ? "ejector_upgrade" : "pulling_upgrade");
                var upgrade = new ItemStack(CompatHost.item(upgradeName));
                String nozzle = eject ? "pipe_injection_nozzle" : "pipe_extraction_nozzle";
                level.setBlockAndUpdate(adjacent, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(
                        adjacent,
                        CompatHost.block("bronze_item_pipe_small")
                                .defaultBlockState()
                                .setValue(
                                        BlockStateProperties.FACING,
                                        eject ? Direction.EAST : Direction.WEST));
                fit(level, player, adjacent, nozzle);
                if (eject) storage.setItem(1, new ItemStack(Items.COBBLESTONE));
                else {
                    storage.setItem(0, ItemStack.EMPTY);
                    var inlet = MachineSideHost.item(level, adjacent, Direction.UP);
                    require(
                            inlet != null && inlet.insert(1, false) == 1,
                            upgradeName + " seeds pipe");
                }
                var pipe = (TransportNode) level.getBlockEntity(adjacent);
                require(
                        pipe.blocksUpgrade(Direction.WEST, eject, false)
                                && !pipe.blocksUpgrade(Direction.WEST, eject, true)
                                && !pipe.blocksUpgrade(Direction.WEST, !eject, false),
                        upgradeName + " channel direction");
                require(
                        !MachineAccess.allowsUpgrade(node, Direction.EAST, eject, false)
                                && MachineAccess.allowsUpgrade(node, Direction.EAST, !eject, false)
                                && MachineAccess.allowsUpgrade(node, Direction.SOUTH, eject, false),
                        upgradeName + " scoped face");
                require(
                        !MachineAutomation.transfer(
                                level, pos, storage, 0, 1, List.of(upgrade), stack -> true),
                        upgradeName + " pauses against its opposing nozzle");
                require(
                        eject == !storage.getItem(eject ? 1 : 0).isEmpty(),
                        upgradeName + " held contents");
                fit(level, player, adjacent, "crowbar");
                require(
                        !pipe.blocksUpgrade(Direction.WEST, eject, false),
                        upgradeName + " released face");
                require(
                        MachineAutomation.transfer(
                                level, pos, storage, 0, 1, List.of(upgrade), stack -> true),
                        upgradeName + " resumes after removing nozzle");
                require(
                        eject == storage.getItem(eject ? 1 : 0).isEmpty(),
                        upgradeName + " delivered contents");
            }
        }
        for (boolean eject : List.of(false, true)) {
            level.setBlockAndUpdate(adjacent, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(
                    adjacent, CompatHost.block("steel_fluid_pipe_small").defaultBlockState());
            fit(
                    level,
                    player,
                    adjacent,
                    eject ? "pipe_injection_nozzle" : "pipe_extraction_nozzle");
            require(
                    !MachineAccess.allowsUpgrade(node, Direction.EAST, eject, true)
                            && MachineAccess.allowsUpgrade(node, Direction.EAST, !eject, true)
                            && MachineAccess.allowsUpgrade(node, Direction.EAST, eject, false)
                            && MachineAccess.allowsUpgrade(node, Direction.SOUTH, eject, true),
                    "fluid upgrade only pauses on its own fitting");
            fit(level, player, adjacent, "crowbar");
            require(
                    MachineAccess.allowsUpgrade(node, Direction.EAST, eject, true),
                    "fluid upgrade direction restored");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        level.setBlockAndUpdate(adjacent, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        report.add(
                "Nozzle upgrades: normal and advanced item pull/eject pause only at their opposing fitting and resume delivery after removal. Both fluid routes distinguish the medium and direction.");
    }

    private static void fit(ServerLevel level, ServerPlayer player, BlockPos pos, String id) {
        var held = new ItemStack(CompatHost.item(id));
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.setPos(pos.getX() - .1, pos.getY() + .5, pos.getZ() + .5);
        var point = Vec3.atCenterOf(pos).add(-.5, 0, 0);
        player.gameMode.useItemOn(
                player,
                level,
                held,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(point, Direction.WEST, pos, false));
    }

    private static void require(boolean result, String label) {
        if (!result) throw new AssertionError(label);
    }
}
