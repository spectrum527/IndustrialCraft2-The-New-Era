package com.industrialage.ia2.modernsmoke;

import com.industrialage.ia2.machine.FuelGenerator;
import com.industrialage.ia2.machine.FuelGeneratorBlockEntity;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Isolated 26.x server fixture; all machine behavior under test is production code. */
@Mod("ic2_modern_smoke")
public final class ModernServerSmoke {
    private static final BlockPos POS = new BlockPos(16, 90, 16);
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks("ic2_modern_smoke");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "ic2_modern_smoke");
    private static final Supplier<GeneratorBlock> BLOCK =
            BLOCKS.registerBlock("generator", GeneratorBlock::new);
    private static final Supplier<BlockEntityType<Generator>> TYPE =
            ENTITIES.register(
                    "generator", () -> new BlockEntityType<>(Generator::new, BLOCK.get()));

    private int stage;
    private int ticks;
    private boolean roundTrip;
    private String minecraft;
    private String neoforge;
    private String ic2Version;

    /** Installs only the development fixture and its server tick listener. */
    public ModernServerSmoke(IEventBus bus) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, this::tick);
    }

    private void tick(ServerTickEvent.Post event) {
        if (stage == 3) {
            return;
        }
        var server = event.getServer();
        try {
            require(++ticks < 1000, "Generator smoke timed out");
            var world = server.overworld();
            if (stage == 0) {
                minecraft = SharedConstants.getCurrentVersion().name();
                neoforge = version("neoforge");
                ic2Version = version("ic2");
                require(
                        minecraft.equals(System.getProperty("ic2.smoke.minecraft")),
                        "Minecraft version");
                require(
                        neoforge.equals(System.getProperty("ic2.smoke.neoforge")),
                        "NeoForge version");
                require(ModList.get().isLoaded("ic2"), "IC2 mod loaded");
                world.setChunkForced(POS.getX() >> 4, POS.getZ() >> 4, true);
                require(
                        world.setBlockAndUpdate(POS, BLOCK.get().defaultBlockState()),
                        "Block placed");
                machine(world).insert(new ItemStack(Items.LAVA_BUCKET));
                stage = 1;
                return;
            }
            var machine = machine(world);
            if (stage == 1) {
                if (machine.hostTicks == 0) {
                    return;
                }
                require(
                        machine.savedState().equals(new FuelGenerator.Snapshot(0, 0)),
                        "Lava rejected");
                require(machine.fuel.is(Items.LAVA_BUCKET), "Lava not consumed");
                machine.insert(new ItemStack(Items.COAL));
                stage = 2;
                return;
            }
            var snapshot = machine.savedState();
            if (!roundTrip && snapshot.storedEnergy() >= 370) {
                require(snapshot.equals(new FuelGenerator.Snapshot(363, 370)), "Coal at 37 ticks");
                var tag = machine.saveWithFullMetadata(world.registryAccess());
                var restored =
                        BlockEntity.loadStatic(
                                POS, machine.getBlockState(), tag, world.registryAccess());
                require(restored instanceof Generator, "Native block entity load");
                require(
                        ((Generator) restored).savedState().equals(snapshot),
                        "Generator codec round trip");
                roundTrip = true;
            }
            if (snapshot.fuelTicks() == 0 && snapshot.storedEnergy() == 4000) {
                require(roundTrip && machine.workTicks == 400, "One coal produced 4000 EU");
                require(machine.fuel.isEmpty(), "One coal consumed");
                require(machine.drawPacket(64) == 32, "32 EU packet limit");
                require(machine.savedState().storedEnergy() == 3968, "Packet debited");
                require(server.saveEverything(false, true, true), "World save");
                writeSuccess();
                stage = 3;
                server.halt(false);
            }
        } catch (IOException | RuntimeException | AssertionError failure) {
            stage = 3;
            var trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            try {
                Files.writeString(Path.of("failure.txt"), trace.toString(), StandardCharsets.UTF_8);
            } catch (IOException writeFailure) {
                failure.addSuppressed(writeFailure);
                throw new IllegalStateException("Cannot record smoke failure", failure);
            } finally {
                server.halt(false);
            }
        }
    }

    private void writeSuccess() throws IOException {
        var result = new Properties();
        result.setProperty("status", "passed");
        result.setProperty("runId", System.getProperty("ic2.smoke.runId"));
        result.setProperty("minecraft", minecraft);
        result.setProperty("neoforge", neoforge);
        result.setProperty("ic2Version", ic2Version);
        result.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        result.setProperty(
                "checks",
                "ic2-loaded,lava-rejected,coal-4000-eu,native-codec,packet-32,world-save");
        result.setProperty("codecBeforeSave", "363 ticks, 370 EU");
        result.setProperty("finalEnergy", "3968");
        var temporary = Path.of("ready.properties.tmp");
        try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            result.store(writer, "26.x dedicated server smoke");
        }
        Files.move(temporary, Path.of("ready.properties"), StandardCopyOption.ATOMIC_MOVE);
    }

    private static String version(String id) {
        return ModList.get()
                .getModContainerById(id)
                .orElseThrow()
                .getModInfo()
                .getVersion()
                .toString();
    }

    private static Generator machine(ServerLevel world) {
        var entity = world.getBlockEntity(POS);
        require(entity instanceof Generator, "Generator block entity exists");
        return (Generator) entity;
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static final class GeneratorBlock extends Block implements EntityBlock {
        private GeneratorBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Generator(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level, BlockState state, BlockEntityType<T> actual) {
            if (!(level instanceof ServerLevel) || actual != TYPE.get()) {
                return null;
            }
            return (world, pos, block, entity) -> {
                if (entity instanceof Generator generator) {
                    generator.tickMachine();
                    generator.hostTicks++;
                    if (generator.isActive()) {
                        generator.workTicks++;
                    }
                }
            };
        }
    }

    private static final class Generator extends FuelGeneratorBlockEntity {
        private ItemStack fuel = ItemStack.EMPTY;
        private int hostTicks;
        private int workTicks;

        private Generator(BlockPos pos, BlockState state) {
            super(TYPE.get(), pos, state, FuelGenerator.DEFAULT_ENERGY_PER_TICK);
        }

        private void insert(ItemStack item) {
            fuel = item;
            setChanged();
        }

        @Override
        protected ItemStack fuelStack() {
            return fuel;
        }

        @Override
        protected void consumeFuel() {
            fuel.shrink(1);
        }
    }
}
