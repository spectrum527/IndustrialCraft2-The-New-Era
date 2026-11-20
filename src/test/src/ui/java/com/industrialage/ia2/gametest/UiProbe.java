package com.industrialage.ia2.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.industrialage.ia2.equipment.MeterMenu;
import com.industrialage.ia2.gameplay.MachineMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Observes the actual screen and container geometry used by the native client. */
public final class UiProbe {
    private static final BlockPos TARGET = new BlockPos(2, 100, 0);

    private UiProbe() {}

    /** Sends a normal use-on packet to the prepared world position. */
    public static void use(Minecraft mc, String id, boolean item, boolean targetBlock) {
        if (item) {
            String held =
                    BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem()).toString();
            if (!held.equals("ic2:" + id))
                throw new IllegalStateException("Unexpected held item " + held);
        } else {
            String block =
                    BuiltInRegistries.BLOCK
                            .getKey(mc.level.getBlockState(TARGET).getBlock())
                            .toString();
            if (!block.equals("ic2:" + id))
                throw new IllegalStateException("Unexpected target block " + block);
        }
        if (!item || targetBlock) {
            mc.gameMode.useItemOn(
                    mc.player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(2, 100.5, 0.5), Direction.WEST, TARGET, false));
        } else {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
    }

    /**
     * Adds native dimensions, active slot locations and the current test target to the probe state.
     */
    public static void record(Minecraft mc, Screen screen, JsonObject data) {
        if (mc.player != null && mc.level != null) {
            var state = mc.level.getBlockState(TARGET);
            data.addProperty(
                    "uiTarget", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            data.addProperty(
                    "uiHeld",
                    BuiltInRegistries.ITEM
                            .getKey(mc.player.getMainHandItem().getItem())
                            .toString());
            data.addProperty("uiY", mc.player.getY());
            var items = new JsonArray();
            for (int i = 0; i < 36; i++) {
                var stack = mc.player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                var item = new JsonObject();
                item.addProperty("index", i);
                describeItem(item, stack);
                items.add(item);
            }
            data.add("uiInventory", items);
        }
        if (!(screen instanceof AbstractContainerScreen<?> container)) return;
        var panel = new JsonObject();
        panel.addProperty("x", container.getGuiLeft());
        panel.addProperty("y", container.getGuiTop());
        panel.addProperty("width", container.getXSize());
        panel.addProperty("height", container.getYSize());
        panel.addProperty("scale", mc.getWindow().getGuiScale());
        panel.addProperty("menuId", container.getMenu().containerId);
        if (container.getMenu() instanceof MachineMenu machine) {
            data.addProperty("redstoneMode", machine.redstoneMode());
            data.addProperty("machineEnergy", machine.energy());
            data.addProperty("machineProgress", machine.progress());
            data.addProperty("fuelFraction", machine.fuelRatio());
        } else if (container.getMenu() instanceof MeterMenu meter) {
            data.addProperty("meterMode", meter.mode());
            data.addProperty("meterSamples", meter.reading().count());
        }
        var slots = new JsonArray();
        for (var slot : container.getMenu().slots) {
            if (!slot.isActive()) continue;
            var point = new JsonObject();
            point.addProperty("x", slot.x);
            point.addProperty("y", slot.y);
            point.addProperty("width", 16);
            point.addProperty("height", 16);
            if (slot.hasItem()) {
                describeItem(point, slot.getItem());
            }
            slots.add(point);
        }
        panel.add("slots", slots);
        data.add("panel", panel);
    }

    private static void describeItem(JsonObject data, ItemStack stack) {
        data.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        data.addProperty("count", stack.getCount());
        for (var component : stack.getComponents().keySet()) {
            String id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component).toString();
            if (id.equals("ic2:crop_genome") || id.equals("ic2:eu")) {
                var value = stack.get(component);
                if (value != null) data.addProperty(id, value.toString());
            }
        }
    }

    /** Offsets a pane-relative mouse coordinate into Minecraft's scaled screen coordinates. */
    public static double[] pointer(
            Minecraft mc, Screen screen, String coordinates, long windowHandle) {
        if (!(screen instanceof AbstractContainerScreen<?> panel)) {
            throw new IllegalStateException("No native panel for relative pointer input");
        }
        var parts = coordinates.split(",", -1);
        if (parts.length != 2) throw new IllegalArgumentException("Expected x,y");
        double x = panel.getGuiLeft() + Double.parseDouble(parts[0]);
        double y = panel.getGuiTop() + Double.parseDouble(parts[1]);
        var window = mc.getWindow();
        GLFW.glfwSetCursorPos(windowHandle, x * window.getGuiScale(), y * window.getGuiScale());
        return new double[] {x, y};
    }
}
