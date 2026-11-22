package com.industrialage.ia2.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.lwjgl.glfw.GLFW;

/** Keeps the permission editor within the original native container session. */
public abstract class DeviceScreen<M extends AbstractContainerMenu>
        extends AbstractContainerScreen<M> {
    private AccessPanel access;
    private boolean accessPress;

    protected DeviceScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
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

    private void moveAccess() {
        access.move(width, height, leftPos + getXSize(), topPos, getYSize());
    }

    @Override
    public final void render(PoseStack graphics, int x, int y, float tick) {
        boolean modal = access != null && access.isOpen();
        renderPanel(graphics, modal ? -10000 : x, modal ? -10000 : y, tick);
        if (access == null) return;
        moveAccess();
        var entry = access.entry();
        boolean entryHovered = !modal && entry.isMouseOver(x, y);
        boolean entryFocused = !modal && getFocused() == entry;
        access.draw(PanelGraphics.at(graphics, font, 0, 0), entryHovered, entryFocused);
        for (var button : access.buttons()) {
            button.render(graphics, x, y, tick);
        }
        if (entryHovered || entryFocused) {
            int tooltipX = entryHovered ? x : entry.x + entry.getWidth() / 2;
            int tooltipY = entryHovered ? y : entry.y + entry.getHeight();
            renderTooltip(graphics, entry.getMessage(), tooltipX, tooltipY);
        }
    }

    /** Renders the existing authored content, before the side access controls. */
    protected void renderPanel(PoseStack graphics, int x, int y, float tick) {
        super.render(graphics, x, y, tick);
    }

    @Override
    public final boolean mouseClicked(double x, double y, int button) {
        if (access != null) {
            moveAccess();
            var widget = access.at(x, y);
            if (widget != null) {
                boolean wasOpen = access.isOpen();
                if (widget.mouseClicked(x, y, button) || wasOpen) {
                    accessPress = true;
                    if (wasOpen && !access.isOpen()) setFocused(access.entry());
                    return true;
                }
            }
            if (access.isOpen()) {
                accessPress = true;
                return true;
            }
        }
        return panelMouseClicked(x, y, button);
    }

    /** Handles native pointer input on the original screen area. */
    protected boolean panelMouseClicked(double x, double y, int button) {
        return super.mouseClicked(x, y, button);
    }

    @Override
    public final boolean mouseReleased(double x, double y, int button) {
        if (capturesPointer()) {
            accessPress = false;
            return true;
        }
        return panelMouseReleased(x, y, button);
    }

    /** Releases the pointer in the original screen area. */
    protected boolean panelMouseReleased(double x, double y, int button) {
        return super.mouseReleased(x, y, button);
    }

    @Override
    public final boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return capturesPointer() || panelMouseDragged(x, y, button, dx, dy);
    }

    /** Continues a stock inventory drag while the permission dialog is closed. */
    protected boolean panelMouseDragged(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public final boolean mouseScrolled(double x, double y, double delta) {
        return capturesPointer() || panelMouseScrolled(x, y, delta);
    }

    /** Scrolls the original screen when it remains active. */
    protected boolean panelMouseScrolled(double x, double y, double delta) {
        return super.mouseScrolled(x, y, delta);
    }

    private boolean capturesPointer() {
        return access != null && (access.isOpen() || accessPress);
    }

    @Override
    public final boolean keyPressed(int key, int scan, int modifiers) {
        if (access != null && access.isOpen()) {
            moveAccess();
            access.key(
                    key,
                    (modifiers & GLFW.GLFW_MOD_SHIFT) != 0,
                    button -> {
                        button.playDownSound(minecraft.getSoundManager());
                        button.onPress();
                    });
            if (!access.isOpen()) setFocused(access.entry());
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    static Button accessWidget(
            int x, int y, int width, int height, Component text, Runnable action) {
        return new AccessButton(x, y, width, height, text, action);
    }

    static void moveAccessWidget(Button button, int x, int y) {
        button.x = x;
        button.y = y;
    }

    static void focusAccessWidget(Button button, boolean focused) {
        ((AccessButton) button).setAccessFocused(focused);
    }

    private static final class AccessButton extends Button {
        private AccessButton(int x, int y, int width, int height, Component text, Runnable action) {
            super(x, y, width, height, text, button -> action.run());
        }

        private void setAccessFocused(boolean focused) {
            setFocused(focused);
        }
    }
}
