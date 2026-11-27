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
import net.minecraftforge.eventbus.api.IEventBus;

/** Local bindings for native vanilla primed entities, persistence and rendering. */
final class ChargeNative {
    private ChargeNative() {}

    static EntityType<Charge> type(boolean nuclear) {
        String name = nuclear ? "nuke" : "itnt";
        return EntityType.Builder.<Charge>of(Charge::new, MobCategory.MISC)
                .sized(0.98F, 0.98F)
                .clientTrackingRange(10)
                .build("ic2:" + name);
    }

    static Charge actor(ServerLevel level, boolean nuclear, BlockState block, float power) {
        var actor =
                new Charge((nuclear ? ChargeTypes.NUCLEAR : ChargeTypes.INDUSTRIAL).get(), level);

        actor.power = power;
        return actor;
    }

    static void consumeIgniter(Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(Items.FIRE_CHARGE)) stack.shrink(1);
        else stack.hurtAndBreak(1, player, owner -> owner.broadcastBreakEvent(hand));
    }

    static final class Charge extends PrimedTnt {
        float power = 5.5F;

        Charge(EntityType<? extends PrimedTnt> type, Level level) {
            super(type, level);
        }

        @Override
        public void tick() {
            if (getFuse() == 1 && level instanceof ServerLevel server) {
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
                            net.minecraft.world.level.Explosion.BlockInteraction.DESTROY);
            } else super.tick();
        }

        @Override
        protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            tag.putFloat("ic2_power", power);
        }

        @Override
        protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            if (tag.contains("ic2_power")) power = tag.getFloat("ic2_power");
        }
    }

    static final class Client {
        static void install(IEventBus bus) {
            bus.addListener(
                    (net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers
                                    event) -> {
                        event.registerEntityRenderer(
                                ChargeTypes.INDUSTRIAL.get(), ChargeRenderer::new);
                        event.registerEntityRenderer(
                                ChargeTypes.NUCLEAR.get(), ChargeRenderer::new);
                    });
        }
    }
}
