package com.industrialage.ia2.gametest;

import com.industrialage.ia2.machine.FuelGenerator;
import com.industrialage.ia2.machine.FuelGeneratorBlockEntity;
import com.industrialage.ia2.platform.RegistryFacade;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Exercises production combustion and persistence using a development-only block and fuel slot. */
final class GeneratorFixture {
    private static final BlockPos POS = new BlockPos(29, 90, 25);
    private final RegistryFacade.Handle<BlockItem> item;
    private final Supplier<BlockEntityType<Generator>> type;
    private final Properties evidence = new Properties();
    private int ticks;
    private boolean readSave;

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
        var entities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "ic2_gametest");
        type =
                entities.register(
                        "fuel_generator",
                        () -> BlockEntityType.Builder.of(this::create, block.get()).build(null));
        entities.register(bus);
    }

    private Generator create(BlockPos pos, BlockState state) {
        return new Generator(type.get(), pos, state);
    }

    boolean step(String phase, ServerLevel world, BiConsumer<String, Boolean> check) {
        if (++ticks > 700) {
            throw new AssertionError("Generator did not finish phase " + phase);
        }
        if (phase.equals("write") && ticks == 1) {
            world.setChunkForced(POS.getX() >> 4, POS.getZ() >> 4, true);
            var floor = POS.below();
            world.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            var player = FakePlayerFactory.getMinecraft(world);
            player.setPos(POS.getX() + 0.5, POS.getY(), POS.getZ() - 1);
            var stack = new ItemStack(item.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atBottomCenterOf(POS), Direction.UP, floor, false);
            var action =
                    player.gameMode.useItemOn(player, world, stack, InteractionHand.MAIN_HAND, hit);
            check.accept("generator-placed-with-item", action.consumesAction() && stack.isEmpty());
            requireGenerator(world).insert(new ItemStack(Items.LAVA_BUCKET));
            return false;
        }
        var machine = requireGenerator(world);
        return phase.equals("write")
                ? beforeSave(machine, check)
                : afterRestart(world, machine, check);
    }

    private boolean beforeSave(Generator machine, BiConsumer<String, Boolean> check) {
        var state = machine.savedState();
        if (machine.fuel.is(Items.LAVA_BUCKET)) {
            // A tick through the native block ticker must actually have occurred.
            if (machine.hostTicks == 0) {
                return false;
            }
            check.accept(
                    "generator-rejected-lava",
                    state.fuelTicks() == 0
                            && state.storedEnergy() == 0
                            && !machine.isActive()
                            && machine.fuel.getCount() == 1);
            machine.insert(new ItemStack(Items.COAL, 2));
            return false;
        }
        if (state.storedEnergy() < 370) {
            return false;
        }
        check.accept(
                "generator-coal-after-37-ticks",
                state.storedEnergy() == 370
                        && state.fuelTicks() == 363
                        && machine.workTicks == 37
                        && machine.isActive()
                        && machine.hasOneCoal());
        recordState("saved", state);
        evidence.setProperty("generator.workTicks", String.valueOf(machine.workTicks));
        evidence.setProperty("generator.coal", String.valueOf(machine.fuel.getCount()));
        return true;
    }

    private boolean afterRestart(
            ServerLevel world, Generator machine, BiConsumer<String, Boolean> check) {
        var state = machine.savedState();
        if (!readSave) {
            check.accept(
                    "generator-cold-state",
                    machine.loadedState != null
                            && machine.loadedState.storedEnergy() == 370
                            && machine.loadedState.fuelTicks() == 363
                            && machine.loadedOneCoal);
            recordState("loaded", machine.loadedState);
            readSave = true;
        }
        if (state.fuelTicks() > 0 || machine.isActive()) {
            if (state.storedEnergy() != 4000 - 10 * state.fuelTicks() || !machine.hasOneCoal()) {
                throw new AssertionError("Incorrect fuel/EU account after restart: " + state);
            }
            return false;
        }
        check.accept(
                "generator-finished-one-coal",
                state.storedEnergy() == 4000
                        && machine.hasOneCoal()
                        && machine.workTicks == 363
                        && machine.hostTicks > machine.workTicks);
        recordState("full", state);
        evidence.setProperty("generator.workTicks", String.valueOf(machine.workTicks));
        evidence.setProperty("generator.coal", String.valueOf(machine.fuel.getCount()));
        int dirtyBefore = machine.dirtyCount;
        double drawn = machine.drawPacket(64);
        check.accept(
                "generator-one-output-packet",
                drawn == 32
                        && machine.savedState().storedEnergy() == 3968
                        && machine.dirtyCount > dirtyBefore);
        evidence.setProperty("generator.drawnEU", String.valueOf(drawn));
        world.setChunkForced(POS.getX() >> 4, POS.getZ() >> 4, false);
        world.destroyBlock(POS, false);
        check.accept(
                "generator-removed-port",
                world.getBlockEntity(POS) == null && machine.drawPacket(32) == 0);
        return true;
    }

    private void recordState(String sample, FuelGenerator.Snapshot state) {
        evidence.setProperty("generator." + sample + ".eu", String.valueOf(state.storedEnergy()));
        evidence.setProperty(
                "generator." + sample + ".fuelTicks", String.valueOf(state.fuelTicks()));
    }

    void addEvidence(Properties report) {
        report.putAll(evidence);
        report.setProperty("generator.position", POS.toShortString());
    }

    private Generator requireGenerator(ServerLevel world) {
        return world.getBlockEntity(POS, type.get()).orElseThrow();
    }

    private final class GeneratorBlock extends Block implements EntityBlock {
        private GeneratorBlock() {
            super(Properties.of().strength(2));
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return create(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level, BlockState state, BlockEntityType<T> actual) {
            if (!(level instanceof ServerLevel) || actual != type.get()) {
                return null;
            }
            return (world, pos, block, entity) -> {
                if (entity instanceof Generator machine) {
                    int dirtyBefore = machine.dirtyCount;
                    machine.tickMachine();
                    if (machine.isActive() != (machine.dirtyCount > dirtyBefore)) {
                        throw new AssertionError("Incorrect generator save notification at " + pos);
                    }
                    machine.hostTicks++;
                    if (machine.isActive()) {
                        machine.workTicks++;
                    }
                }
            };
        }
    }

    private static final class Generator extends FuelGeneratorBlockEntity {
        private ItemStack fuel = ItemStack.EMPTY;
        private FuelGenerator.Snapshot loadedState;
        private boolean loadedOneCoal;
        private int hostTicks;
        private int workTicks;
        private int dirtyCount;

        private Generator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, FuelGenerator.DEFAULT_ENERGY_PER_TICK);
        }

        private void insert(ItemStack stack) {
            fuel = stack;
            setChanged();
        }

        private boolean hasOneCoal() {
            return fuel.is(Items.COAL) && fuel.getCount() == 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            dirtyCount++;
        }

        @Override
        protected ItemStack fuelStack() {
            return fuel;
        }

        @Override
        protected void consumeFuel() {
            fuel.shrink(1);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
            super.saveAdditional(tag, lookup);
            tag.put("testFuelSlot", fuel.saveOptional(lookup));
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
            super.loadAdditional(tag, lookup);
            fuel = ItemStack.parseOptional(lookup, tag.getCompound("testFuelSlot"));
            loadedState = savedState();
            loadedOneCoal = hasOneCoal();
        }
    }
}
