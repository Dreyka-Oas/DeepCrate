package oas.dreyka.deepcrate.inventory.slot;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import oas.dreyka.deepcrate.inventory.container.CrateStorage;
import oas.dreyka.deepcrate.inventory.container.CrateVanillaStacks;

/**
 * How many slots a crate has: growing it when its tier or its row modules call for more, cutting it
 * back when they call for fewer, and swapping the whole list for one the same length. Built around
 * the storage's own reference, the same pattern {@code CrateModuleHolder} uses for a block entity.
 */
public final class CrateSlotResize {
    private final CrateStorage storage;

    public CrateSlotResize(CrateStorage storage) {
        this.storage = storage;
    }

    public void replaceSlots(NonNullList<ItemStack> nonNullList) {
        int size = this.storage.slots().size();
        if (nonNullList.size() != size) {
            throw new IllegalArgumentException("This crate takes " + size + " slots, got " + nonNullList.size());
        }

        this.storage.setSlots(nonNullList);
    }

    /**
     * Grows the crate to {@code slotCount}, keeping what is already there. Shrinking is refused: a
     * tier that lost rows in an update would otherwise silently swallow the slots beyond the new
     * end.
     */
    public void grow(int slotCount) {
        NonNullList<ItemStack> slots = this.storage.slots();
        if (slotCount < slots.size()) {
            throw new IllegalArgumentException("A crate cannot shrink from " + slots.size() + " to " + slotCount);
        }

        if (slotCount == slots.size()) {
            return;
        }

        NonNullList<ItemStack> grown = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        for (int i = 0; i < slots.size(); i++) {
            grown.set(i, slots.get(i));
        }

        this.storage.setSlots(grown);
    }

    /**
     * Cuts the crate back to {@code slotCount} and hands back everything that was past the new end,
     * in stacks a hand or an item entity can hold. A crate already at or below that size is left
     * alone.
     */
    public List<ItemStack> trimTo(int slotCount) {
        if (slotCount < 1) {
            throw new IllegalArgumentException("A crate needs at least one slot, got " + slotCount);
        }

        NonNullList<ItemStack> slots = this.storage.slots();
        if (slotCount >= slots.size()) {
            return List.of();
        }

        List<ItemStack> removed = new ArrayList<>();
        NonNullList<ItemStack> kept = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        for (int i = 0; i < slots.size(); i++) {
            if (i < slotCount) {
                kept.set(i, slots.get(i));
            } else {
                removed.addAll(CrateVanillaStacks.split(slots.get(i), slots.get(i).getCount()));
            }
        }

        this.storage.setSlots(kept);
        return removed;
    }
}
