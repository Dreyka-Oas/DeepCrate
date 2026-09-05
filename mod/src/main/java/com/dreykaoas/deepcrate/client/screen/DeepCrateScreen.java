package com.dreykaoas.deepcrate.client.screen;

import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.client.sort.CrateSortOrder;
import com.dreykaoas.deepcrate.client.sort.DeepCrateClientApi;
import com.dreykaoas.deepcrate.client.sort.SortButton;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import com.dreykaoas.deepcrate.inventory.DeepCrateSlot;
import com.dreykaoas.deepcrate.net.CrateSortPayload;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
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
    /**
     * The panel of generic_54: seven pixels of border, nine cells of eighteen, seven more.
     *
     * The eighteen-pixel band at u = 7 repeats exactly across all nine cells, on every row of the
     * file, which is what lets a panel of any width be built out of it without a seam.
     */
    private static final int PANEL_BORDER = DeepCrateMenu.PANEL_BORDER;
    private static final int CELL = DeepCrateMenu.CELL;
    private static final int PAGE_BUTTONS_PER_COLUMN = 4;
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

    private final CrateSearch crateSearch;
    private SearchBox searchBox;

    public DeepCrateScreen(DeepCrateMenu deepCrateMenu, Inventory inventory, Component component) {
        super(deepCrateMenu, inventory, component);
        this.rows = deepCrateMenu.layout().rowsPerPage();
        this.columns = deepCrateMenu.columns();
        this.crateSearch = new CrateSearch(deepCrateMenu);
        this.moduleTabHeight = CratePanel.MODULE_TAB_CAP * 2 + DeepCrateApi.moduleSlots().size() * CratePanel.MODULE_TAB_CELL;
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
        this.searchBox = this.crateSearch.buildBox(
            this.font, this.leftPos + labelEnd, this.topPos + this.inventoryLabelY - 2, this.imageWidth - labelEnd - DeepCrateMenu.GRID_LEFT
        );
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
                        this.topPos + CratePanel.HEADER_HEIGHT + page % PAGE_BUTTONS_PER_COLUMN * (PageButton.SIZE + 2),
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
        int tabX = i + DeepCrateMenu.MODULE_X - CratePanel.MODULE_TAB_MARGIN;
        int tabY = j + DeepCrateMenu.MODULE_Y - CratePanel.MODULE_TAB_MARGIN;
        return d >= tabX && d < tabX + CratePanel.MODULE_TAB_WIDTH && e >= tabY && e < tabY + this.moduleTabHeight;
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

        CratePanel.blitBand(guiGraphics, x, y, 0, CratePanel.HEADER_HEIGHT + rowsOnPage * 18, this.columns, this.imageWidth);
        if (rowsOnPage < this.rows) {
            CratePanel.fillBarePanel(
                guiGraphics, x, y + CratePanel.HEADER_HEIGHT + rowsOnPage * 18, (this.rows - rowsOnPage) * 18, this.imageWidth, this.columns
            );
        }

        // The player keeps their nine columns whatever the crate has, so the band is asked for nine.
        // Its foot is drawn apart: bare panel carries no bottom edge, and a crate wider than the
        // player's inventory would otherwise stop short of closing on either side of it.
        int playerY = y + CratePanel.HEADER_HEIGHT + this.rows * 18;
        CratePanel.blitBand(
            guiGraphics, x, playerY, CratePanel.PLAYER_PANEL_V, CratePanel.PLAYER_PANEL_HEIGHT - CratePanel.PANEL_FOOT,
            DeepCrateMenu.COLUMNS_OF_A_PLAYER, this.imageWidth
        );
        // Asked for a full interior of cells rather than the crate's count: the foot carries no cell,
        // so tiling it end to end is what leaves no bare panel where the edge should be.
        int interior = this.imageWidth - PANEL_BORDER * 2;
        CratePanel.blitBand(
            guiGraphics, x, playerY + CratePanel.PLAYER_PANEL_HEIGHT - CratePanel.PANEL_FOOT, CratePanel.PANEL_FOOT_V, CratePanel.PANEL_FOOT,
            interior / CELL, this.imageWidth
        );
        CratePanel.renderModuleTab(guiGraphics, x, y);
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

        if (this.crateSearch.dims(slot)) {
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

    private static String abbreviate(int count) {
        if (count >= 1_000_000) {
            return count / 1_000_000 + "M";
        }

        return count / 1000 + "k";
    }
}
