package com.industrialage.ia2.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native draggable inventory with IC2's authored background and synchronized menu data. */
public abstract class AuthoredScreen<M extends AbstractContainerMenu>
        extends AbstractContainerScreen<M> {
    private PanelViewport viewport = new PanelViewport(0, 0, 1);

    protected AuthoredScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    @Override
    protected void init() {
        super.init();
        viewport = PanelViewport.fit(width, height, imageWidth, imageHeight);
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
    public boolean mouseClicked(MouseButtonEvent button, boolean twice) {
        var local = unscale(button);
        int action =
                actionAt(
                        (int) Math.floor(local.x() - leftPos),
                        (int) Math.floor(local.y() - topPos),
                        local.button());
        if (action < 0) return super.mouseClicked(local, twice);
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
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return super.mouseDragged(unscale(event), dx / viewport.scale(), dy / viewport.scale());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(unscale(event));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        return super.mouseScrolled(viewport.unscaleX(mouseX), viewport.unscaleY(mouseY), dx, dy);
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
    public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float tick) {
        if (viewport.scale() == 1) {
            super.extractRenderState(g, x, y, tick);
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        try {
            pose.translate(viewport.translateX(), viewport.translateY());
            pose.scale(viewport.scale());
            super.extractRenderState(
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
