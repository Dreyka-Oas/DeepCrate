package com.dreykaoas.deepcrate.client;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** What a mod can add to the crate screen: for now, an order the sort buttons offer. */
public final class DeepCrateClientApi {
    private static final List<CrateSortOrder> SORT_ORDERS = new ArrayList<>();

    private DeepCrateClientApi() {}

    public static CrateSortOrder registerSortOrder(CrateSortOrder crateSortOrder) {
        for (CrateSortOrder existing : SORT_ORDERS) {
            if (existing.id().equals(crateSortOrder.id())) {
                throw new IllegalStateException("Sort order " + crateSortOrder.id() + " registered twice");
            }
        }

        SORT_ORDERS.add(crateSortOrder);
        SORT_ORDERS.sort(Comparator.comparingInt(CrateSortOrder::order));
        return crateSortOrder;
    }

    public static List<CrateSortOrder> sortOrders() {
        return Collections.unmodifiableList(SORT_ORDERS);
    }

    public static @Nullable CrateSortOrder sortOrder(Identifier identifier) {
        for (CrateSortOrder crateSortOrder : SORT_ORDERS) {
            if (crateSortOrder.id().equals(identifier)) {
                return crateSortOrder;
            }
        }

        return null;
    }

    /**
     * Which items the crate should hold in which order. Worked out here rather than on the server:
     * only this side knows which of the two names the player is reading.
     */
    public static List<Item> order(CrateSortOrder crateSortOrder, Container container, boolean reversed) {
        Map<Item, Long> totals = new LinkedHashMap<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack itemStack = container.getItem(i);
            if (!itemStack.isEmpty()) {
                totals.merge(itemStack.getItem(), (long) itemStack.getCount(), Long::sum);
            }
        }

        Comparator<Item> comparator = crateSortOrder.rule().comparator(totals, Collator.getInstance(gameLocale()));
        List<Item> items = new ArrayList<>(totals.keySet());
        items.sort(reversed ? comparator.reversed() : comparator);
        return items;
    }

    /**
     * The plain name of the item, not the name of the stack: a renamed pile belongs with the rest of
     * its kind rather than under the letter someone typed on an anvil.
     */
    public static String nameOf(Item item) {
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
