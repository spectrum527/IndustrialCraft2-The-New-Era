package com.industrialage.ia2.equipment;

import com.industrialage.ia2.forge.client.ForgeClient;
import com.industrialage.ia2.forge.content.ForgeContent;
import com.industrialage.ia2.forge.content.ForgePower;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;

/** Native item and data menu for the Forge EU reader. */
public final class MeterPlatform {
    private static Supplier<MenuType<MeterMenu>> menu;

    private MeterPlatform() {}

    /** Registers the EU reader and its server and client containers. */
    public static void install(
            DeferredRegister<Item> items, DeferredRegister<MenuType<?>> menus, IEventBus modBus) {
        menu =
                menus.register(
                        "meter",
                        () ->
                                IForgeMenuType.create(
                                        (id, inventory, data) ->
                                                new MeterMenu(
                                                        menu.get(), id, inventory, null, null)));
        items.register("meter", () -> new Reader(ForgeContent.itemProperties("meter").stacksTo(1)));
        if (FMLEnvironment.dist == Dist.CLIENT) Client.install(modBus);
    }

    static boolean metered(ServerLevel level, BlockPos pos) {
        return ForgePower.metered(level, pos);
    }

    private static final class Reader extends Item {
        Reader(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
            if (!(context.getPlayer() instanceof ServerPlayer player))
                return InteractionResult.SUCCESS;
            var level = player.getLevel();
            var pos = context.getClickedPos();
            if (!metered(level, pos)) {
                player.displayClientMessage(
                        Component.translatable("item.ic2.meter.invalid_target"), true);
                return InteractionResult.FAIL;
            }
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, opener) ->
                                    new MeterMenu(menu.get(), id, inventory, level, pos),
                            Component.translatable("item.ic2.meter")));
            return InteractionResult.CONSUME;
        }
    }

    private static final class Client {
        static void install(IEventBus modBus) {
            ForgeClient.registerMenuScreen(modBus, menu, MeterScreen::new);
        }
    }
}
