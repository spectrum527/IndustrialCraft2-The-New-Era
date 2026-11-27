package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.machine.FuelGenerator;
import com.industrialage.ia2.platform.RegistryFacade;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Samples the live production generator only after native ticks or inside its native load callback.
 */
final class GeneratorFixture {
    private static final BlockPos POS = new BlockPos(29, 90, 25);
    private final RegistryFacade.Handle<BlockItem> item;
    private final Supplier<BlockEntityType<GeneratorProbe>> type;
    private final Properties evidence = new Properties();
    private int elapsed;

    GeneratorFixture(RegistryFacade registry, IEventBus bus) {
        var id = new RegistryFacade.Id("ic2:qa_fuel_generator");
        var block =
                registry.register(
                        new RegistryFacade.Kind<>("block", Block.class), id, GeneratorBlock::new);
        item =
                registry.register(
                        new RegistryFacade.Kind<>("item", BlockItem.class),
                        id,
                        () -> new BlockItem(block.get(), new Item.Properties()));
        var entities = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "ic2_qa");
        type =
                entities.register(
                        "fuel_generator",
                        () -> BlockEntityType.Builder.of(this::create, block.get()).build(null));
        entities.register(bus);
    }

    boolean step(String phase, ServerLevel world, BiConsumer<String, Boolean> check)
            throws IOException {
        if (++elapsed > 700)
            throw new AssertionError("Native generator ticks stalled during " + phase);
        if (phase.equals("write") && elapsed == 1) {
            world.setChunkForced(POS.getX() >> 4, POS.getZ() >> 4, true);
            world.setBlockAndUpdate(POS.below(), Blocks.STONE.defaultBlockState());
            var player = FakePlayerFactory.getMinecraft(world);
            player.setPos(POS.getX() + 0.5, POS.getY(), POS.getZ() - 1);
            var stack = new ItemStack(item.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit =
                    new BlockHitResult(
                            Vec3.atBottomCenterOf(POS), Direction.UP, POS.below(), false);
            var result =
                    player.gameMode.useItemOn(player, world, stack, InteractionHand.MAIN_HAND, hit);
            check.accept("generator-item-placement", result.consumesAction() && stack.isEmpty());
            machine(world).insert(new ItemStack(Items.LAVA_BUCKET));
            return false;
        }
        var generator = machine(world);
        return phase.equals("write")
                ? beforeSave(generator, check)
                : afterLoad(world, generator, check);
    }

    private boolean beforeSave(GeneratorProbe generator, BiConsumer<String, Boolean> check)
            throws IOException {
        var state = generator.savedState();
        if (generator.fuel.is(Items.LAVA_BUCKET)) {
            if (generator.nativeTicks == 0) return false;
            check.accept(
                    "generator-lava-retained",
                    !generator.isActive()
                            && generator.fuel.getCount() == 1
                            && state.equals(new FuelGenerator.Snapshot(0, 0))
                            && generator.workTicks == 0);
            record("lava", state, generator.fuel, generator.workTicks);
            generator.insert(new ItemStack(Items.COAL, 2));
            return false;
        }
        if (generator.workTicks < 37) return false;
        check.accept(
                "generator-saved-37",
                generator.workTicks == 37
                        && generator.isActive()
                        && state.equals(new FuelGenerator.Snapshot(363, 370))
                        && oneCoal(generator.fuel));
        record("saved", state, generator.fuel, generator.workTicks);
        NbtIo.writeCompressed(generator.saveWithFullMetadata(), new File("generator-write.nbt"));
        return true;
    }

    private boolean afterLoad(
            ServerLevel world, GeneratorProbe generator, BiConsumer<String, Boolean> check)
            throws IOException {
        if (elapsed == 1) {
            var saved = generator.firstLoad;
            check.accept(
                    "generator-native-cold-read",
                    saved != null
                            && generator.ticksAtLoad == 0
                            && generator.stateAtLoad.equals(new FuelGenerator.Snapshot(363, 370))
                            && oneCoal(generator.slotAtLoad)
                            && "ic2_qa:fuel_generator".equals(saved.getString("id"))
                            && saved.getInt("x") == POS.getX()
                            && saved.getInt("y") == POS.getY()
                            && saved.getInt("z") == POS.getZ()
                            && IntTag.valueOf(363)
                                    .equals(saved.getCompound("generator").get("fuel_ticks"))
                            && DoubleTag.valueOf(370)
                                    .equals(saved.getCompound("generator").get("stored_energy"))
                            && oneCoal(ItemStack.of(saved.getCompound("qa_slot"))));
            record("cold", generator.stateAtLoad, generator.slotAtLoad, generator.ticksAtLoad);
            NbtIo.writeCompressed(saved, new File("generator-read.nbt"));
        }
        var state = generator.savedState();
        if (state.fuelTicks() != 0 || generator.isActive()) {
            if (state.storedEnergy() != 4000 - state.fuelTicks() * 10 || !oneCoal(generator.fuel)) {
                throw new AssertionError(
                        "Incorrect fuel or EU while resuming the saved coal: " + state);
            }
            return false;
        }
        check.accept(
                "generator-full-idle",
                generator.nativeTicks > generator.workTicks
                        && generator.workTicks == 363
                        && state.equals(new FuelGenerator.Snapshot(0, 4000))
                        && oneCoal(generator.fuel));
        record("full", state, generator.fuel, generator.workTicks);
        var port = (java.util.function.DoubleUnaryOperator) generator::drawPacket;
        int before = generator.dirtyNotifications;
        double drawn = port.applyAsDouble(64);
        check.accept(
                "generator-max-packet",
                drawn == 32
                        && generator.dirtyNotifications > before
                        && generator.savedState().equals(new FuelGenerator.Snapshot(0, 3968))
                        && oneCoal(generator.fuel));
        record("afterPacket", generator.savedState(), generator.fuel, generator.workTicks);
        evidence.setProperty("generator.drawnEU", Double.toString(drawn));
        world.setChunkForced(POS.getX() >> 4, POS.getZ() >> 4, false);
        boolean destroyed = world.destroyBlock(POS, false);
        double fromOldPort = port.applyAsDouble(32);
        check.accept(
                "generator-removed-port",
                destroyed
                        && world.getBlockEntity(POS) == null
                        && world.getBlockState(POS).isAir()
                        && fromOldPort == 0
                        && generator.savedState().storedEnergy() == 3968);
        evidence.setProperty("generator.removedPortEU", Double.toString(fromOldPort));
        return true;
    }

    private static boolean oneCoal(ItemStack stack) {
        return stack.is(Items.COAL) && stack.getCount() == 1;
    }

    private void record(String sample, FuelGenerator.Snapshot state, ItemStack slot, int ticks) {
        String key = "generator." + sample + ".";
        evidence.setProperty(key + "fuelTicks", Integer.toString(state.fuelTicks()));
        evidence.setProperty(key + "eu", Double.toString(state.storedEnergy()));
        evidence.setProperty(key + "item", ForgeRegistries.ITEMS.getKey(slot.getItem()).toString());
        evidence.setProperty(key + "count", Integer.toString(slot.getCount()));
        evidence.setProperty(key + "workTicks", Integer.toString(ticks));
    }

    void evidence(Properties report) {
        report.putAll(evidence);
        report.setProperty("generator.position", POS.toShortString());
        for (String id : new String[] {"ic2", "ic2_qa"}) {
            var file = ModList.get().getModFileById(id).getFile().getFilePath();
            report.setProperty("generator.modFile." + id, file.toAbsolutePath().toString());
        }
    }

    private GeneratorProbe machine(ServerLevel world) {
        return world.getBlockEntity(POS, type.get()).orElseThrow();
    }

    private GeneratorProbe create(BlockPos pos, BlockState state) {
        return new GeneratorProbe(type.get(), pos, state);
    }

    private final class GeneratorBlock extends Block implements EntityBlock {
        GeneratorBlock() {
            super(Properties.of(Material.METAL).strength(2));
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return create(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level, BlockState state, BlockEntityType<T> actual) {
            if (!(level instanceof ServerLevel) || actual != type.get()) return null;
            return (world, pos, block, entity) -> ((GeneratorProbe) entity).observeNativeTick();
        }
    }
}
