package com.dreykaoas.deepcrate.client.screen.hook;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * The room the crate screen gives away, and the rectangles it defends outside its own panel.
 *
 * Built again on every layout: a resized window moves all of them, and a rectangle left over from the
 * previous size either swallows a click meant for the world or lets one through that drops what the
 * player is carrying.
 */
public final class PanelArea implements CrateScreenArea {
    /** What a screen's own {@code addRenderableWidget} is, reached from outside the class. */
    public interface WidgetSink {
        <T extends AbstractWidget> T add(T widget);
    }

    private record Rect(int x, int y, int width, int height) {
        boolean holds(double pointerX, double pointerY) {
            return pointerX >= this.x
                && pointerX < this.x + this.width
                && pointerY >= this.y
                && pointerY < this.y + this.height;
        }
    }

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final WidgetSink widgetSink;
    private final List<Rect> clickable = new ArrayList<>();

    public PanelArea(int left, int top, int width, int height, WidgetSink widgetSink) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.widgetSink = widgetSink;
    }

    @Override
    public int left() {
        return this.left;
    }

    @Override
    public int top() {
        return this.top;
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    @Override
    public <T extends AbstractWidget> T addWidget(T widget) {
        return this.widgetSink.add(widget);
    }

    @Override
    public void keepClickable(int x, int y, int width, int height) {
        this.clickable.add(new Rect(x, y, width, height));
    }

    /** Whether a click the game reads as outside the panel is on something drawn there anyway. */
    public boolean holdsClick(double pointerX, double pointerY) {
        for (Rect rect : this.clickable) {
            if (rect.holds(pointerX, pointerY)) {
                return true;
            }
        }

        return false;
    }

    /** The widget goes on the screen and its rectangle is defended, which is what a button needs. */
    public <T extends AbstractWidget> T addClickableWidget(T widget) {
        this.keepClickable(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
        return this.addWidget(widget);
    }
}
