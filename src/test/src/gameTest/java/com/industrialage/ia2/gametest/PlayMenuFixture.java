package com.industrialage.ia2.gametest;

import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.gameplay.IndustrialMachine;
import com.industrialage.ia2.platform.NeoForgePlatform;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Owns a test-only server menu and command for the two-process client acceptance run. */
@Mod("ic2_gametest")
public final class PlayMenuFixture implements IExtensionPoint {
    private Supplier<MenuType<SampleMenu>> type;

    /** Installs this fixture only in the explicitly selected development profile. */
    public PlayMenuFixture(IEventBus bus, ModContainer container) {
        if (!Boolean.getBoolean("ic2.qa.clientAcceptance")) {
            return;
        }
        var menus = DeferredRegister.create(Registries.MENU, "ic2_gametest");
        type =
                menus.register(
                        "session",
                        () ->
                                new MenuType<>(
                                        (id, inventory) -> new SampleMenu(type.get(), id),
                                        FeatureFlags.DEFAULT_FLAGS));
        menus.register(bus);
        container.registerExtensionPoint(PlayMenuFixture.class, this);
        NeoForge.EVENT_BUS.addListener(
                RegisterCommandsEvent.class,
                event ->
                        event.getDispatcher()
                                .register(
                                        Commands.literal("ic2accept")
                                                .requires(source -> source.hasPermission(2))
                                                .then(
                                                        Commands.literal("menu")
                                                                .executes(
                                                                        context -> {
                                                                            var player =
                                                                                    context.getSource()
                                                                                            .getPlayerOrException();
                                                                            player.openMenu(
                                                                                    new SimpleMenuProvider(
                                                                                            (id,
                                                                                                    inventory,
                                                                                                    recipient) ->
                                                                                                    new SampleMenu(
                                                                                                            type
                                                                                                                    .get(),
                                                                                                            id),
                                                                                            Component
                                                                                                    .literal(
                                                                                                            "IC2 acceptance"
                                                                                                                    + " menu")));
                                                                            return 1;
                                                                        }))
                                                .then(
                                                        Commands.literal("values")
                                                                .executes(PlayMenuFixture::values))
                                                .then(
                                                        Commands.literal("macerator")
                                                                .executes(
                                                                        PlayMenuFixture::macerator))
                                                .then(
                                                        Commands.literal("flight")
                                                                .executes(PlayMenuFixture::flight))
                                                .then(
                                                        Commands.literal("stop-macerator")
                                                                .executes(
                                                                        PlayMenuFixture
                                                                                ::stopMacerator))
                                                .then(
                                                        Commands.literal("laser")
                                                                .executes(PlayMenuFixture::laser))
                                                .then(
                                                        Commands.literal("meter")
                                                                .executes(PlayMenuFixture::meter))
                                                .then(
                                                        Commands.literal("quantum")
                                                                .executes(PlayMenuFixture::quantum))
                                                .then(
                                                        Commands.literal("boat")
                                                                .then(
                                                                        Commands.argument(
                                                                                        "design",
                                                                                        StringArgumentType
                                                                                                .word())
                                                                                .executes(
                                                                                        PlayMenuFixture
                                                                                                ::boat)))));
    }

    static MenuType<SampleMenu> menuType() {
        return ModList.get()
                .getModContainerById("ic2_gametest")
                .orElseThrow()
                .getCustomExtension(PlayMenuFixture.class)
                .orElseThrow()
                .type
                .get();
    }

    private static int values(CommandContext<CommandSourceStack> context) {
        var platform =
                ModList.get()
                        .getModContainerById("ic2")
                        .orElseThrow()
                        .getCustomExtension(NeoForgePlatform.class)
                        .orElseThrow()
                        .services();
        var snapshot = platform.config().current().orElseThrow();
        double ratio =
                snapshot.get(
                        ActiveConfig.scalarKey(
                                "balance/energyRetainedInStorageBlockDrops", Double.class));
        int heat =
                snapshot.get(ActiveConfig.scalarKey("balance/fermenter/hU_per_run", Integer.class));
        context.getSource()
                .sendSuccess(
                        () -> Component.literal("IC2 checked values " + ratio + "/" + heat), false);
        return 1;
    }

    private static int macerator(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var level = player.serverLevel();
        var pos = player.blockPosition().east(2);
        var block =
                BuiltInRegistries.BLOCK
                        .getOptional(ResourceLocation.parse("ic2:macerator"))
                        .orElseThrow();
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        if (level.getBlockEntity(pos) instanceof IndustrialMachine machine) {
            machine.setItem(0, new ItemStack(Items.COAL, 2));
            machine.receive(1_200);
        }
        player.gameMode.useItemOn(
                player,
                level,
                ItemStack.EMPTY,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.WEST, pos, false));
        return 1;
    }

    private static int stopMacerator(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        player.serverLevel()
                .setBlockAndUpdate(player.blockPosition().east(2), Blocks.AIR.defaultBlockState());
        return 1;
    }

    private static int laser(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var item =
                BuiltInRegistries.ITEM
                        .getOptional(ResourceLocation.parse("ic2:mining_laser"))
                        .orElseThrow();
        var stack = new ItemStack(item);
        stack.set(energyComponent(), 2_000.0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItem(player, player.serverLevel(), stack, InteractionHand.MAIN_HAND);
        return 1;
    }

    private static int meter(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var level = player.serverLevel();
        var receiver = player.blockPosition().east(2);
        var source = receiver.north();
        var box =
                BuiltInRegistries.BLOCK
                        .getOptional(ResourceLocation.parse("ic2:batbox"))
                        .orElseThrow()
                        .defaultBlockState();
        level.setBlockAndUpdate(source, box.setValue(BlockStateProperties.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(
                receiver, box.setValue(BlockStateProperties.FACING, Direction.EAST));
        ((IndustrialMachine) level.getBlockEntity(source)).receive(32.5);
        ((IndustrialMachine) level.getBlockEntity(receiver)).receive(39_999.5);
        var tool = equipment("meter");
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        player.gameMode.useItemOn(
                player,
                level,
                tool,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(receiver), Direction.WEST, receiver, false));
        return 1;
    }

    private static int quantum(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var boots = equipment("quantum_boots");
        boots.set(energyComponent(), 9_000.0);
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.FEET, boots);
        return 1;
    }

    private static int boat(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var level = player.serverLevel();
        String kind = StringArgumentType.getString(context, "design");
        if (!java.util.Set.of("rubber_boat", "carbon_boat", "electric_boat").contains(kind))
            return 0;
        player.stopRiding();
        var center = new BlockPos(player.getBlockX() + 9, 80, player.getBlockZ());
        for (var pos : BlockPos.betweenClosed(center.offset(-3, 0, -3), center.offset(3, 0, 3))) {
            boolean wall =
                    Math.abs(pos.getX() - center.getX()) == 3
                            || Math.abs(pos.getZ() - center.getZ()) == 3;
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, (wall ? Blocks.STONE : Blocks.WATER).defaultBlockState());
        }
        player.connection.teleport(
                center.getX() - 2.5, center.getY() + 1.0, center.getZ() + 0.5, -90, 32);
        player.setItemInHand(InteractionHand.MAIN_HAND, equipment(kind));
        return 1;
    }

    private static ItemStack equipment(String path) {
        return new ItemStack(
                BuiltInRegistries.ITEM
                        .getOptional(ResourceLocation.fromNamespaceAndPath("ic2", path))
                        .orElseThrow());
    }

    static DataComponentType<Double> energyComponent() {
        @SuppressWarnings("unchecked")
        var key =
                (DataComponentType<Double>)
                        BuiltInRegistries.DATA_COMPONENT_TYPE
                                .getOptional(ResourceLocation.parse("ic2:eu"))
                                .orElseThrow();
        return key;
    }

    private static int flight(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var equipment =
                new ItemStack(
                        BuiltInRegistries.ITEM
                                .getOptional(ResourceLocation.parse("ic2:electric_jetpack"))
                                .orElseThrow());
        equipment.set(energyComponent(), 1_200.0);
        player.setItemSlot(EquipmentSlot.CHEST, equipment);
        return 1;
    }

    static final class SampleMenu extends AbstractContainerMenu {
        SampleMenu(MenuType<?> type, int id) {
            super(type, id);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
