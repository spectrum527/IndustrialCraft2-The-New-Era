package com.industrialage.ia2.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native draggable inventory with IC2's authored background and synchronized menu data. */
public abstract class AuthoredScreen<M extends AbstractContainerMenu> extends DeviceScreen<M> {
    private PanelViewport viewport = new PanelViewport(0, 0, 1);

    protected AuthoredScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
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
    protected boolean panelMouseClicked(MouseButtonEvent button, boolean twice) {
        var local = unscale(button);
        int action =
                actionAt(
                        (int) Math.floor(local.x() - leftPos),
                        (int) Math.floor(local.y() - topPos),
                        local.button());
        if (action < 0) return super.panelMouseClicked(local, twice);
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        return true;
    }

    private MouseButtonEvent unscale(MouseButtonEvent event) {
        return viewport.scale() == 1
                ? event
                : new MouseButtonEvent(
                        viewport.unscaleX(event.x()),
                        viewport.unscaleY(event.y()),
                        event.buttonInfo());
    }

    @Override
    protected boolean panelMouseDragged(MouseButtonEvent event, double dx, double dy) {
        return super.panelMouseDragged(
                unscale(event), dx / viewport.scale(), dy / viewport.scale());
    }

    @Override
    protected boolean panelMouseReleased(MouseButtonEvent event) {
        return super.panelMouseReleased(unscale(event));
    }

    @Override
    protected boolean panelMouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        return super.panelMouseScrolled(
                viewport.unscaleX(mouseX), viewport.unscaleY(mouseY), dx, dy);
    }

    /** Paint background, inlaid controls and text before native item stacks are rendered. */
    protected abstract void paint(PanelCanvas canvas);

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float tick) {
        paint(PanelGraphics.at(g, font, leftPos, topPos));
        super.extractContents(g, mouseX, mouseY, tick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    @Override
    protected void renderPanel(GuiGraphicsExtractor g, int x, int y, float tick) {
        if (viewport.scale() == 1) {
            super.renderPanel(g, x, y, tick);
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        try {
            pose.translate(viewport.translateX(), viewport.translateY());
            pose.scale(viewport.scale());
            super.renderPanel(
                    g,
                    (int) Math.floor(viewport.unscaleX(x)),
                    (int) Math.floor(viewport.unscaleY(y)),
                    tick);
        } finally {
            pose.popMatrix();
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int x, int y) {
        super.extractTooltip(g, viewport.screenX(x), viewport.screenY(y));
    }
}
