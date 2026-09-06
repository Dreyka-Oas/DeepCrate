package com.dreykaoas.deepcrate.api.module;

import com.dreykaoas.deepcrate.api.TagMatch;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A row module. Each one in the crate's second module slot adds {@code rows} rows of nine slots, and
 * the slot takes a stack of them, so a crate can be extended several rows at a time.
 *
 * Like a capacity module it points at a tag rather than at one item, so another mod can promote an
 * item it already ships without any code.
 */
public record RowModule(Identifier id, int rows, TagKey<Item> items) {
    public RowModule {
        if (rows < 1) {
            throw new IllegalArgumentException("Row module " + id + " needs a positive row count, got " + rows);
        }
    }

    public boolean matches(ItemStack itemStack) {
        return TagMatch.matches(itemStack, this.items);
    }
}
