package oas.dreyka.deepcrate.inventory;

import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.api.module.CrateCapacityCallback;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What one slot of a crate holds for a given item, decided from the crate's own capacity, its
 * tier and whatever an addon layers on through {@link CrateCapacityCallback}.
 */
final class CrateSlotCapacity {
    private int capacity;
    private @Nullable CrateTier tier;

    // Takes a capacity CrateStorage's own constructor has already run through requirePositive,
    // so the check is not repeated here.
    CrateSlotCapacity(int capacity) {
        this.capacity = capacity;
    }

    int capacity() {
        return this.capacity;
    }

    /** Set when the crate lines up with its block, which is the first moment the tier is known. */
    void setTier(@Nullable CrateTier crateTier) {
        this.tier = crateTier;
    }

    /**
     * What one slot holds for a given item. A tool, a bucket or anything else the game refuses to
     * stack stays at its own limit: a module lifts stacks, it does not turn a sword into a stack of
     * swords.
     */
    int capacityFor(ItemStack itemStack) {
        // Never below one. A slot told it holds nothing would write a stack of nothing, and that is
        // how an item gets destroyed rather than refused; refusing is what accepts is for.
        return Math.max(1, this.limitFor(itemStack));
    }

    /** Whether this crate takes the item at all, which an addon decides through the event. */
    boolean accepts(ItemStack itemStack) {
        return this.limitFor(itemStack) > 0;
    }

    private int limitFor(ItemStack itemStack) {
        int proposed = !itemStack.isEmpty() && itemStack.getMaxStackSize() <= 1 ? itemStack.getMaxStackSize() : this.capacity;
        return CrateCapacityCallback.EVENT.invoker().capacity(this.tier, itemStack, proposed);
    }

    /** What a hopper or a pipe may push into one slot, which is not always what a player may. */
    int automationCapacityFor(ItemStack itemStack) {
        if (!this.accepts(itemStack)) {
            return 0;
        }

        int limit = this.capacityFor(itemStack);
        return DeepCrateApi.automationLimited() ? Math.min(limit, CrateStorage.VANILLA_LIMIT) : limit;
    }

    /**
     * Changes what a slot may hold. Nothing is cut back here: a slot left above the new capacity
     * keeps its content until the crate's overflow is drained, which is what happens when the
     * screen closes after a module is pulled out.
     */
    void setCapacity(int capacity) {
        this.capacity = requirePositive(capacity);
    }

    static int requirePositive(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("A slot capacity must be positive, got " + capacity);
        }

        return capacity;
    }
}
