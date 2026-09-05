package com.dreykaoas.deepcrate.inventory;

/**
 * Where the panel, its grid and its module cell sit, in the pixel space of the chest texture the
 * screen is built out of. The menu and the screen both read these: a slot only ever lands where it
 * was drawn.
 */
public final class CratePanelGeometry {
    private CratePanelGeometry() {}

    /** Border, cells, border: the 176 of a chest screen is 7 + 9 * 18 + 7. */
    public static final int PANEL_BORDER = 7;
    public static final int CELL = 18;
    /**
     * No panel is narrower than a chest's, whatever the crate is. The player's own nine columns are
     * drawn on it too, and they do not fit in less.
     */
    public static final int MIN_PANEL_WIDTH = 176;
    /** A player's inventory is nine wide, and a crate of another width does not change that. */
    public static final int COLUMNS_OF_A_PLAYER = 9;

    public static final int GRID_LEFT = 8;
    /** The chest's own grid position: the module lives outside the panel, so nothing is pushed down. */
    public static final int GRID_TOP = 18;
    /** Left of the panel, on its own tab, standing clear of it rather than glued to its edge. */
    public static final int MODULE_X = -25;
    public static final int MODULE_Y = 18;
    /** One cell under the next, eighteen pixels apart, as many as there are registered kinds. */
    public static final int MODULE_SPACING = 18;

    public static int panelWidth(int columns) {
        return Math.max(MIN_PANEL_WIDTH, PANEL_BORDER * 2 + columns * CELL);
    }

    /**
     * Where the first slot of a run of {@code cells} columns sits, centred in the panel. The extra
     * pixel is the cell frame: a slot is drawn one in from the corner of its cell.
     */
    public static int gridLeft(int panelWidth, int cells) {
        return PANEL_BORDER + (panelWidth - PANEL_BORDER * 2 - cells * CELL) / 2 + 1;
    }
}
