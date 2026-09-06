package oas.dreyka.deepcrate.inventory.module;

import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.inventory.slot.StoredSlot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

/**
 * One saved module slot: which cell, what item, how many.
 *
 * The item is split from its count for the same reason {@link StoredSlot} does it: the vanilla stack
 * codec refuses a count above 99, and nothing stops an addon from opening a cell that takes more.
 */
public record StoredModule(Identifier id, ItemStack item, int count) {
    public static final Codec<StoredModule> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
                Identifier.CODEC.fieldOf("Id").forGetter(StoredModule::id),
                ItemStack.SINGLE_ITEM_CODEC.fieldOf("Item").forGetter(StoredModule::item),
                ExtraCodecs.intRange(1, DeepCrateApi.MAX_CAPACITY).fieldOf("Count").forGetter(StoredModule::count)
            )
            .apply(instance, StoredModule::new)
    );

    public static StoredModule of(Identifier identifier, ItemStack itemStack) {
        return new StoredModule(identifier, itemStack.copyWithCount(1), itemStack.getCount());
    }

    public ItemStack toStack() {
        return this.item.copyWithCount(this.count);
    }
}
