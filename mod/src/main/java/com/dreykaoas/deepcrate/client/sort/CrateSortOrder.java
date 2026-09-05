package com.dreykaoas.deepcrate.client.sort;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * One button above the crate.
 *
 * @param order place in the row, smallest first
 * @param icon  sixteen wide and thirty-two tall: the plain way up top, the reversed one under it
 */
public record CrateSortOrder(Identifier id, int order, Identifier icon, CrateSortRule rule) {
    /** The drawing shows what the next press will do, so the wording follows the same rule. */
    public Component label(boolean reversed) {
        return Component.translatable("screen." + this.id.getNamespace() + ".sort." + this.id.getPath() + (reversed ? "_reversed" : ""));
    }
}
