package oas.dreyka.deepcrate.api.module;

import oas.dreyka.deepcrate.api.TagMatch;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What {@link CrateModule} and {@link RowModule} share: an id, a tag of matching items, and the guard
 * on the one number each of them carries. Package-private on purpose, an addon keeps compiling against
 * the two record names it already knows.
 */
interface TaggedModule {
    Identifier id();

    TagKey<Item> items();

    default boolean matches(ItemStack itemStack) {
        return TagMatch.matches(itemStack, this.items());
    }

    static void requirePositive(Identifier id, int value, String kind, String noun) {
        if (value < 1) {
            throw new IllegalArgumentException(kind + " " + id + " needs a positive " + noun + ", got " + value);
        }
    }
}
