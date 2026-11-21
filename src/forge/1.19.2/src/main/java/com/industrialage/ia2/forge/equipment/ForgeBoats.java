package com.industrialage.ia2.forge.equipment;

import com.industrialage.ia2.forge.content.ForgeContent;
import com.mojang.datafixers.util.Pair;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.BoatModel;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The three IC2 boats available to Forge survival players. */
public final class ForgeBoats {
    private static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "ic2");
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "ic2");
    private static final RegistryObject<Item> DAMAGED =
            ITEMS.register(
                    "broken_rubber_boat",
                    () -> new Item(ForgeContent.itemProperties("broken_rubber_boat")));
    private static final Map<Kind, RegistryObject<EntityType<Craft>>> CRAFT =
            new EnumMap<>(Kind.class);
    private static final Map<Kind, RegistryObject<Item>> LAUNCHERS = new EnumMap<>(Kind.class);

    private ForgeBoats() {}

    /** Adds the three entity types, launch items, and physical-client renderers. */
    public static void install(IEventBus bus) {
        for (var kind : Kind.values()) {
            CRAFT.put(
                    kind,
                    TYPES.register(
                            kind.path,
                            () ->
                                    EntityType.Builder.<Craft>of(
                                                    (type, level) -> new Craft(type, level, kind),
                                                    MobCategory.MISC)
                                            .sized(1.375F, 0.5625F)
                                            .clientTrackingRange(10)
                                            .build("ic2:" + kind.path)));
            LAUNCHERS.put(kind, ITEMS.register(kind.path, () -> new BoatItem(kind)));
        }
        TYPES.register(bus);
        ITEMS.register(bus);
        if (FMLEnvironment.dist == Dist.CLIENT) Client.install(bus);
    }

    private enum Kind {
        RUBBER("rubber_boat", "boat_rubber"),
        CARBON("carbon_boat", "boat_carbon"),
        ELECTRIC("electric_boat", "boat_electric");

        final String path;
        final String texture;

        Kind(String path, String texture) {
            this.path = path;
            this.texture = texture;
        }
    }

    private static final class BoatItem extends Item {
        private final Kind kind;

        BoatItem(Kind kind) {
            super(ForgeContent.itemProperties(kind.path).stacksTo(1));
            this.kind = kind;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(
                Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
            if (hit.getType() != HitResult.Type.BLOCK
                    || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
                return InteractionResultHolder.pass(stack);
            }
            var boat = new Craft(CRAFT.get(kind).get(), level, kind);
            var pos = hit.getLocation();
            boat.setPos(pos.x, pos.y, pos.z);
            boat.setYRot(player.getYRot());
            if (!level.noCollision(boat, boat.getBoundingBox())) {
                return InteractionResultHolder.fail(stack);
            }
            if (level instanceof ServerLevel && level.addFreshEntity(boat)) {
                if (!player.isCreative()) stack.shrink(1);
                player.awardStat(Stats.ITEM_USED.get(this));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
    }

    private static final class Craft extends Boat {
        private final Kind kind;

        Craft(EntityType<? extends Craft> type, Level level, Kind kind) {
            super(type, level);
            this.kind = kind;
        }

        @Override
        public Item getDropItem() {
            return kind == Kind.RUBBER ? DAMAGED.get() : LAUNCHERS.get(kind).get();
        }

        @Override
        public InteractionResult interact(Player player, InteractionHand hand) {
            if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty() && !isVehicle()) {
                if (level instanceof ServerLevel) {
                    if (!player.isCreative()) spawnAtLocation(LAUNCHERS.get(kind).get());
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
                    || !(getControllingPassenger() instanceof Player rider)) return;
            double extra =
                    switch (kind) {
                        case RUBBER -> 0;
                        case CARBON -> 0.016;
                        case ELECTRIC ->
                                ForgeGear.boatPower(rider, !level.isClientSide()) ? 0.042 : 0;
                    };
            if (extra > 0 && isControlledByLocalInstance()) {
                setDeltaMovement(
                        getDeltaMovement()
                                .add(Vec3.directionFromRotation(0, getYRot()).scale(extra)));
            }
        }
    }

    private static final class Client {
        private Client() {}

        static void install(IEventBus bus) {
            bus.addListener(
                    (EntityRenderersEvent.RegisterRenderers event) -> {
                        for (var kind : Kind.values()) {
                            var texture =
                                    new ResourceLocation(
                                            "ic2", "textures/entity/boat/" + kind.texture + ".png");
                            event.registerEntityRenderer(
                                    CRAFT.get(kind).get(),
                                    context ->
                                            new BoatRenderer(context, false) {
                                                @Override
                                                public Pair<ResourceLocation, BoatModel>
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
