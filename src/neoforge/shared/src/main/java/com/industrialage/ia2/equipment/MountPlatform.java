package com.industrialage.ia2.equipment;

import com.industrialage.ia2.gameplay.PortableGear;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Saves mounted flight units as a component and observes native armor break operations. */
public final class MountPlatform {
    private static Supplier<DataComponentType<Double>> mounted;
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, "ic2");
    private static final Supplier<RecipeSerializer<?>> ATTACHMENT =
            RECIPES.register("jetpack_attachment", NativeMount::recipe);

    private MountPlatform() {}

    /** Registers the mounting plate, crafting table recipe and mounted energy data. */
    public static void install(
            DeferredRegister.Items items,
            DeferredRegister<DataComponentType<?>> components,
            IEventBus modBus,
            IEventBus gameBus) {
        items.registerSimpleItem("jetpack_attachment_plate");
        mounted =
                components.register(
                        "mounted_jetpack",
                        () ->
                                DataComponentType.<Double>builder()
                                        .persistent(Codec.doubleRange(0, 30_000))
                                        .networkSynchronized(ByteBufCodecs.DOUBLE)
                                        .build());
        RECIPES.register(modBus);
        gameBus.addListener(
                LivingIncomingDamageEvent.class,
                event -> {
                    if (event.getEntity() instanceof ServerPlayer player)
                        JetpackMount.beforeDamage(player);
                });
        gameBus.addListener(
                LivingDamageEvent.Post.class,
                event -> {
                    if (event.getEntity() instanceof ServerPlayer player)
                        JetpackMount.afterDamage(player);
                });
        gameBus.addListener(
                ItemTooltipEvent.class,
                event -> {
                    if (JetpackMount.installed(event.getItemStack())) {
                        event.getToolTip()
                                .add(
                                        Component.translatable(
                                                "item.ic2.jetpack.mounted",
                                                Math.round(
                                                        JetpackMount.energy(
                                                                event.getItemStack()))));
                    }
                });
    }

    static RecipeSerializer<?> serializer() {
        return ATTACHMENT.get();
    }

    static String itemId(ItemStack stack) {
        return NativeMount.itemId(stack);
    }

    static boolean chestArmor(ItemStack stack) {
        return NativeMount.chestArmor(stack);
    }

    static double packEnergy(ItemStack stack) {
        return PortableGear.stored(stack);
    }

    static double read(ItemStack stack) {
        return stack.getOrDefault(mounted.get(), -1.0);
    }

    static void write(ItemStack stack, double eu) {
        stack.set(mounted.get(), eu);
    }

    static ItemStack recover(double eu) {
        var pack = NativeMount.electricJetpack();
        PortableGear.restoreEnergy(pack, eu);
        return pack;
    }
}
