package com.industrialage.ia2.compatqa;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import mcjty.theoneprobe.api.IElement;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.apiimpl.ProbeInfo;
import mcjty.theoneprobe.apiimpl.elements.AbstractElementPanel;
import mcjty.theoneprobe.apiimpl.elements.ElementText;
import mcjty.theoneprobe.network.PacketGetEntityInfo;
import mcjty.theoneprobe.network.PacketGetInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Inspects the actual reply built by TOP after its normal IMC provider registration. */
final class TopReply {
    private TopReply() {}

    static void block(ServerLevel level, Player player, BlockPos pos, String key, Object... args)
            throws ReflectiveOperationException {
        var builder =
                method(
                        PacketGetInfo.class,
                        Player.class,
                        ProbeMode.class,
                        Level.class,
                        BlockPos.class,
                        Direction.class,
                        Vec3.class,
                        ItemStack.class);
        var reply =
                (ProbeInfo)
                        builder.invoke(
                                null,
                                player,
                                ProbeMode.NORMAL,
                                level,
                                pos,
                                Direction.UP,
                                Vec3.atCenterOf(pos),
                                ItemStack.EMPTY);
        check(reply, key, args);
    }

    static void entity(ServerLevel level, Player player, Entity entity, String key, Object... args)
            throws ReflectiveOperationException {
        var builder =
                method(
                        PacketGetEntityInfo.class,
                        Player.class,
                        ProbeMode.class,
                        Level.class,
                        Entity.class,
                        Vec3.class);
        var reply =
                (ProbeInfo)
                        builder.invoke(
                                null, player, ProbeMode.NORMAL, level, entity, entity.position());
        check(reply, key, args);
    }

    private static Method method(Class<?> owner, Class<?>... types)
            throws ReflectiveOperationException {
        var build = owner.getDeclaredMethod("getProbeInfo", types);
        build.setAccessible(true);
        return build;
    }

    private static void check(ProbeInfo reply, String key, Object[] args)
            throws ReflectiveOperationException {
        Field field = ElementText.class.getDeclaredField("text");
        field.setAccessible(true);
        var found = find(reply.getElements(), field, key);
        OptionalCompatFixture.require(
                found != null && Arrays.equals(actual(found.getArgs()), actual(args)),
                "TOP " + key + " expected " + Arrays.toString(args) + " actual " + found);
    }

    private static TranslatableContents find(List<IElement> elements, Field field, String key)
            throws IllegalAccessException {
        for (var element : elements) {
            if (element instanceof ElementText text
                    && ((Component) field.get(text)).getContents()
                            instanceof TranslatableContents line
                    && line.getKey().equals(key)) return line;
            if (element instanceof AbstractElementPanel panel) {
                var line = find(panel.getElements(), field, key);
                if (line != null) return line;
            }
        }
        return null;
    }

    private static Object[] actual(Object[] args) {
        return Arrays.stream(args)
                .map(value -> value instanceof Component text ? text.getString() : value)
                .toArray();
    }
}
