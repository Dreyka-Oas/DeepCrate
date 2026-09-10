package oas.dreyka.deepcrate.api.module;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * A capacity module. Anything in {@code items} raises every slot of the crate it sits in to
 * {@code capacity}.
 *
 * The module points at a tag rather than a single item on purpose: another mod can promote an item
 * it already ships by adding it to the tag, with no code and no registration.
 */
public record CrateModule(Identifier id, int capacity, TagKey<Item> items) implements TaggedModule {
    public CrateModule {
        TaggedModule.requirePositive(id, capacity, "Crate module", "capacity");
    }
}
