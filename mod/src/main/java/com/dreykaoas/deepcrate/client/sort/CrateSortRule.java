package com.dreykaoas.deepcrate.client.sort;

import java.text.Collator;
import java.util.Comparator;
import java.util.Map;
import net.minecraft.world.item.Item;

/**
 * How one order compares two kinds of item.
 *
 * The collator comes from the language the player reads, which is why an order lives on the client: a
 * server holds no language files, so it cannot know that this player reads Pierre where another reads
 * Stone.
 */
@FunctionalInterface
public interface CrateSortRule {
    /**
     * @param totals   how many of each kind the crate holds, every slot counted
     * @param collator built for the player's language, so accents land where a reader expects
     */
    Comparator<Item> comparator(Map<Item, Long> totals, Collator collator);
}
