package com.industrialage.ia2.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.lwjgl.glfw.GLFW;

/** Keeps the permission editor within the original native container session. */
public abstract class DeviceScreen<M extends AbstractContainerMenu>
        extends AbstractContainerScreen<M> {
    private AccessPanel access;
    private boolean accessPress;

    protected DeviceScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    @Override
    protected void init() {
        super.init();
        if (menu instanceof DeviceAccessMenu settings) {
            access =
                    new AccessPanel(
                            settings,
                            button -> {
                                if (minecraft != null && minecraft.gameMode != null) {
                                    minecraft.gameMode.handleInventoryButtonClick(
                                            menu.containerId, button);
                                }
                            });
            int room = width - leftPos - imageWidth;
            if (room < AccessPanel.ENTRY_GUTTER) {
                leftPos = Math.max(2, width - imageWidth - AccessPanel.ENTRY_GUTTER);
            }
            addWidget(access.entry());
            moveAccess();
        }
    }

    final PanelViewport fitNativePanel() {
        var natural = PanelViewport.fit(width, height, imageWidth, imageHeight);
        if (access == null
                || width - natural.left() - natural.drawSize(imageWidth)
                        >= AccessPanel.ENTRY_GUTTER) {
            return natural;
        }
        return PanelViewport.fit(width - AccessPanel.ENTRY_GUTTER, height, imageWidth, imageHeight);
    }

    protected final void moveAccess() {
        if (access != null) {
            access.move(width, height, leftPos + getXSize(), topPos, getYSize());
        }
    }

    @Override
    public final void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        boolean modal = access != null && access.isOpen();
        renderPanel(graphics, modal ? -10000 : x, modal ? -10000 : y, tick);
        if (access == null) return;
        moveAccess();
        graphics.nextStratum();
        var entry = access.entry();
        boolean entryHovered = !modal && entry.isMouseOver(x, y);
        boolean entryFocused =
                !modal
                        && getFocused() == entry
                        && minecraft != null
                        && minecraft.getLastInputType().isKeyboard();
        access.draw(PanelGraphics.at(graphics, font, 0, 0), entryHovered, entryFocused);
        for (var button : access.buttons()) {
            button.extractRenderState(graphics, x, y, tick);
        }
        if (entryHovered || entryFocused) {
            int tooltipX = entryHovered ? x : entry.getX() + entry.getWidth() / 2;
            int tooltipY = entryHovered ? y : entry.getY() + entry.getHeight();
            graphics.setTooltipForNextFrame(font, entry.getMessage(), tooltipX, tooltipY);
        }
    }

    /** Renders the existing authored content, before the side access controls. */
    protected void renderPanel(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        super.extractRenderState(graphics, x, y, tick);
    }

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (access != null) {
            moveAccess();
            var widget = access.at(event.x(), event.y());
            if (widget != null) {
                boolean wasOpen = access.isOpen();
                if (widget.mouseClicked(event, twice) || wasOpen) {
                    accessPress = true;
                    if (wasOpen && !access.isOpen()) {
                        setFocused(
                                minecraft != null && minecraft.getLastInputType().isKeyboard()
                                        ? access.entry()
                                        : null);
                    }
                    return true;
                }
            }
            if (access.isOpen()) {
                accessPress = true;
                return true;
            }
            if (getFocused() == access.entry()) {
                setFocused(null);
            }
        }
        return panelMouseClicked(event, twice);
    }

    /** Handles native pointer input on the original screen area. */
    protected boolean panelMouseClicked(MouseButtonEvent event, boolean twice) {
        return super.mouseClicked(event, twice);
    }

    @Override
    public final boolean mouseReleased(MouseButtonEvent event) {
        if (capturesPointer()) {
            accessPress = false;
            return true;
        }
        return panelMouseReleased(event);
    }

    /** Releases the pointer in the original screen area. */
    protected boolean panelMouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(event);
    }

    @Override
    public final boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return capturesPointer() || panelMouseDragged(event, dx, dy);
    }

    /** Continues a stock inventory drag while the permission dialog is closed. */
    protected boolean panelMouseDragged(MouseButtonEvent event, double dx, double dy) {
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public final boolean mouseScrolled(double x, double y, double dx, double dy) {
        return capturesPointer() || panelMouseScrolled(x, y, dx, dy);
    }

    /** Scrolls the original screen when it remains active. */
    protected boolean panelMouseScrolled(double x, double y, double dx, double dy) {
        return super.mouseScrolled(x, y, dx, dy);
    }

    private boolean capturesPointer() {
        return access != null && (access.isOpen() || accessPress);
    }

    @Override
    public final boolean keyPressed(KeyEvent event) {
        if (access != null && access.isOpen()) {
            moveAccess();
            access.key(
                    event.key(),
                    (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0,
                    button -> {
                        button.playDownSound(minecraft.getSoundManager());
                        button.onPress(event);
                    });
            if (!access.isOpen()) setFocused(access.entry());
            return true;
        }
        return super.keyPressed(event);
    }

    static Button accessWidget(
            int x, int y, int width, int height, Component text, Runnable action) {
        return Button.builder(text, button -> action.run()).bounds(x, y, width, height).build();
    }

    static void moveAccessWidget(Button button, int x, int y) {
        button.setX(x);
        button.setY(y);
    }

    static void focusAccessWidget(Button button, boolean focused) {
        button.setFocused(focused);
    }
}
