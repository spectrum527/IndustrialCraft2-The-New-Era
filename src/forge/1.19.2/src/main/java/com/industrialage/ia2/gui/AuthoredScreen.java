package com.industrialage.ia2.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native draggable inventory with IC2's authored background and synchronized menu data. */
public abstract class AuthoredScreen<M extends AbstractContainerMenu>
        extends AbstractContainerScreen<M> {
    private PanelViewport viewport = new PanelViewport(0, 0, 1);

    protected AuthoredScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title);
        imageWidth = width;
        imageHeight = height;
    }

    @Override
    protected void init() {
        super.init();
        viewport = PanelViewport.fit(width, height, imageWidth, imageHeight);
        leftPos = viewport.left();
        topPos = viewport.top();
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = viewport.unscaleX(mouseX);
        double y = viewport.unscaleY(mouseY);
        int action = actionAt((int) Math.floor(x - leftPos), (int) Math.floor(y - topPos), button);
        if (action < 0) return super.mouseClicked(x, y, button);
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(
                viewport.unscaleX(x),
                viewport.unscaleY(y),
                button,
                dx / panelScale(),
                dy / panelScale());
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return super.mouseReleased(viewport.unscaleX(x), viewport.unscaleY(y), button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        return super.mouseScrolled(viewport.unscaleX(x), viewport.unscaleY(y), delta);
    }

    /** Paint background, inlaid controls and text before native item stacks are rendered. */
    protected abstract void paint(PanelCanvas canvas);

    @Override
    protected void renderBg(PoseStack g, float tick, int mouseX, int mouseY) {
        paint(PanelGraphics.at(g, font, leftPos, topPos));
    }

    @Override
    protected void renderLabels(PoseStack graphics, int mouseX, int mouseY) {}

    @Override
    public void render(PoseStack g, int x, int y, float tick) {
        renderBackground(g);
        if (panelScale() == 1) {
            super.render(g, x, y, tick);
        } else {
            g.pushPose();
            try {
                g.translate(viewport.translateX(), viewport.translateY(), 0);
                g.scale(panelScale(), panelScale(), 1);
                super.render(
                        g,
                        (int) Math.floor(viewport.unscaleX(x)),
                        (int) Math.floor(viewport.unscaleY(y)),
                        tick);
            } finally {
                g.popPose();
            }
        }
        renderTooltip(g, x, y);
    }
}
