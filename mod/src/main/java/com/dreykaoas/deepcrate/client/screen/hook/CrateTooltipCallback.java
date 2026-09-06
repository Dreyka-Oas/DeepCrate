package com.dreykaoas.deepcrate.client.screen.hook;

import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** The lines of a tooltip on a crate screen, before they are drawn. */
@FunctionalInterface
public interface CrateTooltipCallback {
    Event<CrateTooltipCallback> EVENT = EventFactory.createArrayBacked(
        CrateTooltipCallback.class,
        listeners -> (menu, slot, itemStack, lines) -> {
            for (CrateTooltipCallback listener : listeners) {
                listener.addLines(menu, slot, itemStack, lines);
            }
        }
    );

    /**
     * @param slot the slot the pointer is over, which is how a listener tells a crate cell from a
     *             cell of the player's inventory: both draw tooltips through the same screen
     * @param lines the lines as they stand, writable: adding at index 1 puts a line right under the
     *              item's name
     */
    void addLines(DeepCrateMenu menu, @Nullable Slot slot, ItemStack itemStack, List<Component> lines);
}
