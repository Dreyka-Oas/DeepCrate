package oas.dreyka.deepcrate.client.screen.layout;

import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.init.RegistryInit;
import oas.dreyka.deepcrate.inventory.slot.CratePanelGeometry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Paints the panel a crate screen is built out of, and the tab its module slot sits on, both blitted
 * from vanilla's chest texture and the mod's own small one.
 *
 * DeepCrateScreen decides which bands to draw and how many cells each needs; this class only turns
 * that into blits.
 */
final class CratePanel {
    private CratePanel() {}

    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    static final int HEADER_HEIGHT = 17;
    /**
     * The panel of generic_54: seven pixels of border, nine cells of eighteen, seven more.
     *
     * The eighteen-pixel band at u = 7 repeats exactly across all nine cells, on every row of the
     * file, which is what lets a panel of any width be built out of it without a seam.
     */
    private static final int PANEL_BORDER = CratePanelGeometry.PANEL_BORDER;
    private static final int PANEL_WIDTH = CratePanelGeometry.MIN_PANEL_WIDTH;
    private static final int CELL = CratePanelGeometry.CELL;
    /** The only band of the texture that is bare panel, measured on generic_54: rows 125 to 138. */
    private static final int BARE_PANEL_V = 125;
    private static final int BARE_PANEL_HEIGHT = 14;
    static final int PLAYER_PANEL_V = 126;
    static final int PLAYER_PANEL_HEIGHT = 96;
    /** The three rows that close the panel at the bottom. They run its full width, cells or not. */
    static final int PANEL_FOOT = 3;
    static final int PANEL_FOOT_V = PLAYER_PANEL_V + PLAYER_PANEL_HEIGHT - PANEL_FOOT;
    /** The tab the module slot sits on, left of the panel: its own small panel with the same border. */
    private static final Identifier MODULE_TAB = RegistryInit.id("textures/gui/module_tab.png");
    static final int MODULE_TAB_WIDTH = 28;
    /** The tab is built as a cap, one cell, a foot: five rows of texture, eighteen, five. */
    static final int MODULE_TAB_CAP = 5;
    static final int MODULE_TAB_CELL = 18;
    /**
     * The texture carries two cells, so its foot starts below both. Reading it one cell up lands on
     * the top edge of the second, which closes the tab on the beginning of a slot that holds nothing.
     */
    private static final int MODULE_TAB_CELLS_DRAWN = 2;
    private static final int MODULE_TAB_FOOT_V = MODULE_TAB_CAP + MODULE_TAB_CELLS_DRAWN * MODULE_TAB_CELL;
    /** The tab is drawn this far up and left of the slot, so its frame lands exactly around it. */
    static final int MODULE_TAB_MARGIN = 6;
    private static final int MODULE_TAB_TEXTURE = 64;

    /**
     * One horizontal band of the panel: the left border, a centred run of cells, the right border,
     * and bare panel filling whatever the run leaves on either side.
     *
     * At nine cells on a chest-wide panel the run fills the interior exactly and the three pieces
     * land where the single blit of a chest screen does, corners included.
     */
    static void blitBand(GuiGraphics guiGraphics, int x, int y, int v, int height, int cells, int panelWidth) {
        int interior = panelWidth - PANEL_BORDER * 2;
        int left = (interior - cells * CELL) / 2;

        blit(guiGraphics, x, y, 0, v, PANEL_BORDER, height);
        fillBare(guiGraphics, x + PANEL_BORDER, y, left, height);
        for (int cell = 0; cell < cells; cell++) {
            blit(guiGraphics, x + PANEL_BORDER + left + cell * CELL, y, PANEL_BORDER, v, CELL, height);
        }

        fillBare(guiGraphics, x + PANEL_BORDER + left + cells * CELL, y, interior - left - cells * CELL, height);
        blit(guiGraphics, x + panelWidth - PANEL_BORDER, y, PANEL_WIDTH - PANEL_BORDER, v, PANEL_BORDER, height);
    }

    /** Panel with nothing on it, tiled both ways out of the one band of the texture that has none. */
    private static void fillBare(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        for (int drawnDown = 0; drawnDown < height; drawnDown += BARE_PANEL_HEIGHT) {
            int band = Math.min(BARE_PANEL_HEIGHT, height - drawnDown);
            for (int drawnAcross = 0; drawnAcross < width; drawnAcross += CELL) {
                blit(guiGraphics, x + drawnAcross, y + drawnDown, PANEL_BORDER, BARE_PANEL_V, Math.min(CELL, width - drawnAcross), band);
            }
        }
    }

    /** Tiles the one bare band of the texture over a height it does not natively cover. */
    static void fillBarePanel(GuiGraphics guiGraphics, int x, int y, int height, int panelWidth, int columns) {
        int drawn = 0;
        while (drawn < height) {
            int slice = Math.min(BARE_PANEL_HEIGHT, height - drawn);
            blitBand(guiGraphics, x, y + drawn, BARE_PANEL_V, slice, columns, panelWidth);
            drawn += slice;
        }
    }

    /**
     * The bit of panel the module slot sits on: a small window of its own, bordered on all four sides
     * and standing a few pixels clear of the crate panel.
     * A slot itself has no texture in Minecraft: it is the background that carries the 18 by 18 cell.
     */
    static void renderModuleTab(GuiGraphics guiGraphics, int x, int y) {
        int tabX = x + CratePanelGeometry.MODULE_X - MODULE_TAB_MARGIN;
        int tabY = y + CratePanelGeometry.MODULE_Y - MODULE_TAB_MARGIN;
        int cells = DeepCrateApi.moduleSlots().size();

        blitTab(guiGraphics, tabX, tabY, 0, MODULE_TAB_CAP);
        for (int cell = 0; cell < cells; cell++) {
            blitTab(guiGraphics, tabX, tabY + MODULE_TAB_CAP + cell * MODULE_TAB_CELL, MODULE_TAB_CAP, MODULE_TAB_CELL);
        }

        blitTab(guiGraphics, tabX, tabY + MODULE_TAB_CAP + cells * MODULE_TAB_CELL, MODULE_TAB_FOOT_V, MODULE_TAB_CAP);
    }

    private static void blitTab(GuiGraphics guiGraphics, int x, int y, int v, int height) {
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED, MODULE_TAB, x, y, 0.0F, (float) v, MODULE_TAB_WIDTH, height, MODULE_TAB_TEXTURE, MODULE_TAB_TEXTURE
        );
    }

    private static void blit(GuiGraphics guiGraphics, int x, int y, int u, int v, int width, int height) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, u, v, width, height, 256, 256);
    }
}
