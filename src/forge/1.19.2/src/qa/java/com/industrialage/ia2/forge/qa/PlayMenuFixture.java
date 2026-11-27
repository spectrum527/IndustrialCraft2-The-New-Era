package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.forge.ForgePlatform;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Owns a test-only server menu and command for the two-process client acceptance run. */
final class PlayMenuFixture {
    private Supplier<MenuType<SampleMenu>> type;

    /** Installs this fixture only in the explicitly selected development profile. */
    PlayMenuFixture(IEventBus bus) {
        var menus = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "ic2_qa");
        type =
                menus.register(
                        "session",
                        () ->
                                IForgeMenuType.create(
                                        (id, inventory, data) -> new SampleMenu(type.get(), id)));
        menus.register(bus);
        MinecraftForge.EVENT_BUS.addListener(
                (RegisterCommandsEvent event) ->
                        event.getDispatcher()
                                .register(
                                        Commands.literal("ic2accept")
                                                .requires(source -> source.hasPermission(2))
                                                .then(Commands.literal("menu").executes(this::open))
                                                .then(
                                                        Commands.literal("values")
                                                                .executes(
                                                                        PlayMenuFixture::values))));
    }

    private int open(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, recipient) -> new SampleMenu(type.get(), id),
                        Component.literal("IC2 acceptance menu")));
        return 1;
    }

    MenuType<SampleMenu> menuType() {
        return type.get();
    }

    private static int values(CommandContext<CommandSourceStack> context) {
        var platform =
                ModList.get()
                        .getModContainerById("ic2")
                        .orElseThrow()
                        .getCustomExtension(ForgePlatform.class)
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
                .sendSuccess(Component.literal("IC2 checked values " + ratio + "/" + heat), false);
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
