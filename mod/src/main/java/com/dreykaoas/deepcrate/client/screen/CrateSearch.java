package com.dreykaoas.deepcrate.client.screen;

import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import com.dreykaoas.deepcrate.inventory.DeepCrateSlot;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

/**
 * The search field above a crate's own inventory, and what typing into it does to the grid: a stack
 * whose name does not match is greyed out rather than hidden, so a player still sees where it sits.
 *
 * The query survives past the field that holds it: a window resized mid-session rebuilds the search
 * box, and the text typed into the old one has to land in the new one rather than being lost.
 */
final class CrateSearch {
    static final int SEARCH_HEIGHT = 11;

    private final DeepCrateMenu menu;
    private String query = "";

    CrateSearch(DeepCrateMenu menu) {
        this.menu = menu;
    }

    /** A fresh widget wired to this state, carrying over whatever was typed before it was rebuilt. */
    SearchBox buildBox(Font font, int x, int y, int width) {
        SearchBox searchBox = new SearchBox(font, x, y, width, SEARCH_HEIGHT, Component.translatable("screen.deepcrate.search"));
        searchBox.setHint(Component.translatable("screen.deepcrate.search"));
        searchBox.setMaxLength(48);
        searchBox.setResponder(this::onQueryChanged);
        searchBox.setValue(this.query);
        return searchBox;
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
    boolean dims(Slot slot) {
        if (this.query.isEmpty() || !(slot instanceof DeepCrateSlot) || slot.getItem().isEmpty()) {
            return false;
        }

        return !slot.getItem().getHoverName().getString().toLowerCase(Locale.ROOT).contains(this.query);
    }
}
