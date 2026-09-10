package oas.dreyka.deepcrate.inventory.slot;

import oas.dreyka.deepcrate.api.DeepCrateApi;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

/**
 * One saved slot: whichever key locates it, what item, and how many.
 *
 * A crate slot is keyed by its index, a module cell by its {@code Identifier}; {@link #codec} takes
 * that key's own codec and NBT field name, so each caller still writes the same two shapes it always
 * did. Vanilla's ItemStackWithSlot cannot be reused here: its codec routes the count through
 * ItemStack.MAP_CODEC, which refuses anything above 99, so a slot holding 512 would come back clamped
 * or rejected. Splitting the item from its count sidesteps that range entirely.
 */
public record StoredEntry<K>(K key, ItemStack item, int count) {
    public static <K> Codec<StoredEntry<K>> codec(String keyField, Codec<K> keyCodec) {
        return RecordCodecBuilder.create(
            instance -> instance.group(
                    keyCodec.fieldOf(keyField).forGetter(StoredEntry::key),
                    ItemStack.SINGLE_ITEM_CODEC.fieldOf("Item").forGetter(StoredEntry::item),
                    ExtraCodecs.intRange(1, DeepCrateApi.MAX_CAPACITY).fieldOf("Count").forGetter(StoredEntry::count)
                )
                .apply(instance, StoredEntry::new)
        );
    }

    public static <K> StoredEntry<K> of(K key, ItemStack itemStack) {
        return new StoredEntry<>(key, itemStack.copyWithCount(1), itemStack.getCount());
    }

    public ItemStack toStack() {
        return this.item.copyWithCount(this.count);
    }
}
