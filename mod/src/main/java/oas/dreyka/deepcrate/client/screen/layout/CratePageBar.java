package oas.dreyka.deepcrate.client.screen.layout;

import oas.dreyka.deepcrate.client.screen.hook.PanelArea;
import oas.dreyka.deepcrate.inventory.menu.DeepCrateMenu;
import oas.dreyka.deepcrate.inventory.slot.DeepCrateSlot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import oas.dreyka.deepcrate.client.widget.PageButton;

/**
 * The page buttons stacked down the right edge of a crate, and the dot each one carries while its
 * own page holds a stack.
 */
final class CratePageBar {
    private static final int PAGE_BUTTONS_PER_COLUMN = 4;

    private final DeepCrateMenu menu;
    private final List<PageButton> pageButtons = new ArrayList<>();
    /** One flag per page, raised while that page holds a stack. What the dot on a page button reads. */
    private boolean[] pagesHoldingItems = new boolean[0];

    CratePageBar(DeepCrateMenu menu) {
        this.menu = menu;
    }

    /** Rebuilds the buttons for the current layout: called from init(), again on every window resize. */
    void init(PanelArea panelArea, int leftPos, int topPos, int imageWidth) {
        this.pageButtons.clear();
        this.pagesHoldingItems = new boolean[this.menu.wiring().layout().pageCount()];
        this.readPagesHoldingItems();
        this.addPageButtons(panelArea, leftPos, topPos, imageWidth);
    }

    /**
     * A page emptied into the player's inventory has to lose its dot while the screen stays open, so
     * the flags are read again as the crate changes. Once a tick and once for every page, rather than
     * once a frame and once for every button: twelve buttons would otherwise walk the same slots
     * twelve times, sixty times a second.
     */
    void tick() {
        this.readPagesHoldingItems();
    }

    private void addPageButtons(PanelArea panelArea, int leftPos, int topPos, int imageWidth) {
        if (this.menu.wiring().layout().pageCount() < 2) {
            return;
        }

        // Stacked down the right edge, outside the panel: the crate grid already fills the width.
        // Four to a column, then a second column further right, so a crate with many pages does not
        // grow a strip taller than the screen.
        for (int page = 0; page < this.menu.wiring().layout().pageCount(); page++) {
            int target = page;
            this.pageButtons.add(
                panelArea.addClickableWidget(
                    new PageButton(
                        leftPos + imageWidth + 3 + page / PAGE_BUTTONS_PER_COLUMN * (PageButton.SIZE + 2),
                        topPos + CratePanel.HEADER_HEIGHT + page % PAGE_BUTTONS_PER_COLUMN * (PageButton.SIZE + 2),
                        Component.literal(String.valueOf(page + 1)),
                        () -> this.pagesHoldingItems[target],
                        () -> this.menu.setPage(target)
                    )
                )
            );
        }
    }

    private void readPagesHoldingItems() {
        Arrays.fill(this.pagesHoldingItems, false);
        for (Slot slot : this.menu.slots) {
            if (slot instanceof DeepCrateSlot deepCrateSlot && !slot.getItem().isEmpty()) {
                this.pagesHoldingItems[deepCrateSlot.page()] = true;
            }
        }
    }
}
