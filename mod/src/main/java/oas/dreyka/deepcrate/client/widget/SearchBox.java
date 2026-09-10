package oas.dreyka.deepcrate.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The crate's search field, drawn smaller than the font of the game.
 *
 * There is one font and it does not come in sizes, so the whole widget is drawn under a scale. Its
 * own bounds are the inverse of that scale, which puts the drawn box back on the pixels asked for.
 * {@link #isMouseOver} answers on the drawn pixels rather than on the bounds, and a click is handed
 * on with its position mapped into the same space, so the caret lands under the pointer.
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
    public void onClick(MouseButtonEvent mouseButtonEvent, boolean bl) {
        super.onClick(this.intoBoxSpace(mouseButtonEvent), bl);
    }

    @Override
    protected void onDrag(MouseButtonEvent mouseButtonEvent, double d, double e) {
        super.onDrag(this.intoBoxSpace(mouseButtonEvent), d, e);
    }

    /**
     * The mouse arrives in screen pixels, while the base class reads it against bounds that live in
     * the scaled space. Undoing the scale about the drawn corner is what keeps a click on the tenth
     * letter from landing on the seventh.
     */
    private MouseButtonEvent intoBoxSpace(MouseButtonEvent mouseButtonEvent) {
        return new MouseButtonEvent(
            this.drawnX + (mouseButtonEvent.x() - this.drawnX) / SCALE,
            this.drawnY + (mouseButtonEvent.y() - this.drawnY) / SCALE,
            mouseButtonEvent.buttonInfo()
        );
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
