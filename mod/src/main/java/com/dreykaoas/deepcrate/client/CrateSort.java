package com.dreykaoas.deepcrate.client;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The two orders the buttons above the crate offer. */
public enum CrateSort {
    /** Alphabetical, in the language the player has the game in. */
    NAME,
    /** The fullest pile first, then the letters between piles of the same size. */
    COUNT;

    /**
     * Which items the crate should hold in which order. Worked out here rather than on the server:
     * "Pierre" and "Stone" do not sort to the same place, and only this side knows which of the two
     * the player is reading.
     */
    public List<Item> order(Container container, boolean reversed) {
        Map<Item, Long> totals = new LinkedHashMap<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack itemStack = container.getItem(i);
            if (!itemStack.isEmpty()) {
                totals.merge(itemStack.getItem(), (long) itemStack.getCount(), Long::sum);
            }
        }

        Collator collator = Collator.getInstance(gameLocale());
        Comparator<Item> byName = Comparator.comparing(CrateSort::nameOf, collator);
        Comparator<Item> comparator = this == NAME
            ? byName
            : Comparator.<Item, Long>comparing(totals::get).reversed().thenComparing(byName);

        List<Item> items = new ArrayList<>(totals.keySet());
        items.sort(reversed ? comparator.reversed() : comparator);
        return items;
    }

    /**
     * The plain name of the item, not the name of the stack: a renamed pile belongs with the rest of
     * its kind rather than under the letter someone typed on an anvil.
     */
    private static String nameOf(Item item) {
        return Component.translatable(item.getDescriptionId()).getString();
    }

    /**
     * Accents decide where a word lands, and only a collator built for the right language puts them
     * where a reader of that language expects.
     */
    private static Locale gameLocale() {
        String[] parts = Minecraft.getInstance().options.languageCode.split("_");
        return parts.length < 2 ? Locale.of(parts[0]) : Locale.of(parts[0], parts[1].toUpperCase(Locale.ROOT));
    }
}
