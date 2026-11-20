package com.industrialage.ia2.equipment;

import com.industrialage.ia2.client.ClientServices;
import com.industrialage.ia2.gameplay.EnergyNetwork;
import com.industrialage.ia2.gameplay.NativeGameplay;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native item and data menu for the EU reader. */
public final class MeterPlatform {
    private static Supplier<MenuType<MeterMenu>> menu;

    private MeterPlatform() {}

    /** Registers the EU reader and its server and client containers. */
    public static void install(
            DeferredRegister.Items items,
            DeferredRegister<MenuType<?>> menus,
            IEventBus modBus,
            Dist side) {
        menu =
                menus.register(
                        "meter",
                        () ->
                                new MenuType<>(
                                        (id, inventory) ->
                                                new MeterMenu(
                                                        menu.get(), id, inventory, null, null),
                                        FeatureFlags.DEFAULT_FLAGS));
        items.registerItem("meter", props -> new Reader(props.stacksTo(1)));
        if (side == Dist.CLIENT) Client.install(modBus);
    }

    static boolean metered(ServerLevel level, BlockPos pos) {
        return EnergyNetwork.metered(level, pos);
    }

    private static final class Reader extends Item {
        Reader(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
            if (!(context.getPlayer() instanceof ServerPlayer player))
                return InteractionResult.SUCCESS;
            var level = (ServerLevel) context.getLevel();
            var pos = context.getClickedPos();
            if (!metered(level, pos)) {
                NativeGameplay.overlay(
                        player, Component.translatable("item.ic2.meter.invalid_target"));
                return InteractionResult.FAIL;
            }
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, opener) ->
                                    new MeterMenu(menu.get(), id, inventory, level, pos),
                            Component.translatable("item.ic2.meter")));
            return InteractionResult.SUCCESS;
        }
    }

    private static final class Client {
        static void install(IEventBus modBus) {
            ClientServices.registerMenuScreen(modBus, menu, MeterScreen::new);
        }
    }
}
