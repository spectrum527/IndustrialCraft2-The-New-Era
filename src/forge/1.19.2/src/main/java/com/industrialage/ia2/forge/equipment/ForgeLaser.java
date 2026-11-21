package com.industrialage.ia2.forge.equipment;

import com.industrialage.ia2.audio.IndustrialSounds;
import com.industrialage.ia2.content.MiningLaserModes;
import com.industrialage.ia2.content.MiningLaserModes.Mode;
import com.industrialage.ia2.content.PortableEquipment;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The Forge mining laser's position, charge, and native block-destruction rules. */
final class ForgeLaser {
    private static final String MODE = "ic2_laser_mode";
    private static final Map<ServerPlayer, Integer> COOLDOWN = new WeakHashMap<>();
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "ic2");
    private static final RegistryObject<EntityType<Bolt>> BOLT =
            ENTITIES.register(
                    "laser_bullet",
                    () ->
                            EntityType.Builder.<Bolt>of(Bolt::new, MobCategory.MISC)
                                    .sized(0.2F, 0.2F)
                                    .clientTrackingRange(8)
                                    .updateInterval(1)
                                    .noSave()
                                    .build("ic2:laser_bullet"));

    private ForgeLaser() {}

    static void install(IEventBus bus) {
        ENTITIES.register(bus);
        if (FMLEnvironment.dist == Dist.CLIENT) Client.install(bus);
    }

    private static int setting(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(MODE) : 0;
    }

    private static Component label(int index) {
        return Component.translatable(
                "item.ic2.mining_laser.mode",
                Component.translatable(MiningLaserModes.translationKey(index)),
                MiningLaserModes.get(index).cost());
    }

    static void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (ForgeGear.type(stack) == PortableEquipment.MINING_LASER)
            event.getToolTip().add(label(setting(stack)));
    }

    static void use(PlayerInteractEvent.RightClickItem event) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        var stack = event.getItemStack();
        int index = setting(stack);
        if (player.isShiftKeyDown()) {
            int next = MiningLaserModes.next(index);
            stack.getOrCreateTag().putInt(MODE, next);
            player.displayClientMessage(label(next), true);
            return;
        }
        Mode profile = MiningLaserModes.get(index);
        var level = player.getLevel();
        int tick = level.getServer().getTickCount();
        if (tick < COOLDOWN.getOrDefault(player, 0)) return;
        Vec3 look = player.getLookAngle();
        if (profile.levelOnly() && Math.abs(look.y) > 0.5) {
            player.displayClientMessage(
                    Component.translatable("item.ic2.mining_laser.angle"), true);
            return;
        }
        var stored = ForgeGear.stored(stack);
        if (!player.isCreative() && stored < profile.cost()) return;
        if (fire(level, player, look, profile)) {
            if (!player.isCreative()) ForgeGear.setStored(stack, stored - profile.cost());
            COOLDOWN.put(player, tick + 5);
            IndustrialSounds.play(level, player.blockPosition(), MiningLaserModes.sound(index));
        }
    }

    private static boolean fire(ServerLevel level, ServerPlayer player, Vec3 look, Mode profile) {
        var forward =
                profile.levelOnly() ? new Vec3(look.x, 0, look.z).normalize() : look.normalize();
        var perpendicular = forward.cross(new Vec3(0, 1, 0)).normalize();
        if (perpendicular.lengthSqr() < 0.5) perpendicular = new Vec3(1, 0, 0);
        var up = perpendicular.cross(forward).normalize();
        int mid = profile.size() / 2;
        boolean added = false;
        for (int x = -mid; x <= mid; x++) {
            for (int y = -mid; y <= mid; y++) {
                var start = player.getEyePosition().add(forward.scale(0.35));
                var offset = perpendicular.scale(x).add(up.scale(y));
                var direction =
                        profile.parallel() ? forward : forward.add(offset.scale(0.10)).normalize();
                if (profile.parallel()) start = start.add(offset.scale(0.86));
                var bolt = new Bolt(BOLT.get(), level);
                bolt.aim(player, start, direction, profile);
                added |= level.addFreshEntity(bolt);
            }
        }
        return added;
    }

    private static boolean cut(ServerLevel world, BlockPos pos, ServerPlayer ripper, Mode profile) {
        if (profile.blast()) {
            boolean changed = false;
            for (var block : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                changed |= cutOne(world, block, ripper, false);
            }
            return changed;
        }
        return cutOne(world, pos, ripper, profile.smelt());
    }

    private static boolean cutOne(
            ServerLevel level, BlockPos pos, ServerPlayer player, boolean smelt) {
        if (!player.mayBuild() || !level.hasChunkAt(pos) || !level.mayInteract(player, pos))
            return false;
        var state = level.getBlockState(pos);
        if (state.isAir()
                || state.getDestroySpeed(level, pos) < 0
                || MinecraftForge.EVENT_BUS.post(
                        new BlockEvent.BreakEvent(level, pos, state, player))) return false;
        if (!smelt) return level.destroyBlock(pos, true, player, Block.UPDATE_LIMIT);
        var drops =
                Block.getDrops(
                        state, level, pos, level.getBlockEntity(pos), player, ItemStack.EMPTY);
        if (!level.destroyBlock(pos, false, player, Block.UPDATE_LIMIT)) return false;
        for (var drop : drops) {
            var recipe =
                    level.getRecipeManager()
                            .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(drop), level);
            if (recipe.isEmpty()) Block.popResource(level, pos, drop);
            else {
                var output = recipe.orElseThrow().assemble(new SimpleContainer(drop));
                output.setCount(output.getCount() * drop.getCount());
                Block.popResource(level, pos, output);
            }
        }
        return true;
    }

    private static final class Bolt extends Arrow {
        private Mode profile;
        private int hits;

        Bolt(EntityType<? extends Bolt> type, Level world) {
            super(type, world);
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
        protected void onHitBlock(BlockHitResult impact) {
            if (level instanceof ServerLevel server
                    && getOwner() instanceof ServerPlayer owner
                    && profile != null
                    && hits > 0
                    && cut(server, impact.getBlockPos(), owner, profile)
                    && --hits > 0) {
                var next = impact.getLocation().add(getDeltaMovement().normalize().scale(0.06));
                setPos(next.x, next.y, next.z);
                return;
            }
            discard();
        }

        @Override
        protected void onHitEntity(EntityHitResult impact) {
            discard();
        }

        @Override
        public void tick() {
            super.tick();
            if (profile != null && tickCount >= profile.lifetime()) discard();
        }
    }

    private static final class Client {
        static void install(IEventBus bus) {
            bus.addListener(
                    (EntityRenderersEvent.RegisterRenderers event) ->
                            event.registerEntityRenderer(
                                    BOLT.get(),
                                    context ->
                                            new TippableArrowRenderer(context) {
                                                @Override
                                                public ResourceLocation getTextureLocation(
                                                        Arrow arrow) {
                                                    return ResourceLocation.fromNamespaceAndPath(
                                                            "ic2", "textures/models/beam.png");
                                                }
                                            }));
        }
    }
}
