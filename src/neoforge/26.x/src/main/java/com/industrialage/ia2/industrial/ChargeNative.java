package com.industrialage.ia2.industrial;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;

/** Local bindings for native vanilla primed entities, persistence and rendering. */
final class ChargeNative {
    private ChargeNative() {}

    static EntityType<Charge> type(boolean nuclear) {
        String name = nuclear ? "nuke" : "itnt";
        return EntityType.Builder.<Charge>of(Charge::new, MobCategory.MISC)
                .sized(0.98F, 0.98F)
                .clientTrackingRange(10)
                .build(
                        net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.ENTITY_TYPE,
                                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                                        "ic2", name)));
    }

    static Charge actor(ServerLevel level, boolean nuclear, BlockState block, float power) {
        var actor =
                new Charge((nuclear ? ChargeTypes.NUCLEAR : ChargeTypes.INDUSTRIAL).get(), level);
        actor.setBlockState(block);
        actor.power = power;
        return actor;
    }

    static void consumeIgniter(Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(Items.FIRE_CHARGE)) stack.shrink(1);
        else
            stack.hurtAndBreak(
                    1,
                    player,
                    hand == InteractionHand.MAIN_HAND
                            ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                            : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
    }

    static final class Charge extends PrimedTnt {
        float power = 5.5F;

        Charge(EntityType<? extends PrimedTnt> type, Level level) {
            super(type, level);
        }

        @Override
        public void tick() {
            if (getFuse() == 1 && level() instanceof ServerLevel server) {
                discard();
                float radius =
                        Math.min(
                                power,
                                getType() == ChargeTypes.NUCLEAR.get()
                                        ? Math.max(0, PowerSettings.gameChargeLimit())
                                        : 5.5F);
                if (radius > 0)
                    server.explode(
                            this,
                            getX(),
                            getY() + 0.06,
                            getZ(),
                            radius,
                            Level.ExplosionInteraction.TNT);
            } else super.tick();
        }

        @Override
        protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
            super.addAdditionalSaveData(tag);
            tag.putFloat("ic2_power", power);
        }

        @Override
        protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
            super.readAdditionalSaveData(tag);
            power = tag.getFloatOr("ic2_power", power);
        }
    }

    static final class Client {
        static void install(IEventBus bus) {
            bus.addListener(
                    net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers
                            .class,
                    event -> {
                        event.registerEntityRenderer(
                                ChargeTypes.INDUSTRIAL.get(),
                                net.minecraft.client.renderer.entity.TntRenderer::new);
                        event.registerEntityRenderer(
                                ChargeTypes.NUCLEAR.get(),
                                net.minecraft.client.renderer.entity.TntRenderer::new);
                    });
        }
    }
}
