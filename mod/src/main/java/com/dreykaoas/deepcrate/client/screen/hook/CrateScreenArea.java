package com.dreykaoas.deepcrate.client.screen.hook;

import net.minecraft.client.gui.components.AbstractWidget;

/**
 * The room a mod is given on the crate screen, in screen pixels, with the panel's top left corner at
 * {@link #left()} and {@link #top()}.
 *
 * A widget dropped outside the panel has to be named through {@link #keepClickable}: the game counts
 * a click outside a container screen as a click into the world, and releasing one there throws on the
 * ground whatever the player is carrying.
 */
public interface CrateScreenArea {
    int left();

    int top();

    int width();

    int height();

    <T extends AbstractWidget> T addWidget(T widget);

    void keepClickable(int x, int y, int width, int height);
}
