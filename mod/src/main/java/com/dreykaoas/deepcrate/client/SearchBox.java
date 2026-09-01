package com.dreykaoas.deepcrate.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * The crate's search field, drawn smaller than the font of the game.
 *
 * There is one font and it does not come in sizes, so the whole widget is drawn under a scale. Its
 * own bounds are the inverse of that scale, which puts the drawn box back on the pixels asked for,
 * and {@link #isMouseOver} answers on those pixels rather than on the bounds. What stays out of step
 * is the caret placed by a click, which the base class works out from the raw mouse position: on a
 * long query it lands a character or two off. Typing and erasing are unaffected.
 */
public class SearchBox extends EditBox {
    private static final float SCALE = 0.75F;

    private final int drawnX;
    private final int drawnY;
    private final int drawnWidth;
    private final int drawnHeight;

    public SearchBox(Font font, int x, int y, int width, int height, Component component) {
        super(font, x, y, Math.round(width / SCALE), Math.round(height / SCALE), component);
        this.drawnX = x;
        this.drawnY = y;
        this.drawnWidth = width;
        this.drawnHeight = height;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(this.drawnX, this.drawnY);
        guiGraphics.pose().scale(SCALE, SCALE);
        guiGraphics.pose().translate(-this.drawnX, -this.drawnY);
        super.renderWidget(guiGraphics, i, j, f);
        guiGraphics.pose().popMatrix();
    }

    @Override
    public boolean isMouseOver(double d, double e) {
        return this.isVisible()
            && d >= this.drawnX
            && d < this.drawnX + this.drawnWidth
            && e >= this.drawnY
            && e < this.drawnY + this.drawnHeight;
    }
}
