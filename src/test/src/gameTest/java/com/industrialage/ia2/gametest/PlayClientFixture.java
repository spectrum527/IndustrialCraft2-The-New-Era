package com.industrialage.ia2.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.industrialage.ia2.client.ClientServices;
import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.equipment.MeterMenu;
import com.industrialage.ia2.gameplay.MachineMenu;
import com.industrialage.ia2.platform.NeoForgePlatform;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.Boat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Operates the real 1.21.1 client from its isolated acceptance directory. */
@Mod(value = "ic2_gametest", dist = Dist.CLIENT)
public final class PlayClientFixture {
    private final Path input = Path.of("qa-input.txt");
    private final Path output = Path.of("qa-output.json");
    private String chat = "";
    private int chatCount;
    private String boatKind = "";
    private final java.util.concurrent.ConcurrentHashMap<String, SoundInstance> played =
            new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Integer> starts =
            new java.util.concurrent.ConcurrentHashMap<>();
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
        NeoForge.EVENT_BUS.addListener(
                PlaySoundSourceEvent.class,
                event -> {
                    var sound = event.getSound();
                    if (sound.getLocation().getNamespace().equals("ic2")) {
                        played.put(event.getName(), sound);
                        starts.merge(event.getName(), 1, Integer::sum);
                    }
                });
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
            String loop = "machine.macerator.operate";
            result.addProperty("maceratorStarts", starts.getOrDefault(loop, 0));
            var looping = played.get(loop);
            result.addProperty(
                    "maceratorPlaying", looping != null && mc.getSoundManager().isActive(looping));
            result.addProperty("laserStarts", starts.getOrDefault("item.laser.shoot", 0));
            if (mc.level != null && mc.getOverlay() == null) {
                result.addProperty("iconsBaked", sprite(mc, "ic2:items/battery/re_battery_0"));
                result.addProperty(
                        "machineFacesBaked",
                        sprite(mc, "ic2:blocks/machine/processing/basic/macerator_front")
                                && sprite(
                                        mc,
                                        "ic2:blocks/machine/processing/basic/macerator_front_active"));
                result.addProperty("vanillaStoneBaked", sprite(mc, "minecraft:block/stone"));
            }
            if (mc.player != null && mc.level != null && !boatKind.isEmpty()) {
                result.addProperty("boatNearby", nearbyBoat(mc) != null);
                result.addProperty(
                        "heldItem",
                        BuiltInRegistries.ITEM
                                .getKey(mc.player.getMainHandItem().getItem())
                                .toString());
                if (mc.player.getVehicle() instanceof Boat boat
                        && mc.getEntityRenderDispatcher().getRenderer(boat)
                                instanceof BoatRenderer renderer) {
                    var texture = renderer.getModelWithLocation(boat).getFirst();
                    result.addProperty("boatTexture", texture.toString());
                    result.addProperty(
                            "boatTextureLoaded",
                            mc.getResourceManager().getResource(texture).isPresent());
                }
            }
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
                result.addProperty("height", mc.player.getY());
                result.addProperty("grounded", mc.player.onGround());
                var vehicle = mc.player.getVehicle();
                result.addProperty(
                        "vehicle",
                        vehicle == null
                                ? ""
                                : BuiltInRegistries.ENTITY_TYPE
                                        .getKey(vehicle.getType())
                                        .toString());
                result.addProperty(
                        "bootEnergy",
                        mc.player
                                .getItemBySlot(EquipmentSlot.FEET)
                                .getOrDefault(PlayMenuFixture.energyComponent(), 0.0));
                result.addProperty(
                        "jetpackEnergy",
                        mc.player
                                .getItemBySlot(EquipmentSlot.CHEST)
                                .getOrDefault(PlayMenuFixture.energyComponent(), 0.0));
                if (mc.player.containerMenu instanceof MachineMenu machine) {
                    result.addProperty("machineCapacity", machine.capacity());
                    result.addProperty("machineDuration", machine.duration());
                }
                if (mc.player.containerMenu instanceof MeterMenu meter) {
                    result.addProperty("meterMode", meter.mode());
                    result.addProperty("meterAmount", meter.reading().max());
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
            case "macerator" -> mc.getConnection().sendCommand("ic2accept macerator");
            case "macerator-stop" -> mc.getConnection().sendCommand("ic2accept stop-macerator");
            case "laser-shot" -> mc.getConnection().sendCommand("ic2accept laser");
            case "meter-open" -> mc.getConnection().sendCommand("ic2accept meter");
            case "meter-next" ->
                    mc.screen.children().stream()
                            .filter(Button.class::isInstance)
                            .map(Button.class::cast)
                            .findFirst()
                            .orElseThrow()
                            .onPress();
            case "meter-shot" -> shot(mc, "ic2-meter.png");
            case "quantum-ready" -> mc.getConnection().sendCommand("ic2accept quantum");
            case "quantum-boost" -> {
                var boost =
                        Arrays.stream(mc.options.keyMappings)
                                .filter(
                                        binding ->
                                                binding.getName().equals("key.ic2.quantum_boost"))
                                .findFirst()
                                .orElseThrow();
                mc.options.setKey(boost, InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F7));
                KeyMapping.resetMapping();
                mc.keyboardHandler.keyPress(
                        mc.getWindow().getWindow(), GLFW.GLFW_KEY_F7, 0, GLFW.GLFW_PRESS, 0);
            }
            case "quantum-release" ->
                    mc.keyboardHandler.keyPress(
                            mc.getWindow().getWindow(), GLFW.GLFW_KEY_F7, 0, GLFW.GLFW_RELEASE, 0);
            case "boat-rubber_boat", "boat-carbon_boat", "boat-electric_boat" -> {
                boatKind = action.substring(5);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.getConnection().sendCommand("ic2accept boat " + boatKind);
            }
            case "boat-launch" -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            case "boat-board" -> {
                var boat = nearbyBoat(mc);
                if (boat == null) throw new IllegalStateException("No matching boat in the pool");
                mc.gameMode.interact(mc.player, boat, InteractionHand.MAIN_HAND);
            }
            case "rider-camera" -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.player.setXRot(24);
            }
            case "first-person-camera" -> mc.options.setCameraType(CameraType.FIRST_PERSON);
            case "shot-rubber_boat", "shot-carbon_boat", "shot-electric_boat" ->
                    shot(mc, "ic2-" + action.substring(5) + ".png");
            case "hide-chat" -> mc.gui.getChat().clearMessages(false);
            case "machine-screenshot" -> shot(mc, "ic2-machine.png");
            case "flight-ready" -> mc.getConnection().sendCommand("ic2accept flight");
            case "flight-start" -> mc.options.keyJump.setDown(true);
            case "flight-stop" -> mc.options.keyJump.setDown(false);
            case "flight-hover" -> {
                var hover =
                        Arrays.stream(mc.options.keyMappings)
                                .filter(
                                        binding ->
                                                binding.getName().equals("key.ic2.jetpack_hover"))
                                .findFirst()
                                .orElseThrow();
                mc.options.setKey(hover, InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F8));
                KeyMapping.resetMapping();
                press(mc, GLFW.GLFW_KEY_F8);
            }
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

    private Boat nearbyBoat(Minecraft mc) {
        String entityId = "ic2:" + boatKind;
        var boats =
                mc.level.getEntitiesOfClass(
                        Boat.class,
                        mc.player.getBoundingBox().inflate(6),
                        boat ->
                                BuiltInRegistries.ENTITY_TYPE
                                        .getKey(boat.getType())
                                        .toString()
                                        .equals(entityId));
        return boats.stream().findFirst().orElse(null);
    }

    private static void press(Minecraft mc, int key) {
        long window = mc.getWindow().getWindow();
        mc.keyboardHandler.keyPress(window, key, 0, GLFW.GLFW_PRESS, 0);
        mc.keyboardHandler.keyPress(window, key, 0, GLFW.GLFW_RELEASE, 0);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
    }

    private static boolean sprite(Minecraft mc, String id) {
        var key = net.minecraft.resources.ResourceLocation.parse(id);
        return mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(key)
                .contents()
                .name()
                .equals(key);
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
