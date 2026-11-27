package com.industrialage.ia2.forge.client;

import com.industrialage.ia2.network.EchoProtocol;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Local status screen showing the active client resource and the current connection's echo. */
public final class StatusScreen extends Screen {
    private static final int GAP = 8;
    private static final int BUTTON_HEIGHT = 20;

    private record Text(String title, String resource, String receipt) {}

    private final StatusResources resources;
    private final EchoProtocol echo;
    private final BooleanSupplier connected;
    private Text displayed;
    private MultiLineLabel titleLines;
    private MultiLineLabel resourceLines;
    private MultiLineLabel receiptLines;
    private int textTop;
    private Button testConnection;
    private boolean sendRejected;

    StatusScreen(StatusResources resources, EchoProtocol echo, BooleanSupplier connected) {
        super(Component.translatable("screen.ic2.status"));
        this.resources = resources;
        this.echo = echo;
        this.connected = connected;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.max(1, Math.min(150, width - 24));
        testConnection =
                addRenderableWidget(
                        new Button(
                                (width - buttonWidth) / 2,
                                0,
                                buttonWidth,
                                BUTTON_HEIGHT,
                                Component.translatable("screen.ic2.test_connection"),
                                button -> requestEcho()));
        displayed = null;
        tick();
    }

    @Override
    public void tick() {
        testConnection.active = connected.getAsBoolean();
        var text =
                new Text(
                        getTitle().getString(),
                        resourceMessage().getString(),
                        echoMessage().getString());
        if (text.equals(displayed)) {
            return;
        }
        displayed = text;
        int wrapWidth = Math.max(1, Math.min(300, width - 24));
        titleLines = MultiLineLabel.create(font, Component.literal(text.title()), wrapWidth, 2);
        resourceLines =
                MultiLineLabel.create(font, Component.literal(text.resource()), wrapWidth, 4);
        receiptLines = MultiLineLabel.create(font, Component.literal(text.receipt()), wrapWidth, 2);
        int rows =
                titleLines.getLineCount()
                        + resourceLines.getLineCount()
                        + receiptLines.getLineCount();
        int bodyHeight = rows * font.lineHeight + GAP * 3;
        textTop = Math.max(0, (height - bodyHeight - BUTTON_HEIGHT) / 2);
        testConnection.y = textTop + bodyHeight;
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        int center = width / 2;
        int y = titleLines.renderCentered(pose, center, textTop, font.lineHeight, 0xffffff) + GAP;
        y = resourceLines.renderCentered(pose, center, y, font.lineHeight, 0xffffff) + GAP;
        receiptLines.renderCentered(pose, center, y, font.lineHeight, 0xffffff);
        super.render(pose, mouseX, mouseY, partialTick);
    }

    @Override
    public Component getNarrationMessage() {
        return getTitle()
                .copy()
                .append(". ")
                .append(resourceMessage())
                .append(". ")
                .append(echoMessage());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component resourceMessage() {
        return resources
                .message()
                .<Component>map(Component::literal)
                .orElseGet(() -> Component.translatable("screen.ic2.resource_unavailable"));
    }

    private Component echoMessage() {
        if (sendRejected || !connected.getAsBoolean()) {
            return Component.translatable("screen.ic2.echo_unavailable");
        }
        var reply = echo.lastReply();
        return reply.isPresent()
                ? Component.translatable("screen.ic2.echo_reply", reply.getAsLong())
                : Component.translatable("screen.ic2.echo_none");
    }

    private void requestEcho() {
        try {
            echo.request(System.nanoTime());
            sendRejected = false;
        } catch (IllegalStateException unavailable) {
            sendRejected = true;
        }
    }
}
