package oas.dreyka.deepcrate.inventory;

import oas.dreyka.deepcrate.api.CrateTier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

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
    private final CrateSlotCapacity slotCapacity;

    public CrateStorage(int slotCount, int capacity) {
        if (slotCount < 1) {
            throw new IllegalArgumentException("A crate needs at least one slot, got " + slotCount);
        }

        this.slots = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        this.slotCapacity = new CrateSlotCapacity(CrateSlotCapacity.requirePositive(capacity));
    }

    public int size() {
        return this.slots.size();
    }

    public int capacity() {
        return this.slotCapacity.capacity();
    }

    public void setTier(@Nullable CrateTier crateTier) {
        this.slotCapacity.setTier(crateTier);
    }

    public int capacityFor(ItemStack itemStack) {
        return this.slotCapacity.capacityFor(itemStack);
    }

    public boolean accepts(ItemStack itemStack) {
        return this.slotCapacity.accepts(itemStack);
    }

    public int automationCapacityFor(ItemStack itemStack) {
        return this.slotCapacity.automationCapacityFor(itemStack);
    }

    public void setCapacity(int capacity) {
        this.slotCapacity.setCapacity(capacity);
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

    /**
     * Cuts the crate back to {@code slotCount} and hands back everything that was past the new end,
     * in stacks a hand or an item entity can hold. A crate already at or below that size is left
     * alone.
     */
    public List<ItemStack> trimTo(int slotCount) {
        if (slotCount < 1) {
            throw new IllegalArgumentException("A crate needs at least one slot, got " + slotCount);
        }

        if (slotCount >= this.slots.size()) {
            return List.of();
        }

        List<ItemStack> removed = new ArrayList<>();
        NonNullList<ItemStack> kept = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        for (int i = 0; i < this.slots.size(); i++) {
            if (i < slotCount) {
                kept.set(i, this.slots.get(i));
            } else {
                removed.addAll(split(this.slots.get(i), this.slots.get(i).getCount()));
            }
        }

        this.slots = kept;
        return removed;
    }

    public ItemStack get(int i) {
        return this.slots.get(i);
    }

    public void set(int i, ItemStack itemStack) {
        // A refused item written here is written whole: the caller is a command or another mod
        // forcing it, and clamping it to the refusal would grind the stack down to one.
        if (this.slotCapacity.accepts(itemStack)) {
            int limit = this.slotCapacity.capacityFor(itemStack);
            if (itemStack.getCount() > limit) {
                itemStack.setCount(limit);
            }
        }

        this.slots.set(i, itemStack);
    }

    /**
     * Puts a stack back exactly as it was saved, capacity or no capacity.
     *
     * Loading must never clamp: a crate reloaded before its module is known would silently destroy
     * everything above 64, and the half of a pair that does not hold the module never knows it.
     */
    public void restore(int i, ItemStack itemStack) {
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
        if (itemStack.isEmpty() || !this.slotCapacity.accepts(itemStack)) {
            return itemStack;
        }

        int limit = this.slotCapacity.capacityFor(itemStack);

        for (int i = 0; i < this.slots.size() && !itemStack.isEmpty(); i++) {
            ItemStack itemStack2 = this.slots.get(i);
            if (!itemStack2.isEmpty() && ItemStack.isSameItemSameComponents(itemStack2, itemStack)) {
                int j = Math.min(limit - itemStack2.getCount(), itemStack.getCount());
                if (j > 0) {
                    itemStack2.grow(j);
                    itemStack.shrink(j);
                }
            }
        }

        for (int i = 0; i < this.slots.size() && !itemStack.isEmpty(); i++) {
            if (this.slots.get(i).isEmpty()) {
                this.slots.set(i, itemStack.split(Math.min(limit, itemStack.getCount())));
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

        // Never more than the game can put in a hand, and never more than the item itself stacks to.
        return itemStack.split(Math.min(wanted, Math.min(VANILLA_LIMIT, itemStack.getMaxStackSize())));
    }

    /**
     * Cuts every slot back to the current capacity and hands back what no longer fits, in stacks a
     * hand or an item entity can hold. Called when the screen closes.
     */
    public List<ItemStack> overflow() {
        List<ItemStack> spilled = new ArrayList<>();

        for (ItemStack itemStack : this.slots) {
            // A crate that has started refusing what it already holds keeps it: there is no capacity
            // to cut back to, and the player takes it out by hand.
            if (!this.slotCapacity.accepts(itemStack)) {
                continue;
            }

            int limit = this.slotCapacity.capacityFor(itemStack);
            int excess = itemStack.getCount() - limit;
            if (excess > 0) {
                itemStack.setCount(limit);
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
        int piece = Math.min(VANILLA_LIMIT, Math.max(1, itemStack.getMaxStackSize()));
        int left = count;
        while (left > 0) {
            int take = Math.min(left, piece);
            list.add(itemStack.copyWithCount(take));
            left -= take;
        }

        return list;
    }
}
