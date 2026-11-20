package com.industrialage.ia2.gameplay;

import java.util.function.Supplier;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Native watercraft binding for Minecraft 26.x. */
final class NativeBoats {
    private NativeBoats() {}

    static EntityType<Craft> type(BoatFleet.Kind kind) {
        return EntityType.Builder.<Craft>of(
                        (type, level) -> new Craft(type, level, kind), MobCategory.MISC)
                .sized(1.375F, 0.5625F)
                .clientTrackingRange(10)
                .build(
                        ResourceKey.create(
                                Registries.ENTITY_TYPE,
                                Identifier.fromNamespaceAndPath("ic2", kind.path)));
    }

    static Item item(
            Item.Properties properties, BoatFleet.Kind kind, Supplier<EntityType<Craft>> entity) {
        return new BoatItem(entity.get(), properties);
    }

    static final class Craft extends Boat {
        private final BoatFleet.Kind kind;

        private Craft(EntityType<? extends Craft> type, Level level, BoatFleet.Kind kind) {
            super(type, level, () -> BoatFleet.wreck(kind));
            this.kind = kind;
        }

        @Override
        public InteractionResult interact(Player player, InteractionHand hand, Vec3 hit) {
            if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty() && !isVehicle()) {
                if (level() instanceof ServerLevel server) {
                    if (!player.isCreative()) spawnAtLocation(server, BoatFleet.recovery(kind));
                    discard();
                }
                return InteractionResult.SUCCESS;
            }
            return super.interact(player, hand, hit);
        }

        @Override
        public void tick() {
            super.tick();
            if (!isInWater()
                    || !getPaddleState(0)
                    || !getPaddleState(1)
                    || getControllingPassenger() == null) return;
            double extra = kind.thrust(getControllingPassenger(), !level().isClientSide());
            if (extra > 0 && isLocalInstanceAuthoritative()) {
                var velocity = Vec3.directionFromRotation(0, getYRot()).scale(extra);
                setDeltaMovement(getDeltaMovement().add(velocity));
            }
        }
    }

    static final class Client {
        private Client() {}

        static void install(IEventBus bus) {
            bus.addListener(
                    EntityRenderersEvent.RegisterLayerDefinitions.class,
                    event -> {
                        for (var kind : BoatFleet.Kind.values()) {
                            event.registerLayerDefinition(layer(kind), BoatModel::createBoatModel);
                        }
                    });
            bus.addListener(
                    EntityRenderersEvent.RegisterRenderers.class,
                    event -> {
                        for (var kind : BoatFleet.Kind.values()) {
                            event.registerEntityRenderer(
                                    BoatFleet.type(kind),
                                    context -> new BoatRenderer(context, layer(kind)));
                        }
                    });
        }

        private static ModelLayerLocation layer(BoatFleet.Kind kind) {
            return new ModelLayerLocation(
                    Identifier.fromNamespaceAndPath("ic2", "boat/" + kind.texture), "main");
        }
    }
}
