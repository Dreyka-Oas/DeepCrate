package com.dreykaoas.deepcrate.api;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Whether a stack carries an item tag, asked from three places that all have to survive an unbound tag. */
public final class TagMatch {
    private TagMatch() {}

    public static boolean matches(ItemStack itemStack, TagKey<Item> items) {
        if (itemStack.isEmpty()) {
            return false;
        }

        try {
            return itemStack.is(items);
        } catch (IllegalStateException e) {
            // Tags bind when a world loads. A creative tab build or an addon's own registration asks
            // earlier than that, and gets "no" rather than a crash.
            return false;
        }
    }
}
