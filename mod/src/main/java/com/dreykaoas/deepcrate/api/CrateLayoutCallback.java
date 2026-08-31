package com.dreykaoas.deepcrate.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Last word on how a crate is paged, called once per opening.
 *
 * Without this, the rows-per-page number would be a frozen constant and an addon could not change
 * the slot and page counts the way the design calls for.
 */
@FunctionalInterface
public interface CrateLayoutCallback {
    Event<CrateLayoutCallback> EVENT = EventFactory.createArrayBacked(
        CrateLayoutCallback.class,
        listeners -> (tier, rows, layout) -> {
            CrateLayout current = layout;
            for (CrateLayoutCallback listener : listeners) {
                current = listener.layout(tier, rows, current);
            }

            return current;
        }
    );

    /**
     * @param tier    the crate being opened
     * @param rows    its total rows, both halves counted when it is a double crate
     * @param layout  what the previous listener decided, starting from {@link CrateLayout#balanced}
     * @return the layout to use; returning {@code layout} unchanged opts out
     */
    CrateLayout layout(CrateTier tier, int rows, CrateLayout layout);
}
