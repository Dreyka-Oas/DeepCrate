package oas.dreyka.deepcrate.api.module;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * A row module. Each one in the crate's second module slot adds {@code rows} rows of nine slots, and
 * the slot takes a stack of them, so a crate can be extended several rows at a time.
 *
 * Like a capacity module it points at a tag rather than at one item, so another mod can promote an
 * item it already ships without any code.
 */
public record RowModule(Identifier id, int rows, TagKey<Item> items) implements TaggedModule {
    public RowModule {
        TaggedModule.requirePositive(id, rows, "Row module", "row count");
    }
}
