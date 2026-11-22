package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.forge.ForgePlatform;
import com.industrialage.ia2.platform.ConfigFacade;
import com.industrialage.ia2.platform.RegistryFacade;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.RegisterEvent;

final class PlatformWorldFixture {
    private static final RegistryFacade.Id ID = new RegistryFacade.Id("ic2:qa_platform_pillar");
    private static final BlockPos REMOVED = new BlockPos(24, 90, 24);
    private static final BlockPos SURVIVOR = new BlockPos(28, 90, 24);
    private final String phase;
    private com.industrialage.ia2.logisticstest.PipeNozzleChecks nozzles;
    private final List<String> assertions = new ArrayList<>();
    private final RegistryFacade.Handle<Block> block;
    private final RegistryFacade.Handle<BlockItem> item;
    private final ConfigFacade config;
    private final PlatformWorldServices services;
    private final GeneratorFixture generator;
    private RegisterEvent blockEvent;
    private int blockCalls;
    private int itemCalls;
    private boolean ran;

    PlatformWorldFixture(String phase, IEventBus modBus) {
        this.phase = phase;
        var host =
                ModList.get()
                        .getModContainerById("ic2")
                        .orElseThrow()
                        .getCustomExtension(ForgePlatform.class)
                        .orElseThrow();
        var registry = host.services().registry();
        generator = new GeneratorFixture(registry, modBus);
        config = host.services().config();
        var blocks = new RegistryFacade.Kind<>("block", Block.class);
        block =
                registry.register(
                        blocks,
                        ID,
                        () -> {
                            blockCalls++;
                            return new RotatedPillarBlock(
                                    BlockBehaviour.Properties.of(Material.WOOD).strength(0.5F));
                        });
        item =
                registry.register(
                        new RegistryFacade.Kind<>("item", BlockItem.class),
                        ID,
                        () -> {
                            itemCalls++;
                            return new BlockItem(block.get(), new Item.Properties());
                        });
        PlatformRegistrationFailures.expect(IllegalStateException.class, block::get);
        PlatformRegistrationFailures.expect(IllegalStateException.class, item::get);
        PlatformRegistrationFailures.expect(
                IllegalStateException.class,
                () -> registry.register(blocks, ID, () -> Blocks.GOLD_BLOCK));
        check("factories-deferred", blockCalls == 0 && itemCalls == 0);
        services = new PlatformWorldServices(host, block);
        new PlatformLifecycleTrace(phase, host.services().events());
        modBus.addListener(
                net.minecraftforge.eventbus.api.EventPriority.NORMAL,
                false,
                RegisterEvent.class,
                event -> {
                    if (event.getRegistryKey().equals(Registry.BLOCK_REGISTRY)) {
                        blockEvent = event;
                    }
                });
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
    }

    private void onTick(TickEvent.ServerTickEvent tick) {
        if (ran || tick.phase != TickEvent.Phase.END) {
            return;
        }
        var server = tick.getServer();
        var world = server.overworld();
        try {
            if (nozzles == null) {
                world.setChunkForced(0, 0, true);
                world.getChunk(0, 0);
                nozzles = NozzleGameTests.matrix(new BlockPos(3, 132, 3));
                var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(world);
                if ("write".equals(phase)) {
                    nozzles.place(world, player, this::check);
                    return;
                }
                nozzles.letDeliver(world, player, "cold", this::check);
            }
            boolean transportReady =
                    "write".equals(phase)
                            ? nozzles.waitForBackpressure(world, this::check)
                            : nozzles.delivered(world, this::check);
            if (!transportReady || !generator.step(phase, world, this::check)) return;
            ran = true;
            var id = new ResourceLocation(ID.value());
            check(
                    "block-bound-once",
                    block.get() == Registry.BLOCK.getOptional(id).orElseThrow()
                            && block.get() == block.get()
                            && blockCalls == 1);
            check(
                    "item-bound-once",
                    item.get() == Registry.ITEM.getOptional(id).orElseThrow()
                            && item.get() == item.get()
                            && itemCalls == 1);
            if ("write".equals(phase)) {
                ForgeArmorChecks.run(world, this::check);
                editWorld(world);
                server.saveEverything(false, true, true);
            } else if (!"read".equals(phase)) {
                throw new IllegalArgumentException("Unexpected world phase: " + phase);
            }
            check(
                    "external-block-tag",
                    block.get()
                            .defaultBlockState()
                            .is(
                                    TagKey.create(
                                            Registry.BLOCK_REGISTRY,
                                            new ResourceLocation("ic2_qa:platform_blocks"))));
            check(
                    "external-item-tag",
                    new ItemStack(item.get())
                            .is(
                                    TagKey.create(
                                            Registry.ITEM_REGISTRY,
                                            new ResourceLocation("ic2_qa:platform_items"))));
            var first = world.getBlockState(REMOVED);
            var second = world.getBlockState(SURVIVOR);
            check("removed-is-air", first.isAir());
            check(
                    "retained-axis-x",
                    second.is(block.get())
                            && second.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X);
            var report = new Properties();
            report.setProperty(
                    "world", server.getWorldPath(LevelResource.ROOT).toRealPath().toString());
            report.setProperty("dimension", world.dimension().location().toString());
            report.setProperty("removed", REMOVED.toShortString() + " " + state(first));
            report.setProperty("retained", SURVIVOR.toShortString() + " " + state(second));
            report.setProperty("run", Files.readString(Path.of("run-id")).trim());
            if ("read".equals(phase)) {
                var saved = new Properties();
                try (var reader = Files.newBufferedReader(Path.of("write.properties"))) {
                    saved.load(reader);
                }
                for (String key : report.stringPropertyNames()) {
                    check(
                            "reloaded-" + key,
                            report.getProperty(key).equals(saved.getProperty(key)));
                }
            }
            PlatformWorldConfig.verify(phase, server, config);
            services.watchWorldUnload(world, SURVIVOR, phase);
            generator.evidence(report);
            report.setProperty("phase", phase);
            report.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
            report.setProperty("assertions", String.join(",", assertions));
            report.setProperty("passedChecks", Integer.toString(assertions.size()));
            report.setProperty("failedChecks", "0");
            report.setProperty("status", "passed");
            try (var writer = Files.newBufferedWriter(Path.of(phase + ".properties"))) {
                report.store(writer, "IndustrialCraft 2 dedicated server assertions");
            }
        } catch (IOException e) {
            ran = true;
            recordFailure(e);
            throw new UncheckedIOException(e);
        } catch (RuntimeException | AssertionError failure) {
            ran = true;
            recordFailure(failure);
            throw failure;
        } finally {
            if (ran) server.halt(false);
        }
    }

    private void recordFailure(Throwable error) {
        try {
            Files.writeString(
                    Path.of(phase + "-failure.txt"),
                    "completedChecks="
                            + assertions.size()
                            + " failedChecks="
                            + (error instanceof AssertionError ? 1 : 0)
                            + "\n"
                            + error
                            + "\n"
                            + String.join(",", assertions));
        } catch (IOException writeFailure) {
            error.addSuppressed(writeFailure);
        }
    }

    private void editWorld(ServerLevel world) {
        check("observed-block-registration", blockEvent != null);
        PlatformRegistrationFailures.verify(blockEvent, this::check);
        var actor = FakePlayerFactory.getMinecraft(world);
        actor.setPos(26.5, 90, 21.5);
        var stack = new ItemStack(item.get(), 2);
        actor.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (var pos : List.of(REMOVED, SURVIVOR)) {
            check("initial-air-" + pos.getX(), world.getBlockState(pos).isAir());
            var support = pos.west();
            world.setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
            var hit =
                    new BlockHitResult(
                            Vec3.atLowerCornerOf(support).add(1, 0.5, 0.5),
                            Direction.EAST,
                            support,
                            false);
            var result =
                    actor.gameMode.useItemOn(actor, world, stack, InteractionHand.MAIN_HAND, hit);
            check(
                    "placed-" + pos.getX(),
                    result.consumesAction() && world.getBlockState(pos).is(block.get()));
        }
        services.checkBeforeRemoval(world, REMOVED, this::check);
        check("destroy-action", actor.gameMode.destroyBlock(REMOVED));
        services.checkAfterRemoval(world, REMOVED, this::check);
    }

    private static String state(BlockState state) {
        var id = Registry.BLOCK.getKey(state.getBlock()).toString();
        return state.hasProperty(RotatedPillarBlock.AXIS)
                ? id + "[axis=" + state.getValue(RotatedPillarBlock.AXIS) + "]"
                : id;
    }

    private void check(String label, boolean passed) {
        if (!passed) {
            throw new AssertionError("Platform world " + phase + ": " + label);
        }
        assertions.add(label);
    }
}
