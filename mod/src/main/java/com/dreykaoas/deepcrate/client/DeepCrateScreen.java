package com.dreykaoas.deepcrate.client;

import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import com.dreykaoas.deepcrate.inventory.DeepCrateSlot;
import com.dreykaoas.deepcrate.net.CrateSortPayload;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The crate screen, built out of the chest texture of the base game.
 *
 * Nothing is shipped for the background: the header, the module strip, the slot frame, the grid and
 * the player inventory are blits from generic_54, which is what keeps a crate looking like a chest at
 * any row count.
 */
public class DeepCrateScreen extends AbstractContainerScreen<DeepCrateMenu> {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int HEADER_HEIGHT = 17;
    /**
     * The panel of generic_54: seven pixels of border, nine cells of eighteen, seven more.
     *
     * The eighteen-pixel band at u = 7 repeats exactly across all nine cells, on every row of the
     * file, which is what lets a panel of any width be built out of it without a seam.
     */
    private static final int PANEL_BORDER = DeepCrateMenu.PANEL_BORDER;
    private static final int PANEL_WIDTH = DeepCrateMenu.MIN_PANEL_WIDTH;
    private static final int CELL = DeepCrateMenu.CELL;
    /** The only band of the texture that is bare panel, measured on generic_54: rows 125 to 138. */
    private static final int BARE_PANEL_V = 125;
    private static final int BARE_PANEL_HEIGHT = 14;
    private static final int SLOT_FRAME_U = 7;
    private static final int SLOT_FRAME_V = 17;
    private static final int PLAYER_PANEL_V = 126;
    private static final int PLAYER_PANEL_HEIGHT = 96;
    /** The three rows that close the panel at the bottom. They run its full width, cells or not. */
    private static final int PANEL_FOOT = 3;
    private static final int PANEL_FOOT_V = PLAYER_PANEL_V + PLAYER_PANEL_HEIGHT - PANEL_FOOT;
    private static final int PAGE_BUTTONS_PER_COLUMN = 4;
    /** The gap between the last crate row and the first inventory row is fourteen pixels; this fits it. */
    private static final int SEARCH_HEIGHT = 11;
    /** The tab the module slot sits on, left of the panel: its own small panel with the same border. */
    private static final Identifier MODULE_TAB = Identifier.fromNamespaceAndPath("deepcrate", "textures/gui/module_tab.png");
    private static final int MODULE_TAB_WIDTH = 28;
    /** The tab is built as a cap, one cell, a foot: five rows of texture, eighteen, five. */
    private static final int MODULE_TAB_CAP = 5;
    private static final int MODULE_TAB_CELL = 18;
    /**
     * The texture carries two cells, so its foot starts below both. Reading it one cell up lands on
     * the top edge of the second, which closes the tab on the beginning of a slot that holds nothing.
     */
    private static final int MODULE_TAB_CELLS_DRAWN = 2;
    private static final int MODULE_TAB_FOOT_V = MODULE_TAB_CAP + MODULE_TAB_CELLS_DRAWN * MODULE_TAB_CELL;
    /** The tab is drawn this far up and left of the slot, so its frame lands exactly around it. */
    private static final int MODULE_TAB_MARGIN = 6;
    private static final int MODULE_TAB_TEXTURE = 64;
    /** The two sort buttons stand above the panel, flush with its left edge and clear of it. */
    private static final int SORT_GAP = 4;
    private static final int SORT_BUTTON_GAP = 2;
    /** Past four digits a count runs out of its cell, so it is shortened and the tooltip carries the truth. */
    private static final int ABBREVIATE_ABOVE = 999;

    private final int rows;
    private final int columns;
    private final int moduleTabHeight;
    private final List<PageButton> pageButtons = new ArrayList<>();
    /** One flag per page, raised while that page holds a stack. What the dot on a page button reads. */
    private boolean[] pagesHoldingItems = new boolean[0];

    private final List<SortButton> sortButtons = new ArrayList<>();
    /** Kept by name and not by position, so a mod loaded since does not shift every direction by one. */
    private final Map<Identifier, Boolean> sortDirections = new HashMap<>();

    private SearchBox searchBox;
    private String query = "";

    public DeepCrateScreen(DeepCrateMenu deepCrateMenu, Inventory inventory, Component component) {
        super(deepCrateMenu, inventory, component);
        this.rows = deepCrateMenu.layout().rowsPerPage();
        this.columns = deepCrateMenu.columns();
        this.moduleTabHeight = MODULE_TAB_CAP * 2 + DeepCrateApi.moduleSlots().size() * MODULE_TAB_CELL;
        // Exactly a chest of this many rows: the module hangs off the left edge rather than taking a
        // band inside the panel.
        this.imageWidth = deepCrateMenu.panelWidth();
        this.imageHeight = 114 + this.rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // On the inventory line, to the right of its label and running to the edge of the panel.
        int labelEnd = DeepCrateMenu.GRID_LEFT + this.font.width(this.playerInventoryTitle) + 6;
        this.searchBox = new SearchBox(
            this.font,
            this.leftPos + labelEnd,
            this.topPos + this.inventoryLabelY - 2,
            this.imageWidth - labelEnd - DeepCrateMenu.GRID_LEFT,
            SEARCH_HEIGHT,
            Component.translatable("screen.deepcrate.search")
        );
        this.searchBox.setHint(Component.translatable("screen.deepcrate.search"));
        this.searchBox.setMaxLength(48);
        this.searchBox.setResponder(this::onQueryChanged);
        this.searchBox.setValue(this.query);
        this.addRenderableWidget(this.searchBox);

        // Read before the widgets are replaced: a window resized mid-session rebuilds the screen, and
        // a button that forgot its direction would sort the same way twice in a row.
        for (SortButton sortButton : this.sortButtons) {
            this.sortDirections.put(sortButton.order().id(), sortButton.isReversed());
        }

        this.sortButtons.clear();
        List<CrateSortOrder> orders = DeepCrateClientApi.sortOrders();
        // Enough orders to outgrow the panel wrap onto a second line above the first, rather than
        // running off the side of the screen.
        int perRow = Math.max(1, this.imageWidth / (SortButton.SIZE + SORT_BUTTON_GAP));
        int sortRows = (orders.size() + perRow - 1) / perRow;
        for (int i = 0; i < orders.size(); i++) {
            CrateSortOrder crateSortOrder = orders.get(i);
            int row = sortRows - 1 - i / perRow;
            this.sortButtons.add(
                this.addRenderableWidget(
                    new SortButton(
                        this.leftPos + i % perRow * (SortButton.SIZE + SORT_BUTTON_GAP),
                        this.topPos - SORT_GAP - SortButton.SIZE - row * (SortButton.SIZE + SORT_BUTTON_GAP),
                        crateSortOrder,
                        this.sortDirections.getOrDefault(crateSortOrder.id(), false),
                        this::sort
                    )
                )
            );
        }

        this.pageButtons.clear();
        this.pagesHoldingItems = new boolean[this.menu.layout().pageCount()];
        this.readPagesHoldingItems();
        if (this.menu.layout().pageCount() < 2) {
            return;
        }

        // Stacked down the right edge, outside the panel: the crate grid already fills the width.
        // Four to a column, then a second column further right, so a crate with many pages does not
        // grow a strip taller than the screen.
        for (int page = 0; page < this.menu.layout().pageCount(); page++) {
            int target = page;
            this.pageButtons.add(
                this.addRenderableWidget(
                    new PageButton(
                        this.leftPos + this.imageWidth + 3 + page / PAGE_BUTTONS_PER_COLUMN * (PageButton.SIZE + 2),
                        this.topPos + HEADER_HEIGHT + page % PAGE_BUTTONS_PER_COLUMN * (PageButton.SIZE + 2),
                        Component.literal(String.valueOf(page + 1)),
                        () -> this.pagesHoldingItems[target],
                        () -> this.menu.setPage(target)
                    )
                )
            );
        }
    }

    /**
     * The page buttons sit past the right edge of the panel, which the game otherwise counts as
     * outside the screen: releasing a click there drops whatever the player is carrying on the ground.
     */
    @Override
    protected boolean hasClickedOutside(double d, double e, int i, int j) {
        return super.hasClickedOutside(d, e, i, j)
            && !this.isOverPageButtons(d, e)
            && !this.isOverModuleTab(d, e, i, j)
            && !this.isOverSortButtons(d, e);
    }

    private boolean isOverModuleTab(double d, double e, int i, int j) {
        int tabX = i + DeepCrateMenu.MODULE_X - MODULE_TAB_MARGIN;
        int tabY = j + DeepCrateMenu.MODULE_Y - MODULE_TAB_MARGIN;
        return d >= tabX && d < tabX + MODULE_TAB_WIDTH && e >= tabY && e < tabY + this.moduleTabHeight;
    }

    private boolean isOverSortButtons(double d, double e) {
        for (SortButton sortButton : this.sortButtons) {
            if (sortButton.isMouseOver(d, e)) {
                return true;
            }
        }

        return false;
    }

    private boolean isOverPageButtons(double d, double e) {
        for (PageButton pageButton : this.pageButtons) {
            if (pageButton.isMouseOver(d, e)) {
                return true;
            }
        }

        return false;
    }

    /**
     * A page emptied into the player's inventory has to lose its dot while the screen stays open, so
     * the flags are read again as the crate changes. Once a tick and once for every page, rather than
     * once a frame and once for every button: twelve buttons would otherwise walk the same slots
     * twelve times, sixty times a second.
     */
    @Override
    protected void containerTick() {
        super.containerTick();
        this.readPagesHoldingItems();
    }

    private void readPagesHoldingItems() {
        Arrays.fill(this.pagesHoldingItems, false);
        for (Slot slot : this.menu.slots) {
            if (slot instanceof DeepCrateSlot deepCrateSlot && !slot.getItem().isEmpty()) {
                this.pagesHoldingItems[deepCrateSlot.page()] = true;
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        // AbstractContainerScreen leaves this to the subclass, as ContainerScreen does; without it no
        // slot ever shows a tooltip.
        this.renderTooltip(guiGraphics, i, j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        int x = this.leftPos;
        int y = this.topPos;

        // Only the rows this page holds get slot cells; the last page of a crate whose rows do not
        // divide evenly would otherwise show a row of cells no slot lives in.
        int rowsOnPage = Math.min(
            this.rows,
            Math.max(0, this.menu.getContainer().getContainerSize() / this.columns - this.menu.page() * this.rows)
        );

        this.blitBand(guiGraphics, x, y, 0, HEADER_HEIGHT + rowsOnPage * 18, this.columns);
        if (rowsOnPage < this.rows) {
            this.fillBarePanel(guiGraphics, x, y + HEADER_HEIGHT + rowsOnPage * 18, (this.rows - rowsOnPage) * 18);
        }

        // The player keeps their nine columns whatever the crate has, so the band is asked for nine.
        // Its foot is drawn apart: bare panel carries no bottom edge, and a crate wider than the
        // player's inventory would otherwise stop short of closing on either side of it.
        int playerY = y + HEADER_HEIGHT + this.rows * 18;
        this.blitBand(guiGraphics, x, playerY, PLAYER_PANEL_V, PLAYER_PANEL_HEIGHT - PANEL_FOOT, DeepCrateMenu.COLUMNS_OF_A_PLAYER);
        // Asked for a full interior of cells rather than the crate's count: the foot carries no cell,
        // so tiling it end to end is what leaves no bare panel where the edge should be.
        int interior = this.imageWidth - PANEL_BORDER * 2;
        this.blitBand(guiGraphics, x, playerY + PLAYER_PANEL_HEIGHT - PANEL_FOOT, PANEL_FOOT_V, PANEL_FOOT, interior / CELL);
        this.renderModuleTab(guiGraphics, x, y);
    }

    /**
     * One horizontal band of the panel: the left border, a centred run of cells, the right border,
     * and bare panel filling whatever the run leaves on either side.
     *
     * At nine cells on a chest-wide panel the run fills the interior exactly and the three pieces
     * land where the single blit of a chest screen does, corners included.
     */
    private void blitBand(GuiGraphics guiGraphics, int x, int y, int v, int height, int cells) {
        int interior = this.imageWidth - PANEL_BORDER * 2;
        int left = (interior - cells * CELL) / 2;

        blit(guiGraphics, x, y, 0, v, PANEL_BORDER, height);
        this.fillBare(guiGraphics, x + PANEL_BORDER, y, left, height);
        for (int cell = 0; cell < cells; cell++) {
            blit(guiGraphics, x + PANEL_BORDER + left + cell * CELL, y, PANEL_BORDER, v, CELL, height);
        }

        this.fillBare(guiGraphics, x + PANEL_BORDER + left + cells * CELL, y, interior - left - cells * CELL, height);
        blit(guiGraphics, x + this.imageWidth - PANEL_BORDER, y, PANEL_WIDTH - PANEL_BORDER, v, PANEL_BORDER, height);
    }

    /** Panel with nothing on it, tiled both ways out of the one band of the texture that has none. */
    private void fillBare(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        for (int drawnDown = 0; drawnDown < height; drawnDown += BARE_PANEL_HEIGHT) {
            int band = Math.min(BARE_PANEL_HEIGHT, height - drawnDown);
            for (int drawnAcross = 0; drawnAcross < width; drawnAcross += CELL) {
                blit(guiGraphics, x + drawnAcross, y + drawnDown, PANEL_BORDER, BARE_PANEL_V, Math.min(CELL, width - drawnAcross), band);
            }
        }
    }

    /**
     * The bit of panel the module slot sits on: a small window of its own, bordered on all four sides
     * and standing a few pixels clear of the crate panel.
     * A slot itself has no texture in Minecraft: it is the background that carries the 18 by 18 cell.
     */
    private void renderModuleTab(GuiGraphics guiGraphics, int x, int y) {
        int tabX = x + DeepCrateMenu.MODULE_X - MODULE_TAB_MARGIN;
        int tabY = y + DeepCrateMenu.MODULE_Y - MODULE_TAB_MARGIN;
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

    /**
     * While the search field holds the keyboard, no key reaches the rest of the screen. The base
     * class only asks the focused widget first and then acts on whatever it did not claim, and a
     * plain letter is claimed by nobody: the "e" of "echo" would close the crate, a digit would swap
     * a slot into the hotbar, and the drop key would throw the item under the pointer. Escape is the
     * one key left through, so the screen can still be closed without reaching for the mouse.
     */
    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (this.searchBox != null && this.searchBox.isFocused() && keyEvent.key() != InputConstants.KEY_ESCAPE) {
            this.searchBox.keyPressed(keyEvent);
            return true;
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot, int i, int j) {
        ItemStack itemStack = slot.getItem();
        if (slot instanceof DeepCrateSlot && itemStack.getCount() > ABBREVIATE_ABOVE) {
            guiGraphics.renderItem(itemStack, slot.x, slot.y, slot.x + slot.y * this.imageWidth);
            guiGraphics.renderItemDecorations(this.font, itemStack, slot.x, slot.y, abbreviate(itemStack.getCount()));
        } else {
            super.renderSlot(guiGraphics, slot, i, j);
        }

        if (this.dims(slot)) {
            guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xB0101010);
        }
    }

    /** The abbreviated count hides the real one, so the item's own tooltip carries it. */
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        List<Component> lines = super.getTooltipFromContainerItem(itemStack);
        if (this.hoveredSlot instanceof DeepCrateSlot && itemStack.getCount() > ABBREVIATE_ABOVE) {
            lines = new ArrayList<>(lines);
            lines.add(1, Component.translatable("screen.deepcrate.count", itemStack.getCount()));
        }

        return lines;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        // The page being read, as a bare number against the right edge of the title line.
        String page = String.valueOf(this.menu.page() + 1);
        int pageX = this.imageWidth - 7 - this.font.width(page);
        guiGraphics.drawString(this.font, page, pageX, this.titleLabelY, 0xFF404040, false);

        // A crate named on an anvil can be longer than the panel; the name stops before the number
        // rather than running under it.
        int room = pageX - this.titleLabelX - 4;
        Component title = this.font.width(this.title) <= room
            ? this.title
            : Component.literal(this.font.plainSubstrByWidth(this.title.getString(), room - this.font.width("...")) + "...");
        guiGraphics.drawString(this.font, title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    /**
     * The order is worked out here and sent whole, because it comes from the item names in the
     * language this player reads and the crate has no idea what that is.
     */
    private void sort(CrateSortOrder crateSortOrder, boolean reversed) {
        ClientPlayNetworking.send(
            new CrateSortPayload(this.menu.containerId, DeepCrateClientApi.order(crateSortOrder, this.menu.getContainer(), reversed))
        );
    }

    /**
     * A crate can hold twelve pages, and a slot on a page that is not open is not drawn at all, so a
     * search that matched nothing visible would read as a search that matched nothing. The screen
     * turns to the first page holding a match instead.
     */
    private void onQueryChanged(String text) {
        this.query = text.toLowerCase(Locale.ROOT).trim();
        if (this.query.isEmpty() || this.menu.layout().pageCount() < 2) {
            return;
        }

        for (Slot slot : this.menu.slots) {
            if (slot instanceof DeepCrateSlot deepCrateSlot && !slot.getItem().isEmpty() && !this.dims(slot)) {
                this.menu.setPage(deepCrateSlot.page());
                return;
            }
        }
    }

    /**
     * Whether a slot is greyed out by the search. Only the crate's own slots answer: dimming the
     * player's inventory as well would leave nothing readable on screen.
     */
    private boolean dims(Slot slot) {
        if (this.query.isEmpty() || !(slot instanceof DeepCrateSlot) || slot.getItem().isEmpty()) {
            return false;
        }

        return !slot.getItem().getHoverName().getString().toLowerCase(Locale.ROOT).contains(this.query);
    }

    /** Tiles the one bare band of the texture over a height it does not natively cover. */
    private void fillBarePanel(GuiGraphics guiGraphics, int x, int y, int height) {
        int drawn = 0;
        while (drawn < height) {
            int slice = Math.min(BARE_PANEL_HEIGHT, height - drawn);
            this.blitBand(guiGraphics, x, y + drawn, BARE_PANEL_V, slice, this.columns);
            drawn += slice;
        }
    }

    private static void blit(GuiGraphics guiGraphics, int x, int y, int u, int v, int width, int height) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, u, v, width, height, 256, 256);
    }

    private static String abbreviate(int count) {
        if (count >= 1_000_000) {
            return count / 1_000_000 + "M";
        }

        return count / 1000 + "k";
    }
}
