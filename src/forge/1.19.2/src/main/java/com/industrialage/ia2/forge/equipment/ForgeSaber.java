package com.industrialage.ia2.forge.equipment;

import com.industrialage.ia2.audio.SaberAudio;
import com.industrialage.ia2.content.PortableEquipment;
import java.util.UUID;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;

/** Powered saber transitions, genuine melee modifiers on Forge 1.19.2. */
final class ForgeSaber {
    private static final String STATE = "ic2_saber_active";
    private static final int SWING = 400;
    private static final int IDLE = 64;
    private static final int START_THRESHOLD = 16;
    private static final AttributeModifier ACTIVE_DAMAGE =
            new AttributeModifier(
                    UUID.fromString("d04c28f9-6592-46fc-9f05-f225d6a63a1f"),
                    "IC2 nano saber damage",
                    20,
                    AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier IDLE_DAMAGE =
            new AttributeModifier(
                    ACTIVE_DAMAGE.getId(),
                    "IC2 nano saber damage",
                    4,
                    AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier SPEED =
            new AttributeModifier(
                    UUID.fromString("04306501-90e4-4a5b-8d6a-49cf7ef990df"),
                    "IC2 nano saber speed",
                    -3,
                    AttributeModifier.Operation.ADDITION);

    private ForgeSaber() {}

    static void install(IEventBus modBus, IEventBus gameBus) {
        gameBus.addListener(
                (ItemAttributeModifierEvent event) -> {
                    var item = event.getItemStack();
                    if (event.getSlotType() == EquipmentSlot.MAINHAND
                            && ForgeGear.type(item) == PortableEquipment.NANO_SABER) {
                        event.addModifier(
                                Attributes.ATTACK_DAMAGE,
                                poweredStrike(item) ? ACTIVE_DAMAGE : IDLE_DAMAGE);
                        event.addModifier(Attributes.ATTACK_SPEED, SPEED);
                    }
                });
        gameBus.addListener(ForgeSaber::onHit);
        if (FMLEnvironment.dist == Dist.CLIENT) Client.install(modBus);
    }

    private static boolean active(ItemStack stack) {
        return ForgeGear.type(stack) == PortableEquipment.NANO_SABER
                && stack.hasTag()
                && stack.getTag().getBoolean(STATE);
    }

    private static boolean poweredStrike(ItemStack stack) {
        return active(stack) && ForgeGear.stored(stack) >= SWING;
    }

    static void use(PlayerInteractEvent.RightClickItem event) {
        var stack = event.getItemStack();
        if (!active(stack)
                && !event.getEntity().isCreative()
                && ForgeGear.stored(stack) < START_THRESHOLD) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        if (active(stack)) stack.removeTagKey(STATE);
        else {
            stack.getOrCreateTag().putBoolean(STATE, true);
            SaberAudio.play(player.getLevel(), player, "power_up");
        }
    }

    static void tick(ServerPlayer player, ItemStack stack) {
        if (!player.isCreative()
                && player.level.getGameTime() % 16 == 0
                && active(stack)
                && !ForgeGear.spend(stack, IDLE)) {
            stack.removeTagKey(STATE);
        }
    }

    private static void onHit(LivingHurtEvent event) {
        var source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)
                || source.getDirectEntity() != player
                || !source.getMsgId().equals("player")) return;
        var stack = player.getMainHandItem();
        if (!poweredStrike(stack)) return;
        if (!player.isCreative() && !ForgeGear.spend(stack, SWING)) {
            stack.removeTagKey(STATE);
            return;
        }
        var level = player.getLevel();
        SaberAudio.play(level, player, level.getRandom().nextBoolean() ? "swing1" : "swing2");
    }

    private static final class Client {
        static void install(IEventBus bus) {
            bus.addListener(
                    (FMLClientSetupEvent event) ->
                            event.enqueueWork(
                                    () ->
                                            ItemProperties.register(
                                                    ForgeRegistries.ITEMS.getValue(
                                                            new ResourceLocation(
                                                                    "ic2", "nano_saber")),
                                                    new ResourceLocation("ic2", "saber_active"),
                                                    (stack, world, entity, seed) ->
                                                            active(stack) ? 1F : 0F)));
        }
    }
}
