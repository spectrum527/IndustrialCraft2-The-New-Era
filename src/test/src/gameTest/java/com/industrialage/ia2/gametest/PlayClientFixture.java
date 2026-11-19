package com.industrialage.ia2.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.industrialage.ia2.client.ClientServices;
import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.platform.NeoForgePlatform;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Operates the real 1.21.1 client from its isolated acceptance directory. */
@Mod(value = "ic2_gametest", dist = Dist.CLIENT)
public final class PlayClientFixture {
    private final Path input = Path.of("qa-input.txt");
    private final Path output = Path.of("qa-output.json");
    private String chat = "";
    private int chatCount;
    private CompletableFuture<Void> reload = CompletableFuture.completedFuture(null);

    /** Hooks the test screen and input driver for the selected physical-client profile. */
    public PlayClientFixture(IEventBus modBus) {
        if (!Boolean.getBoolean("ic2.qa.clientAcceptance")) {
            return;
        }
        ClientServices.registerMenuScreen(modBus, PlayMenuFixture::menuType, SampleScreen::new);
        NeoForge.EVENT_BUS.addListener(
                ClientChatReceivedEvent.System.class,
                event -> {
                    chat = event.getMessage().getString();
                    chatCount++;
                });
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    private void tick() {
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
            KeyMapping key = bindings.getFirst();
            act(mc, key, request.get("action").getAsString());
            result.addProperty("key", key.getKey().getName());
            result.addProperty(
                    "screen", mc.screen == null ? "" : mc.screen.getClass().getSimpleName());
            result.addProperty("title", mc.screen == null ? "" : mc.screen.getTitle().getString());
            result.addProperty(
                    "text", mc.screen == null ? "" : mc.screen.getNarrationMessage().getString());
            result.addProperty("overlay", mc.getOverlay() != null);
            result.addProperty("world", mc.level != null);
            result.addProperty(
                    "player", mc.player == null ? "" : mc.player.getGameProfile().getName());
            result.addProperty("integrated", mc.getSingleplayerServer() != null);
            result.addProperty("reloadDone", reload.isDone());
            result.addProperty("reloadFailed", reload.isCompletedExceptionally());
            result.addProperty("chat", chat);
            result.addProperty("chatCount", chatCount);
            if (mc.getSingleplayerServer() != null && mc.level != null) {
                var services =
                        ModList.get()
                                .getModContainerById("ic2")
                                .orElseThrow()
                                .getCustomExtension(NeoForgePlatform.class)
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
                result.addProperty("menuId", mc.player.containerMenu.containerId);
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

    private void act(Minecraft mc, KeyMapping key, String action) {
        switch (action) {
            case "state" -> {}
            case "connect" -> {
                String address = "127.0.0.1:" + Integer.getInteger("ic2.qa.clientPort");
                ConnectScreen.startConnecting(
                        new TitleScreen(),
                        mc,
                        ServerAddress.parseString(address),
                        new ServerData("P2 local server", address, ServerData.Type.OTHER),
                        false,
                        null);
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
            case "reload-config" -> mc.getConnection().sendCommand("ic2 config reload");
            case "values" -> mc.getConnection().sendCommand("ic2accept values");
            case "menu" -> mc.getConnection().sendCommand("ic2accept menu");
            case "pack-on", "pack-off", "pack-refresh" -> {
                var packs = mc.getResourcePackRepository();
                packs.reload();
                if (action.equals("pack-on") && !packs.addPack("file/qa-status")) {
                    throw new IllegalStateException("Could not enable test pack");
                }
                if (action.equals("pack-off") && !packs.removePack("file/qa-status")) {
                    throw new IllegalStateException("Could not disable test pack");
                }
                reload = mc.reloadResourcePacks();
            }
            case "disconnect" -> {
                mc.level.disconnect();
                mc.disconnect(new TitleScreen());
                mc.setScreen(new TitleScreen());
            }
            case "single-a", "single-b" ->
                    mc.createWorldOpenFlows()
                            .openWorld(action, () -> mc.setScreen(new TitleScreen()));
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
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffd7d7d7);
        }
    }
}
