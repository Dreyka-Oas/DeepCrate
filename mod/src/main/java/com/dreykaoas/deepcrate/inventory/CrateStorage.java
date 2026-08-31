package com.dreykaoas.deepcrate.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * The 27 slots of a deep crate, holding up to {@link #SLOT_LIMIT} of one item each.
 *
 * Nothing here touches a level, a menu or a player, so every rule below is reachable from a plain
 * JUnit test. Vanilla's own limit is only ever reached at the edges: what leaves the crate towards a
 * hand or a hopper is capped by {@link #VANILLA_LIMIT}, because a stack above that count cannot be
 * written back by ItemStack.CODEC.
 */
public final class CrateStorage {
    public static final int SLOT_COUNT = 27;
    public static final int SLOT_LIMIT = 128;
    public static final int VANILLA_LIMIT = 64;

    private NonNullList<ItemStack> slots = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public NonNullList<ItemStack> slots() {
        return this.slots;
    }

    public void replaceSlots(NonNullList<ItemStack> nonNullList) {
        if (nonNullList.size() != SLOT_COUNT) {
            throw new IllegalArgumentException("Deep crate takes " + SLOT_COUNT + " slots, got " + nonNullList.size());
        }

        this.slots = nonNullList;
    }

    public ItemStack get(int i) {
        return this.slots.get(i);
    }

    public void set(int i, ItemStack itemStack) {
        if (itemStack.getCount() > SLOT_LIMIT) {
            itemStack.setCount(SLOT_LIMIT);
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
     * Merges what fits into the crate and hands back the leftover. The incoming stack is consumed in
     * place, so a caller holding it sees the same remainder.
     */
    public ItemStack insert(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return itemStack;
        }

        for (int i = 0; i < SLOT_COUNT && !itemStack.isEmpty(); i++) {
            ItemStack itemStack2 = this.slots.get(i);
            if (!itemStack2.isEmpty() && ItemStack.isSameItemSameComponents(itemStack2, itemStack)) {
                int j = Math.min(SLOT_LIMIT - itemStack2.getCount(), itemStack.getCount());
                if (j > 0) {
                    itemStack2.grow(j);
                    itemStack.shrink(j);
                }
            }
        }

        for (int i = 0; i < SLOT_COUNT && !itemStack.isEmpty(); i++) {
            if (this.slots.get(i).isEmpty()) {
                this.slots.set(i, itemStack.split(Math.min(SLOT_LIMIT, itemStack.getCount())));
            }
        }

        return itemStack;
    }

    /**
     * Takes at most {@code wanted} out of a slot, never more than a hand can carry.
     */
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
     * The whole content cut into stacks a hand, an item entity or a save file can hold.
     */
    public List<ItemStack> splitForVanilla() {
        List<ItemStack> list = new ArrayList<>();

        for (ItemStack itemStack : this.slots) {
            int i = itemStack.getCount();
            while (i > 0) {
                int j = Math.min(i, VANILLA_LIMIT);
                list.add(itemStack.copyWithCount(j));
                i -= j;
            }
        }

        return list;
    }
}
