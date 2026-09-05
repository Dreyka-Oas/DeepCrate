package com.dreykaoas.deepcrate.api.module;

import com.dreykaoas.deepcrate.api.TagMatch;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One cell of the tab hanging off the left of the crate screen.
 *
 * What the item sitting in it does is not decided here: a capacity module raises the crate from
 * whichever cell it sits in, a row module adds its rows from whichever cell it sits in. A kind that
 * matches neither is a cell the crate itself makes nothing of, and the mod that added it reads its
 * stack back through {@code DeepCrateBlockEntity.modules()}.
 *
 * @param order     place in the column, smallest at the top
 * @param emptyIcon the sprite drawn while the cell is empty, under {@code textures/gui/sprites}
 * @param filter    what the cell takes
 */
public record CrateModuleSlot(Identifier id, int order, int stackLimit, Identifier emptyIcon, Predicate<ItemStack> filter) {
    public CrateModuleSlot {
        if (stackLimit < 1) {
            throw new IllegalArgumentException("Module slot " + id + " needs a positive stack limit, got " + stackLimit);
        }
    }

    public boolean accepts(ItemStack itemStack) {
        return !itemStack.isEmpty() && this.filter.test(itemStack);
    }

    /**
     * The common case: a cell that takes whatever an item tag names, with no Java on the other side.
     * Answers false for an empty stack, whatever the tag.
     */
    public static Predicate<ItemStack> tagged(TagKey<Item> items) {
        return itemStack -> TagMatch.matches(itemStack, items);
    }
}
