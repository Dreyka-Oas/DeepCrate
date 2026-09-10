package oas.dreyka.deepcrate.client.screen;

import oas.dreyka.deepcrate.inventory.CratePanelGeometry;
import oas.dreyka.deepcrate.inventory.DeepCrateMenu;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Which bands of the chest texture a crate screen is made of, for the page being read: the crate
 * grid, whatever bare panel a short last page leaves under it, the player's own nine columns, the
 * foot that closes them and the module tab.
 */
final class CrateScreenBackground {
    private CrateScreenBackground() {}

    /**
     * The panel of generic_54: seven pixels of border, nine cells of eighteen, seven more.
     *
     * The eighteen-pixel band at u = 7 repeats exactly across all nine cells, on every row of the
     * file, which is what lets a panel of any width be built out of it without a seam.
     */
    private static final int PANEL_BORDER = CratePanelGeometry.PANEL_BORDER;
    private static final int CELL = CratePanelGeometry.CELL;

    static void render(GuiGraphics guiGraphics, DeepCrateMenu menu, int x, int y, int imageWidth, int rows, int columns) {
        // Only the rows this page holds get slot cells; the last page of a crate whose rows do not
        // divide evenly would otherwise show a row of cells no slot lives in.
        int rowsOnPage = Math.min(rows, Math.max(0, menu.getContainer().getContainerSize() / columns - menu.page() * rows));

        CratePanel.blitBand(guiGraphics, x, y, 0, CratePanel.HEADER_HEIGHT + rowsOnPage * 18, columns, imageWidth);
        if (rowsOnPage < rows) {
            CratePanel.fillBarePanel(guiGraphics, x, y + CratePanel.HEADER_HEIGHT + rowsOnPage * 18, (rows - rowsOnPage) * 18, imageWidth, columns);
        }

        // The player keeps their nine columns whatever the crate has, so the band is asked for nine.
        // Its foot is drawn apart: bare panel carries no bottom edge, and a crate wider than the
        // player's inventory would otherwise stop short of closing on either side of it.
        int playerY = y + CratePanel.HEADER_HEIGHT + rows * 18;
        CratePanel.blitBand(
            guiGraphics, x, playerY, CratePanel.PLAYER_PANEL_V, CratePanel.PLAYER_PANEL_HEIGHT - CratePanel.PANEL_FOOT,
            CratePanelGeometry.COLUMNS_OF_A_PLAYER, imageWidth
        );
        // Asked for a full interior of cells rather than the crate's count: the foot carries no cell,
        // so tiling it end to end is what leaves no bare panel where the edge should be.
        int interior = imageWidth - PANEL_BORDER * 2;
        CratePanel.blitBand(
            guiGraphics, x, playerY + CratePanel.PLAYER_PANEL_HEIGHT - CratePanel.PANEL_FOOT, CratePanel.PANEL_FOOT_V, CratePanel.PANEL_FOOT,
            interior / CELL, imageWidth
        );
        CratePanel.renderModuleTab(guiGraphics, x, y);
    }
}
