package com.dreykaoas.deepcrate.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * The slots of one crate, each holding up to whatever the inserted module allows.
 *
 * Nothing here touches a level, a menu or a player, so every rule below is reachable from a plain
 * JUnit test. Vanilla's own limit is only ever met at the edges: what leaves towards a hand or an
 * item entity is capped by {@link #VANILLA_LIMIT}, because a stack above that count cannot be
 * written back by ItemStack.CODEC.
 */
public final class CrateStorage {
    public static final int VANILLA_LIMIT = 64;

    private NonNullList<ItemStack> slots;
    private int capacity;

    public CrateStorage(int slotCount, int capacity) {
        if (slotCount < 1) {
            throw new IllegalArgumentException("A crate needs at least one slot, got " + slotCount);
        }

        this.slots = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        this.capacity = requirePositive(capacity);
    }

    public int size() {
        return this.slots.size();
    }

    public int capacity() {
        return this.capacity;
    }

    /**
     * Changes what a slot may hold. Nothing is cut back here: a slot left above the new capacity
     * keeps its content until {@link #overflow()} is drained, which is what happens when the screen
     * closes after a module is pulled out.
     */
    public void setCapacity(int capacity) {
        this.capacity = requirePositive(capacity);
    }

    public NonNullList<ItemStack> slots() {
        return this.slots;
    }

    public void replaceSlots(NonNullList<ItemStack> nonNullList) {
        if (nonNullList.size() != this.slots.size()) {
            throw new IllegalArgumentException("This crate takes " + this.slots.size() + " slots, got " + nonNullList.size());
        }

        this.slots = nonNullList;
    }

    /**
     * Grows the crate to {@code slotCount}, keeping what is already there. Shrinking is refused: a
     * tier that lost rows in an update would otherwise silently swallow the slots beyond the new
     * end.
     */
    public void grow(int slotCount) {
        if (slotCount < this.slots.size()) {
            throw new IllegalArgumentException("A crate cannot shrink from " + this.slots.size() + " to " + slotCount);
        }

        if (slotCount == this.slots.size()) {
            return;
        }

        NonNullList<ItemStack> grown = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        for (int i = 0; i < this.slots.size(); i++) {
            grown.set(i, this.slots.get(i));
        }

        this.slots = grown;
    }

    public ItemStack get(int i) {
        return this.slots.get(i);
    }

    public void set(int i, ItemStack itemStack) {
        if (itemStack.getCount() > this.capacity) {
            itemStack.setCount(this.capacity);
        }

        this.slots.set(i, itemStack);
    }

    public boolean isEmpty() {
        for (ItemStack itemStack : this.slots) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public void clear() {
        this.slots.clear();
    }

    /**
     * Merges what fits and hands back the leftover. The incoming stack is consumed in place, so a
     * caller holding it sees the same remainder.
     */
    public ItemStack insert(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return itemStack;
        }

        for (int i = 0; i < this.slots.size() && !itemStack.isEmpty(); i++) {
            ItemStack itemStack2 = this.slots.get(i);
            if (!itemStack2.isEmpty() && ItemStack.isSameItemSameComponents(itemStack2, itemStack)) {
                int j = Math.min(this.capacity - itemStack2.getCount(), itemStack.getCount());
                if (j > 0) {
                    itemStack2.grow(j);
                    itemStack.shrink(j);
                }
            }
        }

        for (int i = 0; i < this.slots.size() && !itemStack.isEmpty(); i++) {
            if (this.slots.get(i).isEmpty()) {
                this.slots.set(i, itemStack.split(Math.min(this.capacity, itemStack.getCount())));
            }
        }

        return itemStack;
    }

    /** Takes at most {@code wanted} out of a slot, never more than a hand can carry. */
    public ItemStack extract(int i, int wanted) {
        if (wanted <= 0) {
            throw new IllegalArgumentException("Extract count must be positive, got " + wanted);
        }

        ItemStack itemStack = this.slots.get(i);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return itemStack.split(Math.min(wanted, VANILLA_LIMIT));
    }

    /**
     * Cuts every slot back to the current capacity and hands back what no longer fits, in stacks a
     * hand or an item entity can hold. Called when the screen closes.
     */
    public List<ItemStack> overflow() {
        List<ItemStack> spilled = new ArrayList<>();

        for (ItemStack itemStack : this.slots) {
            int excess = itemStack.getCount() - this.capacity;
            if (excess > 0) {
                itemStack.setCount(this.capacity);
                spilled.addAll(split(itemStack, excess));
            }
        }

        return spilled;
    }

    /** The whole content cut into stacks a hand, an item entity or a save file can hold. */
    public List<ItemStack> splitForVanilla() {
        List<ItemStack> list = new ArrayList<>();

        for (ItemStack itemStack : this.slots) {
            list.addAll(split(itemStack, itemStack.getCount()));
        }

        return list;
    }

    private static List<ItemStack> split(ItemStack itemStack, int count) {
        List<ItemStack> list = new ArrayList<>();
        int left = count;
        while (left > 0) {
            int take = Math.min(left, VANILLA_LIMIT);
            list.add(itemStack.copyWithCount(take));
            left -= take;
        }

        return list;
    }

    private static int requirePositive(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("A slot capacity must be positive, got " + capacity);
        }

        return capacity;
    }
}
