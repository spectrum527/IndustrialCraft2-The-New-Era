package com.industrialage.ia2.client;

import com.industrialage.ia2.network.EchoProtocol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Displays the current client resource message and the reply to a player-initiated connection test.
 */
public final class StatusScreen extends Screen {
    private final StatusResources resources;
    private final EchoProtocol echo;
    private LinearLayout layout;
    private MultiLineTextWidget titleText;
    private MultiLineTextWidget resourceText;
    private MultiLineTextWidget echoText;
    private Button testConnection;
    private boolean sendRejected;

    StatusScreen(StatusResources resources, EchoProtocol echo) {
        super(Component.translatable("screen.ic2.status"));
        this.resources = resources;
        this.echo = echo;
    }

    @Override
    protected void init() {
        layout = LinearLayout.vertical().spacing(10);
        layout.defaultCellSetting().alignHorizontallyCenter();
        titleText = layout.addChild(text(getTitle(), 2));
        resourceText = layout.addChild(text(resourceMessage(), 4));
        echoText = layout.addChild(text(echoMessage(), 2));
        testConnection =
                layout.addChild(
                        Button.builder(
                                        Component.translatable("screen.ic2.test_connection"),
                                        button -> requestEcho())
                                .build());
        testConnection.active = connected();
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    private MultiLineTextWidget text(Component message, int rows) {
        return new MultiLineTextWidget(message, font).setMaxRows(rows).setCentered(true);
    }

    @Override
    protected void repositionElements() {
        int textWidth = Math.max(1, Math.min(300, width - 32));
        titleText.setMaxWidth(textWidth);
        resourceText.setMaxWidth(textWidth);
        echoText.setMaxWidth(textWidth);
        testConnection.setWidth(Math.min(textWidth, 150));
        layout.arrangeElements();
        layout.setPosition((width - layout.getWidth()) / 2, (height - layout.getHeight()) / 2);
    }

    /** Refreshes the visible resource message and connection reply on the client thread. */
    @Override
    public void tick() {
        testConnection.active = connected();
        var resource = resourceMessage();
        var result = echoMessage();
        if (!resource.equals(resourceText.getMessage()) || !result.equals(echoText.getMessage())) {
            resourceText.setMessage(resource);
            echoText.setMessage(result);
            repositionElements();
        }
    }

    /** {@return the screen title and status text for the narrator} */
    @Override
    public Component getNarrationMessage() {
        return getTitle()
                .copy()
                .append(". ")
                .append(resourceMessage())
                .append(". ")
                .append(echoMessage());
    }

    /** {@return false so reading status does not pause an integrated world} */
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
        if (sendRejected || !connected()) {
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

    private static boolean connected() {
        return ClientServices.connected(Minecraft.getInstance().getConnection());
    }
}
