package com.dreykaoas.deepcrate.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

/**
 * One saved crate slot: what item, and how many.
 *
 * Vanilla's ItemStackWithSlot cannot be reused here. Its codec routes the count through
 * ItemStack.MAP_CODEC, which refuses anything above 99, so a slot holding 128 would come back
 * clamped or rejected. Splitting the item from its count sidesteps that range entirely.
 */
public record StoredSlot(int slot, ItemStack item, int count) {
    public static final Codec<StoredSlot> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
                ExtraCodecs.UNSIGNED_BYTE.fieldOf("Slot").forGetter(StoredSlot::slot),
                ItemStack.SINGLE_ITEM_CODEC.fieldOf("Item").forGetter(StoredSlot::item),
                ExtraCodecs.intRange(1, CrateStorage.SLOT_LIMIT).fieldOf("Count").forGetter(StoredSlot::count)
            )
            .apply(instance, StoredSlot::new)
    );

    public static StoredSlot of(int i, ItemStack itemStack) {
        return new StoredSlot(i, itemStack.copyWithCount(1), itemStack.getCount());
    }

    public ItemStack toStack() {
        return this.item.copyWithCount(this.count);
    }
}
