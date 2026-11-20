package com.industrialage.ia2.forge.qa;

import static com.industrialage.ia2.forge.qa.PlayableGameTests.block;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.check;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.machine;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.player;
import static com.industrialage.ia2.forge.qa.PlayableGameTests.useOn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class MatterGameTests {
    private MatterGameTests() {}

    /** Scans from zero, records using a crouch-use, and exercises both replication modes. */
    @GameTest(template = "smoke", timeoutTicks = 3650)
    public static void fullNativeMatterChain(GameTestHelper helper) {
        var source = helper.absolutePos(new BlockPos(0, 6, 0));
        var scanner = source.offset(1, 0, 2);
        var patternStore = scanner.east();
        var duplicator = patternStore.east();
        var world = helper.getLevel();
        var facing = net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
        world.setBlockAndUpdate(
                source, block("mfsu").defaultBlockState().setValue(facing, Direction.EAST));
        var charged = machine(helper, source).saveWithoutMetadata();
        charged.putDouble("ic2_energy", 3_600_000);
        machine(helper, source).load(charged);
        for (var cable :
                new BlockPos[] {
                    source.east(),
                    source.east(2),
                    source.east(3),
                    scanner.north(),
                    duplicator.north()
                }) {
            world.setBlockAndUpdate(cable, block("glass_fibre_cable").defaultBlockState());
        }
        world.setBlockAndUpdate(scanner, block("uu_scanner").defaultBlockState());
        world.setBlockAndUpdate(patternStore, block("pattern_storage").defaultBlockState());
        world.setBlockAndUpdate(duplicator, block("replicator").defaultBlockState());
        var scans = itemPort(helper, scanner, Direction.UP);
        check(
                helper,
                scans.insertItem(0, new ItemStack(Items.COBBLESTONE), false).isEmpty(),
                "Accepted one real cobblestone through the native scanning port");
        var uu = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse("ic2:uu_matter"));
        var intake =
                machine(helper, duplicator)
                        .getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                        .orElseThrow(AssertionError::new);
        check(
                helper,
                intake.fill(new FluidStack(uu, 16000), FluidAction.EXECUTE) == 16000,
                "Filled the actual replicator tank");
        var outputs = itemPort(helper, duplicator, Direction.DOWN);
        var operator = player(helper, scanner);
        operator.setShiftKeyDown(true);
        operator.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        helper.runAtTickTime(
                3298,
                () ->
                        check(
                                helper,
                                machine(helper, scanner)
                                                .saveWithoutMetadata()
                                                .getInt("ic2_progress")
                                        < 3300,
                                "The native scanner did not finish before 3300 working ticks"));
        helper.runAtTickTime(
                3310,
                () -> {
                    var finished = machine(helper, scanner).saveWithoutMetadata();
                    check(
                            helper,
                            finished.getInt("ic2_progress") == 3300
                                    && patternItem(finished)
                                            .getString("id")
                                            .equals("minecraft:cobblestone"),
                            "A real cable powered all 3300 paid scanner ticks");
                    check(
                            helper,
                            useOn(helper, operator, scanner, Direction.NORTH, ItemStack.EMPTY)
                                    .consumesAction(),
                            "Player crouch-use accepted the completed scan");
                    check(
                            helper,
                            patternItem(machine(helper, patternStore).saveWithoutMetadata())
                                    .getString("id")
                                    .equals("minecraft:cobblestone"),
                            "Connected pattern storage received the scanned pattern");
                    check(
                            helper,
                            useOn(helper, operator, duplicator, Direction.NORTH, ItemStack.EMPTY)
                                    .consumesAction(),
                            "Player enabled one replication");
                });
        helper.runAtTickTime(
                3325,
                () -> {
                    var single = outputs.extractItem(0, 64, false);
                    check(
                            helper,
                            single.is(Items.COBBLESTONE)
                                    && single.getCount() == 1
                                    && machine(helper, duplicator)
                                                    .saveWithoutMetadata()
                                                    .getInt("ic2_mode")
                                            == 0,
                            "Single mode produced one item and stopped");
                    useOn(helper, operator, duplicator, Direction.NORTH, ItemStack.EMPTY);
                    useOn(helper, operator, duplicator, Direction.NORTH, ItemStack.EMPTY);
                });
        helper.runAtTickTime(
                3544,
                () -> {
                    var repeated = outputs.getStackInSlot(0);
                    check(
                            helper,
                            repeated.is(Items.COBBLESTONE) && repeated.getCount() == 64,
                            "Continuous mode produced exactly one full output stack");
                    check(
                            helper,
                            intake.getFluidInTank(0).getAmount() == 15999,
                            "The first 65 duplicates together accounted for 1 mB of UU");
                    useOn(helper, operator, duplicator, Direction.NORTH, ItemStack.EMPTY);
                    check(
                            helper,
                            machine(helper, duplicator).saveWithoutMetadata().getInt("ic2_mode")
                                    == 0,
                            "Player stopped replication and the mode persisted");
                    helper.succeed();
                });
    }

    private static CompoundTag patternItem(CompoundTag machine) {
        for (var item : machine.getList("Items", 10)) {
            var stack = (CompoundTag) item;
            if (stack.getByte("Slot") == 14) {
                return stack;
            }
        }
        return new CompoundTag();
    }

    private static IItemHandler itemPort(GameTestHelper helper, BlockPos pos, Direction direction) {
        return machine(helper, pos)
                .getCapability(ForgeCapabilities.ITEM_HANDLER, direction)
                .orElseThrow(AssertionError::new);
    }
}
