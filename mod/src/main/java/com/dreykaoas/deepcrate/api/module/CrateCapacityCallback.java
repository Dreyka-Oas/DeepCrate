package com.dreykaoas.deepcrate.api.module;

import com.dreykaoas.deepcrate.api.CrateTier;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Last word on what one crate slot holds for one item.
 *
 * Without this the rule is frozen: whatever does not stack keeps its own limit, everything else takes
 * the module's number. A crate that refuses gunpowder, or one that holds four times as much ore, has
 * no way in.
 */
@FunctionalInterface
public interface CrateCapacityCallback {
    Event<CrateCapacityCallback> EVENT = EventFactory.createArrayBacked(
        CrateCapacityCallback.class,
        listeners -> (tier, itemStack, proposed) -> {
            int current = proposed;
            for (CrateCapacityCallback listener : listeners) {
                current = listener.capacity(tier, itemStack, current);
            }

            return current;
        }
    );

    /**
     * @param tier     the crate asking, null while one is being read from a save and its block is not
     *                 bound to a level yet
     * @param proposed what the previous listener decided, starting from the crate's own rule
     * @return the limit for this item; zero or less means this crate refuses it
     */
    int capacity(@Nullable CrateTier tier, ItemStack itemStack, int proposed);
}
