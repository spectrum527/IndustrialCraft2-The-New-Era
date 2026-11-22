package com.industrialage.ia2.gui;

import net.minecraft.client.gui.GuiGraphics;
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
    }

    /** Factor from authored coordinates to the current logical screen. */
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
    protected boolean panelMouseDragged(
            double mouseX, double mouseY, int button, double dx, double dy) {
        return super.panelMouseDragged(
                viewport.unscaleX(mouseX),
                viewport.unscaleY(mouseY),
                button,
                dx / viewport.scale(),
                dy / viewport.scale());
    }

    @Override
    protected boolean panelMouseReleased(double mouseX, double mouseY, int button) {
        return super.panelMouseReleased(
                viewport.unscaleX(mouseX), viewport.unscaleY(mouseY), button);
    }

    @Override
    protected boolean panelMouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        return super.panelMouseScrolled(
                viewport.unscaleX(mouseX), viewport.unscaleY(mouseY), dx, dy);
    }

    /** Paint background, inlaid controls and text before native item stacks are rendered. */
    protected abstract void paint(PanelCanvas canvas);

    @Override
    protected void renderBg(GuiGraphics g, float tick, int mouseX, int mouseY) {
        paint(PanelGraphics.at(g, font, leftPos, topPos));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public void renderBackground(GuiGraphics g, int x, int y, float tick) {
        if (viewport.scale() == 1) {
            super.renderBackground(g, x, y, tick);
        } else {
            renderBg(g, tick, x, y);
        }
    }

    @Override
    protected void renderPanel(GuiGraphics g, int x, int y, float tick) {
        if (viewport.scale() == 1) {
            super.renderPanel(g, x, y, tick);
        } else {
            renderTransparentBackground(g);
            var pose = g.pose();
            pose.pushPose();
            try {
                pose.translate(viewport.translateX(), viewport.translateY(), 0);
                pose.scale(viewport.scale(), viewport.scale(), 1);
                super.renderPanel(
                        g,
                        (int) Math.floor(viewport.unscaleX(x)),
                        (int) Math.floor(viewport.unscaleY(y)),
                        tick);
            } finally {
                pose.popPose();
            }
        }
        renderTooltip(g, x, y);
    }
}
