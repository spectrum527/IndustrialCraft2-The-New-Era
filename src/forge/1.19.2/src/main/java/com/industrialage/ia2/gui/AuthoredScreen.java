package com.industrialage.ia2.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native draggable inventory with IC2's authored background and synchronized menu data. */
public abstract class AuthoredScreen<M extends AbstractContainerMenu> extends DeviceScreen<M> {
    private PanelViewport viewport = new PanelViewport(0, 0, 1);

    protected AuthoredScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title);
        imageWidth = width;
        imageHeight = height;
    }

    @Override
    protected void init() {
        super.init();
        viewport = fitNativePanel();
        leftPos = viewport.left();
        topPos = viewport.top();
        moveAccess();
    }

    /** Converts a logical screen X coordinate to panel-local authored pixels. */
    protected final int panelLocalX(int screenX) {
        return (int) Math.floor(viewport.unscaleX(screenX) - leftPos);
    }

    /** Converts a logical screen Y coordinate to panel-local authored pixels. */
    protected final int panelLocalY(int screenY) {
        return (int) Math.floor(viewport.unscaleY(screenY) - topPos);
    }

    /** Number of logical screen pixels drawn per authored panel pixel. */
    public final float panelScale() {
        return viewport.scale();
    }

    @Override
    public int getXSize() {
        return viewport.drawSize(imageWidth);
    }

    @Override
    public int getYSize() {
        return viewport.drawSize(imageHeight);
    }

    /** Menu command for an authored control; a negative value delegates normal inventory input. */
    protected int actionAt(int x, int y, int button) {
        return -1;
    }

    @Override
    protected boolean panelMouseClicked(double mouseX, double mouseY, int button) {
        double x = viewport.unscaleX(mouseX);
        double y = viewport.unscaleY(mouseY);
        int action = actionAt((int) Math.floor(x - leftPos), (int) Math.floor(y - topPos), button);
        if (action < 0) return super.panelMouseClicked(x, y, button);
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        return true;
    }

    @Override
    protected boolean panelMouseDragged(double x, double y, int button, double dx, double dy) {
        return super.panelMouseDragged(
                viewport.unscaleX(x),
                viewport.unscaleY(y),
                button,
                dx / panelScale(),
                dy / panelScale());
    }

    @Override
    protected boolean panelMouseReleased(double x, double y, int button) {
        return super.panelMouseReleased(viewport.unscaleX(x), viewport.unscaleY(y), button);
    }

    @Override
    protected boolean panelMouseScrolled(double x, double y, double delta) {
        return super.panelMouseScrolled(viewport.unscaleX(x), viewport.unscaleY(y), delta);
    }

    /** Paint background, inlaid controls and text before native item stacks are rendered. */
    protected abstract void paint(PanelCanvas canvas);

    /** Paint background and pointer-aware inlaid controls at authored panel coordinates. */
    protected void paint(PanelCanvas canvas, int mouseX, int mouseY) {
        paint(canvas);
    }

    /** Optional tooltip for an authored control at panel-local coordinates. */
    protected Component tooltipAt(int x, int y) {
        return null;
    }

    @Override
    protected void renderBg(PoseStack g, float tick, int mouseX, int mouseY) {
        paint(PanelGraphics.at(g, font, leftPos, topPos), mouseX - leftPos, mouseY - topPos);
    }

    @Override
    protected void renderLabels(PoseStack graphics, int mouseX, int mouseY) {}

    @Override
    protected void renderPanel(PoseStack g, int x, int y, float tick) {
        renderBackground(g);
        if (panelScale() == 1) {
            super.renderPanel(g, x, y, tick);
        } else {
            g.pushPose();
            try {
                g.translate(viewport.translateX(), viewport.translateY(), 0);
                g.scale(panelScale(), panelScale(), 1);
                super.renderPanel(
                        g,
                        (int) Math.floor(viewport.unscaleX(x)),
                        (int) Math.floor(viewport.unscaleY(y)),
                        tick);
            } finally {
                g.popPose();
            }
        }
        renderTooltip(g, x, y);
        if (menu.getCarried().isEmpty() && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            var tip =
                    tooltipAt(
                            (int) Math.floor(viewport.unscaleX(x) - leftPos),
                            (int) Math.floor(viewport.unscaleY(y) - topPos));
            if (tip != null) renderTooltip(g, tip, x, y);
        }
    }
}
