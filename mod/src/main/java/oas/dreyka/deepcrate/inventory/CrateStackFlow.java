package oas.dreyka.deepcrate.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * What crosses the edge of a crate: a stack merged in, a stack taken out, and what a slot left above
 * the current capacity gives back. Built around the storage's own reference, the same pattern
 * {@code CrateModuleHolder} uses for a block entity.
 */
final class CrateStackFlow {
    private final CrateStorage storage;

    CrateStackFlow(CrateStorage storage) {
        this.storage = storage;
    }

    /**
     * Merges what fits and hands back the leftover. The incoming stack is consumed in place, so a
     * caller holding it sees the same remainder.
     */
    ItemStack insert(ItemStack itemStack) {
        if (itemStack.isEmpty() || !this.storage.accepts(itemStack)) {
            return itemStack;
        }

        NonNullList<ItemStack> slots = this.storage.slots();
        int limit = this.storage.capacityFor(itemStack);

        for (int i = 0; i < slots.size() && !itemStack.isEmpty(); i++) {
            ItemStack itemStack2 = slots.get(i);
            if (!itemStack2.isEmpty() && ItemStack.isSameItemSameComponents(itemStack2, itemStack)) {
                int j = Math.min(limit - itemStack2.getCount(), itemStack.getCount());
                if (j > 0) {
                    itemStack2.grow(j);
                    itemStack.shrink(j);
                }
            }
        }

        for (int i = 0; i < slots.size() && !itemStack.isEmpty(); i++) {
            if (slots.get(i).isEmpty()) {
                slots.set(i, itemStack.split(Math.min(limit, itemStack.getCount())));
            }
        }

        return itemStack;
    }

    /** Takes at most {@code wanted} out of a slot, never more than a hand can carry. */
    ItemStack extract(int i, int wanted) {
        if (wanted <= 0) {
            throw new IllegalArgumentException("Extract count must be positive, got " + wanted);
        }

        ItemStack itemStack = this.storage.slots().get(i);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // Never more than the game can put in a hand, and never more than the item itself stacks to.
        return itemStack.split(Math.min(wanted, Math.min(CrateStorage.VANILLA_LIMIT, itemStack.getMaxStackSize())));
    }

    /**
     * Cuts every slot back to the current capacity and hands back what no longer fits, in stacks a
     * hand or an item entity can hold. Called when the screen closes.
     */
    List<ItemStack> overflow() {
        List<ItemStack> spilled = new ArrayList<>();

        for (ItemStack itemStack : this.storage.slots()) {
            // A crate that has started refusing what it already holds keeps it: there is no capacity
            // to cut back to, and the player takes it out by hand.
            if (!this.storage.accepts(itemStack)) {
                continue;
            }

            int limit = this.storage.capacityFor(itemStack);
            int excess = itemStack.getCount() - limit;
            if (excess > 0) {
                itemStack.setCount(limit);
                spilled.addAll(CrateVanillaStacks.split(itemStack, excess));
            }
        }

        return spilled;
    }

    /**
     * A refused item written here is written whole: the caller is a command or another mod forcing
     * it, and clamping it to the refusal would grind the stack down to one.
     */
    ItemStack clampedToCapacity(ItemStack itemStack) {
        if (this.storage.accepts(itemStack)) {
            int limit = this.storage.capacityFor(itemStack);
            if (itemStack.getCount() > limit) {
                itemStack.setCount(limit);
            }
        }

        return itemStack;
    }
}
