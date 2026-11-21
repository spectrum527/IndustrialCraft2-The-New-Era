package com.industrialage.ia2.compatqa;

import com.industrialage.ia2.industrial.WorkshopMenu;
import com.industrialage.ia2.industrial.WorkshopNode;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Player-facing tools, native water storage, and the assembled coke kiln. Runs against the
 * installed JAR.
 */
final class WorkshopToolChecks {
    private static final List<String> CABLES =
            List.of(
                    "copper_cable",
                    "insulated_copper_cable",
                    "tin_cable",
                    "insulated_tin_cable",
                    "gold_cable",
                    "insulated_gold_cable",
                    "double_insulated_gold_cable",
                    "iron_cable",
                    "insulated_iron_cable",
                    "double_insulated_iron_cable",
                    "triple_insulated_iron_cable",
                    "glass_fibre_cable",
                    "detector_cable",
                    "splitter_cable");

    private WorkshopToolChecks() {}

    static void run(ServerLevel world, ServerPlayer player, List<String> report) {
        for (var slot :
                List.of(
                        EquipmentSlot.HEAD,
                        EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS,
                        EquipmentSlot.FEET)) player.setItemSlot(slot, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(false);
        waterloggedCables(world, player);
        report.add(
                "Waterlogging: all 14 cable items preserve a water source on placement, handle bucket pickup/refill, keep their arms and release the water on removal.");
        kiln(world, player);
        report.add(
                "Coke kiln: only the controller opens, incomplete shells pause, coal gives 500 mB and logs 250 mB at 1200 steps; the controller draws from the grate's one real tank.");
        player.setPos(6.5, 127, 6.5);
        world.setBlockAndUpdate(new BlockPos(6, 126, 6), Blocks.STONE.defaultBlockState());
        for (var tool :
                List.of(
                        new Tool("chainsaw", 10, 100),
                        new Tool("drill", 6, 50),
                        new Tool("diamond_drill", 7, 80),
                        new Tool("iridium_drill", 8, 800))) {
            hit(world, player, tool, tool.cost(), tool.damage(), 0, false);
            hit(world, player, tool, 2 * tool.cost(), tool.damage(), tool.cost(), false);
            hit(world, player, tool, tool.cost() - 1, 1, tool.cost() - 1, false);
            hit(world, player, tool, 0, 1, 0, false);
            hit(world, player, tool, tool.cost(), 0, tool.cost(), true);
        }
        report.add(
                "Powered melee: chainsaw 10/100 EU, drill 6/50, diamond 7/80, iridium 8/800. Attack speed, cooldown, empty, one EU short, immune targets and invulnerability frames checked.");
    }

    private record Tool(String path, int damage, int cost) {}

    private static void hit(
            ServerLevel level,
            ServerPlayer player,
            Tool tool,
            int before,
            int damage,
            int after,
            boolean immune) {
        var held = new ItemStack(CompatHost.item(tool.path()));
        ToolCheckHost.charge(held, before);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        for (int i = 0; i < 25; i++) player.doTick();
        player.setOnGround(true);
        player.setSprinting(false);
        player.setDeltaMovement(Vec3.ZERO);
        double speed =
                player.getAttributeValue(
                        net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
        require(
                Math.abs(speed - (before >= tool.cost() ? 1.6 : 4.0)) < 0.001,
                tool.path() + " melee speed " + speed);
        var target = ToolCheckHost.target(level);
        target.setPos(player.getX() + 1.0, player.getY(), player.getZ());
        target.setInvulnerable(immune);
        float health = target.getHealth();
        require(player.getAttackStrengthScale(0.5F) >= 0.999F, "attack ready for " + tool.path());
        player.attack(target);
        require(
                Math.abs(health - target.getHealth() - damage) < 0.001,
                tool.path()
                        + " charged "
                        + before
                        + " EU, expected "
                        + damage
                        + " damage but got "
                        + (health - target.getHealth()));
        require(
                ToolCheckHost.stored(held) == after,
                tool.path() + " remaining " + ToolCheckHost.stored(held) + " / expected " + after);
        if (damage > 0 && after == 0 && before == tool.cost()) {
            var other = ToolCheckHost.target(level);
            other.setPos(player.getX() + 1.0, player.getY(), player.getZ());
            float initial = other.getHealth();
            player.attack(other);
            require(
                    initial - other.getHealth() <= 1.001F,
                    "a second target took powered damage from an empty "
                            + tool.path()
                            + ": "
                            + (initial - other.getHealth()));
            other.discard();
        }
        if (before == 2 * tool.cost()) {
            float surviving = target.getHealth();
            for (int i = 0; i < 25; i++) player.doTick();
            player.setOnGround(true);
            player.attack(target);
            require(
                    target.getHealth() == surviving && ToolCheckHost.stored(held) == after,
                    "invulnerability frames keep charge");
        }
        target.discard();
    }

    private static void waterloggedCables(ServerLevel level, ServerPlayer player) {
        var pos = new BlockPos(8, 116, 8);
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.west(), CompatHost.block("copper_cable").defaultBlockState());
        player.setPos(9.5, 116, 8.5);
        for (String name : CABLES) {
            level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
            var cable = CompatHost.block(name);
            var stack = new ItemStack(cable);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var placed =
                    ((BlockItem) stack.getItem())
                            .place(
                                    new BlockPlaceContext(
                                            player,
                                            InteractionHand.MAIN_HAND,
                                            stack,
                                            hitFace(pos.below(), Direction.UP)));
            var state = level.getBlockState(pos);
            require(
                    placed.consumesAction()
                            && state.is(cable)
                            && state.getValue(BlockStateProperties.WATERLOGGED),
                    name + " placed in water");
            require(
                    level.getFluidState(pos).isSource()
                            && state.getValue(BlockStateProperties.WEST),
                    name + " connected while wet");
            require(
                    ToolCheckHost.bucket(player, level, pos).is(Items.WATER_BUCKET)
                            && level.getFluidState(pos).isEmpty(),
                    name + " native bucket removes water");
            var wet = (SimpleWaterloggedBlock) cable;
            require(
                    wet.placeLiquid(
                            level, pos, level.getBlockState(pos), Fluids.WATER.getSource(false)),
                    name + " refill");
            require(
                    level.destroyBlock(pos, false)
                            && level.getBlockState(pos).is(Blocks.WATER)
                            && level.getFluidState(pos).isSource(),
                    name + " water remains after removal");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void kiln(ServerLevel level, ServerPlayer player) {
        var center = new BlockPos(8, 112, 8);
        var front = center.north();
        var hatch = center.above();
        var grate = center.below();
        var brick = CompatHost.block("refractory_bricks").defaultBlockState();
        for (var pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            level.setBlockAndUpdate(pos, brick);
        }
        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(front, CompatHost.block("coke_kiln").defaultBlockState());
        level.setBlockAndUpdate(hatch, CompatHost.block("coke_kiln_hatch").defaultBlockState());
        level.setBlockAndUpdate(grate, CompatHost.block("coke_kiln_grate").defaultBlockState());
        var node = (WorkshopNode) level.getBlockEntity(front);
        var state = level.getBlockState(front);
        BlockEntityTicker ticker =
                ((EntityBlock) state.getBlock()).getTicker(level, state, node.getType());
        Runnable step = () -> ticker.tick(level, front, level.getBlockState(front), node);
        step.run();
        var formed = state.getBlock().getStateDefinition().getProperty("formed");
        require(
                formed != null && Boolean.TRUE.equals(level.getBlockState(front).getValue(formed)),
                "formed coke kiln");
        var menu = (WorkshopMenu) node.createMenu(19, player.getInventory(), player);
        require(menu != null, "controller owns menu");
        for (var port : List.of(hatch, grate)) {
            require(
                    ((WorkshopNode) level.getBlockEntity(port))
                                    .createMenu(20, player.getInventory(), player)
                            == null,
                    "no auxiliary menu " + port);
        }
        var missing = center.east().above();
        level.setBlockAndUpdate(missing, Blocks.AIR.defaultBlockState());
        menu.getSlot(0).set(new ItemStack(Items.COAL, 2));
        step.run();
        require(
                Boolean.FALSE.equals(level.getBlockState(front).getValue(formed))
                        && menu.getSlot(0).getItem().getCount() == 2,
                "incomplete kiln retains input");
        level.setBlockAndUpdate(missing, brick);
        for (int i = 0; i < 1199; i++) step.run();
        require(
                menu.getSlot(9).getItem().isEmpty() && menu.tank(1).amount() == 0,
                "kiln does not finish early");
        step.run();
        require(
                menu.getSlot(9).getItem().is(CompatHost.item("coke"))
                        && menu.tank(1).amount() == 500,
                "one coke and 500 mB");
        for (int i = 0; i < 1200; i++) step.run();
        require(
                menu.getSlot(9).getItem().getCount() == 2 && menu.tank(1).amount() == 1000,
                "two coke share the grate tank");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        player.setPos(grate.getX() + .5, grate.getY() - 2, grate.getZ() + .5);
        player.setShiftKeyDown(true);
        player.gameMode.useItemOn(
                player,
                level,
                player.getMainHandItem(),
                InteractionHand.MAIN_HAND,
                hitFace(grate, Direction.DOWN));
        require(
                player.getMainHandItem().is(Items.BUCKET) && menu.tank(1).amount() == 1000,
                "crouching at the grate cannot draw product");
        player.setShiftKeyDown(false);
        player.setPos(front.getX() + .5, front.getY(), front.getZ() - 1.5);
        player.gameMode.useItemOn(
                player,
                level,
                player.getMainHandItem(),
                InteractionHand.MAIN_HAND,
                hitFace(front, Direction.NORTH));
        require(
                player.getMainHandItem().is(CompatHost.item("creosote_bucket"))
                        && menu.tank(1).amount() == 0,
                "controller returns the bucket from the same tank");
        menu.getSlot(9).set(ItemStack.EMPTY);
        menu.getSlot(0).set(new ItemStack(Items.OAK_LOG));
        for (int i = 0; i < 1200; i++) step.run();
        require(
                menu.getSlot(9).getItem().is(Items.CHARCOAL) && menu.tank(1).amount() == 250,
                "log returns charcoal and 250 mB");
    }

    private static BlockHitResult hitFace(BlockPos pos, Direction face) {
        var center = Vec3.atCenterOf(pos);
        return new BlockHitResult(
                center.add(face.getStepX() * .5, face.getStepY() * .5, face.getStepZ() * .5),
                face,
                pos,
                false);
    }

    private static void require(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }
}
