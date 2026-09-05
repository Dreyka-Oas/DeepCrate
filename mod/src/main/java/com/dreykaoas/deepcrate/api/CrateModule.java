package com.dreykaoas.deepcrate.api;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A capacity module. Anything in {@code items} raises every slot of the crate it sits in to
 * {@code capacity}.
 *
 * The module points at a tag rather than a single item on purpose: another mod can promote an item
 * it already ships by adding it to the tag, with no code and no registration.
 */
public record CrateModule(Identifier id, int capacity, TagKey<Item> items) {
    public CrateModule {
        if (capacity < 1) {
            throw new IllegalArgumentException("Crate module " + id + " needs a positive capacity, got " + capacity);
        }
    }

    public boolean matches(ItemStack itemStack) {
        return TagMatch.matches(itemStack, this.items);
    }
}
