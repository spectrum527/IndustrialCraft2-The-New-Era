package com.industrialage.ia2.gameplay;

import com.mojang.datafixers.util.Pair;
import java.util.function.Supplier;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Native watercraft binding for Minecraft 1.21.1. */
final class NativeBoats {
    private NativeBoats() {}

    static EntityType<Craft> type(BoatFleet.Kind kind) {
        return EntityType.Builder.<Craft>of(
                        (type, level) -> new Craft(type, level, kind), MobCategory.MISC)
                .sized(1.375F, 0.5625F)
                .clientTrackingRange(10)
                .build("ic2:" + kind.path);
    }

    static Item item(
            Item.Properties properties, BoatFleet.Kind kind, Supplier<EntityType<Craft>> entity) {
        return new Item(properties) {
            @Override
            public InteractionResultHolder<ItemStack> use(
                    Level level, Player player, InteractionHand hand) {
                var stack = player.getItemInHand(hand);
                var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
                if (hit.getType() != HitResult.Type.BLOCK
                        || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
                    return InteractionResultHolder.pass(stack);
                }
                var craft = new Craft(entity.get(), level, kind);
                var pos = hit.getLocation();
                craft.setPos(pos.x, pos.y, pos.z);
                craft.setYRot(player.getYRot());
                if (!level.noCollision(craft, craft.getBoundingBox())) {
                    return InteractionResultHolder.fail(stack);
                }
                if (level instanceof ServerLevel && level.addFreshEntity(craft)) {
                    if (!player.isCreative()) stack.shrink(1);
                    player.awardStat(Stats.ITEM_USED.get(this));
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        };
    }

    static final class Craft extends Boat {
        private final BoatFleet.Kind kind;

        private Craft(EntityType<? extends Craft> type, Level level, BoatFleet.Kind kind) {
            super(type, level);
            this.kind = kind;
        }

        @Override
        public Item getDropItem() {
            return BoatFleet.wreck(kind);
        }

        @Override
        public InteractionResult interact(Player player, InteractionHand hand) {
            if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty() && !isVehicle()) {
                if (level() instanceof ServerLevel) {
                    if (!player.isCreative()) spawnAtLocation(BoatFleet.recovery(kind));
                    discard();
                }
                return InteractionResult.SUCCESS;
            }
            return super.interact(player, hand);
        }

        @Override
        public void tick() {
            super.tick();
            if (!isInWater()
                    || !getPaddleState(0)
                    || !getPaddleState(1)
                    || getControllingPassenger() == null) return;
            double extra = kind.thrust(getControllingPassenger(), !level().isClientSide());
            if (extra > 0 && isControlledByLocalInstance()) {
                var velocity = Vec3.directionFromRotation(0, getYRot()).scale(extra);
                setDeltaMovement(getDeltaMovement().add(velocity));
            }
        }
    }

    static final class Client {
        private Client() {}

        static void install(IEventBus bus) {
            bus.addListener(
                    EntityRenderersEvent.RegisterRenderers.class,
                    event -> {
                        for (var kind : BoatFleet.Kind.values()) {
                            var texture =
                                    ResourceLocation.fromNamespaceAndPath(
                                            "ic2", "textures/entity/boat/" + kind.texture + ".png");
                            event.registerEntityRenderer(
                                    BoatFleet.type(kind),
                                    context ->
                                            new BoatRenderer(context, false) {
                                                @Override
                                                public Pair<ResourceLocation, ListModel<Boat>>
                                                        getModelWithLocation(Boat boat) {
                                                    return Pair.of(
                                                            texture,
                                                            super.getModelWithLocation(boat)
                                                                    .getSecond());
                                                }
                                            });
                        }
                    });
        }
    }
}
