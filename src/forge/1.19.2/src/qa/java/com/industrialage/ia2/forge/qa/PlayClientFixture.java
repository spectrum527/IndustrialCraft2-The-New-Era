package com.industrialage.ia2.forge.qa;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.equipment.MeterMenu;
import com.industrialage.ia2.forge.ForgePlatform;
import com.industrialage.ia2.forge.client.ForgeClient;
import com.industrialage.ia2.forge.content.MachineMenu;
import com.industrialage.ia2.gui.AuthoredScreen;
import com.industrialage.ia2.industrial.WorkshopMenu;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

/** Operates the real 1.19.2 client from its isolated acceptance directory. */
final class PlayClientFixture {
    private static final BlockPos MACHINE_POS = new BlockPos(25, 90, 25);
    private static final BlockPos WORKSHOP_POS = new BlockPos(29, 90, 25);
    private static final BlockPos AUTHORED_POS = WORKSHOP_POS;
    private static final BlockPos STORAGE_POS = new BlockPos(29, 90, 25);
    private boolean storageMining;
    private final Path input = Path.of("qa-input.txt");
    private final Path output = Path.of("qa-output.json");
    private String chat = "";
    private int chatCount;
    private CompletableFuture<Void> reload = CompletableFuture.completedFuture(null);

    /** Hooks the test screen and input driver for the selected physical-client profile. */
    PlayClientFixture(IEventBus modBus, PlayMenuFixture menus) {
        if (!Boolean.getBoolean("ic2.qa.clientAcceptance")) {
            return;
        }
        ForgeClient.registerMenuScreen(modBus, menus::menuType, SampleScreen::new);
        MinecraftForge.EVENT_BUS.addListener(
                net.minecraftforge.eventbus.api.EventPriority.NORMAL,
                false,
                ClientChatReceivedEvent.System.class,
                event -> {
                    chat = event.getMessage().getString();
                    chatCount++;
                });
        MinecraftForge.EVENT_BUS.addListener(
                (TickEvent.ClientTickEvent event) -> {
                    if (event.phase == TickEvent.Phase.END) tick();
                });
    }

    private void tick() {
        var minecraft = Minecraft.getInstance();
        if (storageMining && minecraft.player != null && minecraft.level != null) {
            if (minecraft.level.getBlockState(STORAGE_POS).isAir()) storageMining = false;
            else minecraft.gameMode.continueDestroyBlock(STORAGE_POS, Direction.NORTH);
        }
        if (!Files.isRegularFile(input)) {
            return;
        }
        var result = new JsonObject();
        try {
            var request = JsonParser.parseString(Files.readString(input)).getAsJsonObject();
            Files.delete(input);
            result.addProperty("seq", request.get("seq").getAsInt());
            var mc = Minecraft.getInstance();
            var bindings =
                    Arrays.stream(mc.options.keyMappings)
                            .filter(key -> key.getName().equals("key.ic2.open_status"))
                            .toList();
            result.addProperty("bindings", bindings.size());
            KeyMapping key = bindings.get(0);
            act(mc, key, request.get("action").getAsString());
            result.addProperty("key", key.getKey().getName());
            result.addProperty(
                    "screen", mc.screen == null ? "" : mc.screen.getClass().getSimpleName());
            result.addProperty("title", mc.screen == null ? "" : mc.screen.getTitle().getString());
            result.addProperty(
                    "text", mc.screen == null ? "" : mc.screen.getNarrationMessage().getString());
            result.addProperty("overlay", mc.getOverlay() != null);
            result.addProperty("world", mc.level != null);
            readAuthoredScreen(mc, result);

            result.addProperty("pid", ProcessHandle.current().pid());
            result.addProperty(
                    "player", mc.player == null ? "" : mc.player.getGameProfile().getName());
            result.addProperty("integrated", mc.getSingleplayerServer() != null);
            result.addProperty("reloadDone", reload.isDone());
            result.addProperty("reloadFailed", reload.isCompletedExceptionally());
            result.addProperty("chat", chat);
            result.addProperty("chatCount", chatCount);
            result.addProperty("storageMining", storageMining);
            if (mc.player != null && mc.level != null) {
                result.addProperty(
                        "storageBlock",
                        net.minecraftforge.registries.ForgeRegistries.BLOCKS
                                .getKey(mc.level.getBlockState(STORAGE_POS).getBlock())
                                .toString());
                var hotbar = new com.google.gson.JsonArray();
                for (int i = 0; i < 9; i++)
                    hotbar.add(itemEvidence(mc.player.getInventory().getItem(i)));
                result.add("storageHotbar", hotbar);
            }
            if (mc.getSingleplayerServer() != null && mc.level != null) {
                var services =
                        ModList.get()
                                .getModContainerById("ic2")
                                .orElseThrow()
                                .getCustomExtension(ForgePlatform.class)
                                .orElseThrow()
                                .services();
                services.config()
                        .current()
                        .ifPresent(
                                snapshot -> {
                                    result.addProperty(
                                            "integratedRatio",
                                            snapshot.get(
                                                    ActiveConfig.scalarKey(
                                                            "balance/energyRetainedInStorageBlockDrops",
                                                            Double.class)));
                                    result.addProperty(
                                            "integratedHeat",
                                            snapshot.get(
                                                    ActiveConfig.scalarKey(
                                                            "balance/fermenter/hU_per_run",
                                                            Integer.class)));
                                });
            }
            if (mc.player != null) {
                result.addProperty("height", mc.player.getY());
                result.addProperty("menuId", mc.player.containerMenu.containerId);
                if (mc.level != null) {
                    var block = mc.level.getBlockState(MACHINE_POS).getBlock();
                    result.addProperty(
                            "nativeMachine",
                            net.minecraftforge.registries.ForgeRegistries.BLOCKS
                                    .getKey(block)
                                    .toString());
                }
                if (mc.level != null) {
                    var block = mc.level.getBlockState(WORKSHOP_POS).getBlock();
                    result.addProperty(
                            "nativeWorkshop",
                            net.minecraftforge.registries.ForgeRegistries.BLOCKS
                                    .getKey(block)
                                    .toString());
                }
                if (mc.player.containerMenu instanceof WorkshopMenu workshop) {
                    result.addProperty("workshopEnergy", workshop.energy());
                    result.addProperty("workshopCapacity", workshop.capacity());
                    result.addProperty("workshopProgress", workshop.progress());
                    result.addProperty("workshopDuration", workshop.duration());
                    result.addProperty("workshopRecipe", workshop.job().getString());
                    result.addProperty("workshopMode", workshop.controlLabel().getString());
                    result.addProperty("workshopSlot", workshop.slotLabel(18).getString());
                    result.addProperty("workshopFluid", workshop.tank(0).amount());
                    result.addProperty("workshopFluidName", workshop.tank(0).liquid().getString());
                }
                if (mc.player.containerMenu instanceof MachineMenu machine) {
                    result.addProperty("machineEnergy", machine.energy());
                    result.addProperty("machineCapacity", machine.capacity());
                    result.addProperty("machineRedstone", machine.redstoneMode());
                }
            }
        } catch (Exception failure) {
            result.addProperty("error", failure.toString());
        }
        try {
            Path temp = output.resolveSibling("qa-output.tmp");
            Files.writeString(temp, result.toString());
            Files.move(
                    temp,
                    output,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private static void readAuthoredScreen(Minecraft minecraft, JsonObject result) {
        if (minecraft.level != null) {
            result.addProperty(
                    "uiTarget",
                    net.minecraftforge.registries.ForgeRegistries.BLOCKS
                            .getKey(minecraft.level.getBlockState(AUTHORED_POS).getBlock())
                            .toString());
        }
        if (minecraft.player != null) {
            var held = minecraft.player.getMainHandItem();
            result.addProperty("uiHeld", itemId(held));
            var inventory = minecraft.player.getInventory();
            var stored = new JsonArray();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                var stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    var record = itemEvidence(stack);
                    record.addProperty("index", i);
                    stored.add(record);
                }
            }
            result.add("uiInventory", stored);
            if (held.hasTag()) {
                result.addProperty("uiHeldEu", held.getTag().getDouble("ic2_energy"));
            }
        }
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen)) return;
        var window = minecraft.getWindow();
        var panel = new JsonObject();
        panel.addProperty("left", screen.getGuiLeft());
        panel.addProperty("top", screen.getGuiTop());
        panel.addProperty("width", screen.getXSize());
        panel.addProperty("height", screen.getYSize());
        panel.addProperty("guiScale", window.getGuiScale());
        panel.addProperty("framebufferWidth", window.getWidth());
        panel.addProperty("framebufferHeight", window.getHeight());
        if (screen instanceof AuthoredScreen<?> authored) {
            panel.addProperty("panelScale", authored.panelScale());
            panel.addProperty("viewportWidth", window.getGuiScaledWidth());
            panel.addProperty("viewportHeight", window.getGuiScaledHeight());
        }
        if (screen.getMenu() instanceof WorkshopMenu workshop) {
            panel.addProperty("device", workshop.device());
            panel.addProperty("atlas", workshop.atlas());
        }
        if (screen.getMenu() instanceof MeterMenu meter) {
            result.addProperty("meterMode", meter.mode());
        } else if (screen.getMenu() instanceof MachineMenu machine) {
            result.addProperty("redstoneMode", machine.redstoneMode());
        }
        var slots = new JsonArray();
        for (var slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;
            var item = itemEvidence(slot.getItem());
            item.addProperty("index", slot.index);
            item.addProperty("x", slot.x);
            item.addProperty("y", slot.y);
            slots.add(item);
        }
        panel.add("slots", slots);
        panel.add("carried", itemEvidence(screen.getMenu().getCarried()));
        result.add("uiPanel", panel);
    }

    private static String itemId(ItemStack stack) {
        return net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getKey(stack.getItem())
                .toString();
    }

    private static JsonObject itemEvidence(ItemStack stack) {
        var evidence = new JsonObject();
        evidence.addProperty("item", itemId(stack));
        evidence.addProperty("count", stack.getCount());
        if (stack.hasTag()) evidence.addProperty("tag", stack.getTag().toString());
        return evidence;
    }

    private void act(Minecraft mc, KeyMapping key, String action) {
        if (action.startsWith("ui:")) {
            mc.gui.getChat().clearMessages(false);
            mc.gameMode.useItemOn(
                    mc.player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(
                            Vec3.atCenterOf(AUTHORED_POS).add(0, 0, -0.5),
                            Direction.NORTH,
                            AUTHORED_POS,
                            false));
            return;
        }
        if (action.startsWith("ui-item:")) {
            mc.gui.getChat().clearMessages(false);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            return;
        }
        if (action.startsWith("snapshot-ui:")) {
            String name = action.substring("snapshot-ui:".length());
            if (!name.matches("[a-z0-9_]+"))
                throw new IllegalArgumentException("Invalid screen name");
            Screenshot.grab(
                    mc.gameDirectory,
                    "authored-" + name + ".png",
                    mc.getMainRenderTarget(),
                    message -> {});
            return;
        }
        if (action.startsWith("move-ui:") || action.startsWith("click-ui:")) {
            if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return;
            var offsets = action.substring(action.indexOf(':') + 1).split(",");
            float scale = screen instanceof AuthoredScreen<?> authored ? authored.panelScale() : 1;
            double x = screen.getGuiLeft() + Integer.parseInt(offsets[0]) * scale;
            double y = screen.getGuiTop() + Integer.parseInt(offsets[1]) * scale;
            if (action.startsWith("click-ui:")) {
                mc.screen.mouseClicked(x, y, 0);
                mc.screen.mouseReleased(x, y, 0);
            } else {
                mc.getToasts().clear();
                var window = mc.getWindow();
                long handle = window.getWindow();
                var cursor = GLFW.glfwSetCursorPosCallback(handle, null);
                if (cursor == null) throw new IllegalStateException("Game cursor callback missing");
                GLFW.glfwSetCursorPosCallback(handle, cursor);
                // Deliver the same movement event even when the test window is in the background.
                cursor.invoke(
                        handle,
                        x * window.getScreenWidth() / window.getGuiScaledWidth(),
                        y * window.getScreenHeight() / window.getGuiScaledHeight());
            }
            return;
        }

        if (action.startsWith("storage-select:")) {
            String id = action.substring("storage-select:".length());
            for (int i = 0; i < 9; i++) {
                if (itemId(mc.player.getInventory().getItem(i)).equals(id)) {
                    mc.player.getInventory().selected = i;
                    return;
                }
            }
            throw new IllegalStateException("Storage item absent from hotbar: " + id);
        }
        switch (action) {
            case "storage-mine" -> {
                storageMining = true;
                mc.gameMode.startDestroyBlock(STORAGE_POS, Direction.NORTH);
            }
            case "storage-wrench" ->
                    mc.gameMode.useItemOn(
                            mc.player,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(
                                    Vec3.atCenterOf(STORAGE_POS).add(0, 0, -.5),
                                    Direction.NORTH,
                                    STORAGE_POS,
                                    false));
            case "storage-place" ->
                    mc.gameMode.useItemOn(
                            mc.player,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(
                                    Vec3.atBottomCenterOf(STORAGE_POS),
                                    Direction.UP,
                                    STORAGE_POS.below(),
                                    false));
            case "state" -> {}
            case "connect" -> {
                String address = "127.0.0.1:" + Integer.getInteger("ic2.qa.clientPort");
                ConnectScreen.startConnecting(
                        new TitleScreen(),
                        mc,
                        ServerAddress.parseString(address),
                        new ServerData("P2 local server", address, false));
            }
            case "bind" -> {
                mc.options.setKey(key, InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F9));
                KeyMapping.resetMapping();
            }
            case "press" -> press(mc, GLFW.GLFW_KEY_F9);
            case "esc" -> press(mc, GLFW.GLFW_KEY_ESCAPE);
            case "chat" -> mc.setScreen(new ChatScreen(""));
            case "ping" ->
                    mc.screen.children().stream()
                            .filter(Button.class::isInstance)
                            .map(Button.class::cast)
                            .filter(
                                    b ->
                                            b.getMessage()
                                                    .getString()
                                                    .equals(
                                                            Component.translatable(
                                                                            "screen.ic2.test_connection")
                                                                    .getString()))
                            .findFirst()
                            .orElseThrow()
                            .onPress();
            case "reload-config" -> mc.player.commandUnsigned("ic2 config reload");
            case "values" -> mc.player.commandUnsigned("ic2accept values");
            case "menu" -> mc.player.commandUnsigned("ic2accept menu");
            case "use-native-machine" ->
                    mc.gameMode.useItemOn(
                            mc.player,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(
                                    Vec3.atCenterOf(MACHINE_POS).add(0, 0, -0.5),
                                    Direction.NORTH,
                                    MACHINE_POS,
                                    false));
            case "use-native-workshop" -> {
                mc.gui.getChat().clearMessages(false);
                mc.gameMode.useItemOn(
                        mc.player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(
                                Vec3.atCenterOf(WORKSHOP_POS).add(0, 0, -0.5),
                                Direction.NORTH,
                                WORKSHOP_POS,
                                false));
            }
            case "snapshot-native-workshop" ->
                    Screenshot.grab(
                            mc.gameDirectory,
                            "native-metal-former-upgrade.png",
                            mc.getMainRenderTarget(),
                            message -> {});
            case "snapshot-native-washer" ->
                    Screenshot.grab(
                            mc.gameDirectory,
                            "native-ore-washer.png",
                            mc.getMainRenderTarget(),
                            message -> {});
            case "snapshot-native-machine" ->
                    Screenshot.grab(
                            mc.gameDirectory,
                            "native-batbox.png",
                            mc.getMainRenderTarget(),
                            message -> {});
            case "cycle-native-redstone" ->
                    mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, 0);
            case "pack-on", "pack-off", "pack-refresh" -> {
                var packs = mc.getResourcePackRepository();
                packs.reload();
                var selected = new java.util.ArrayList<>(packs.getSelectedIds());
                if (action.equals("pack-on")) selected.add("file/qa-status");
                if (action.equals("pack-off")) selected.remove("file/qa-status");
                packs.setSelected(selected);
                reload = mc.reloadResourcePacks();
            }
            case "disconnect" -> {
                mc.level.disconnect();
                mc.clearLevel(new TitleScreen());
                mc.setScreen(new TitleScreen());
            }
            case "single-a", "single-b" ->
                    mc.createWorldOpenFlows().loadLevel(new TitleScreen(), action);
            case "accept-experimental-world" -> {
                if (!(mc.screen instanceof ConfirmScreen confirm)) {
                    throw new IllegalStateException(
                            "Expected the isolated world's experimental settings confirmation");
                }
                confirm.children().stream()
                        .filter(Button.class::isInstance)
                        .map(Button.class::cast)
                        .filter(button -> button.active)
                        .findFirst()
                        .orElseThrow()
                        .onPress();
            }
            case "quit" -> mc.stop();
            default -> throw new IllegalArgumentException("Unknown acceptance action " + action);
        }
    }

    private static void press(Minecraft mc, int key) {
        long window = mc.getWindow().getWindow();
        mc.keyboardHandler.keyPress(window, key, 0, GLFW.GLFW_PRESS, 0);
        mc.keyboardHandler.keyPress(window, key, 0, GLFW.GLFW_RELEASE, 0);
    }

    private static final class SampleScreen
            extends AbstractContainerScreen<PlayMenuFixture.SampleMenu> {
        SampleScreen(PlayMenuFixture.SampleMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        protected void renderBg(PoseStack graphics, float partialTick, int mouseX, int mouseY) {
            fill(graphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffd7d7d7);
        }
    }
}
