package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.content.MiningLaserModes.Mode;
import java.util.function.Supplier;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Native projectile bindings for the mining laser. */
final class NativeLaser {
    private NativeLaser() {}

    static EntityType<Bolt> entityType() {
        return EntityType.Builder.<Bolt>of(Bolt::new, MobCategory.MISC)
                .sized(0.2F, 0.2F)
                .clientTrackingRange(8)
                .updateInterval(1)
                .noSave()
                .build(
                        ResourceKey.create(
                                Registries.ENTITY_TYPE,
                                Identifier.fromNamespaceAndPath("ic2", "laser_bullet")));
    }

    static final class Bolt extends Arrow {
        private Mode profile;
        private int hits;

        Bolt(EntityType<? extends Bolt> type, Level level) {
            super(type, level);
            pickup = Pickup.DISALLOWED;
            setNoGravity(true);
        }

        void aim(ServerPlayer owner, Vec3 start, Vec3 direction, Mode mode) {
            profile = mode;
            hits = mode.hits();
            setOwner(owner);
            setPos(start.x, start.y, start.z);
            shoot(direction.x, direction.y, direction.z, 3, 0);
        }

        @Override
        protected void onHitBlock(BlockHitResult hit) {
            if (level() instanceof ServerLevel server
                    && getOwner() instanceof ServerPlayer owner
                    && profile != null
                    && hits > 0
                    && NeoLaser.cut(server, hit.getBlockPos(), owner, profile)
                    && --hits > 0) {
                Vec3 next = hit.getLocation().add(getDeltaMovement().normalize().scale(0.06));
                setPos(next.x, next.y, next.z);
                return;
            }
            discard();
        }

        @Override
        protected void onHitEntity(EntityHitResult hit) {
            discard();
        }

        @Override
        public void tick() {
            super.tick();
            if (profile != null && tickCount >= profile.lifetime()) discard();
        }
    }

    static final class Client {
        private Client() {}

        static void install(IEventBus bus, Supplier<EntityType<Bolt>> type) {
            bus.addListener(
                    EntityRenderersEvent.RegisterRenderers.class,
                    event ->
                            event.registerEntityRenderer(
                                    type.get(),
                                    context ->
                                            new TippableArrowRenderer(context) {
                                                @Override
                                                protected Identifier getTextureLocation(
                                                        net.minecraft.client.renderer.entity.state
                                                                        .TippableArrowRenderState
                                                                state) {
                                                    return Identifier.fromNamespaceAndPath(
                                                            "ic2", "textures/models/beam.png");
                                                }
                                            }));
        }
    }
}
