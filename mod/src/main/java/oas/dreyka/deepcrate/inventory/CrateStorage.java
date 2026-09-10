package oas.dreyka.deepcrate.inventory;

import oas.dreyka.deepcrate.api.CrateTier;
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
    private final CrateSlotResize resize = new CrateSlotResize(this);
    private final CrateStackFlow flow = new CrateStackFlow(this);

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

    /** Package-private on purpose: only {@link CrateSlotResize} decides that a crate changes length. */
    void setSlots(NonNullList<ItemStack> nonNullList) {
        this.slots = nonNullList;
    }

    public void replaceSlots(NonNullList<ItemStack> nonNullList) {
        this.resize.replaceSlots(nonNullList);
    }

    public void grow(int slotCount) {
        this.resize.grow(slotCount);
    }

    public List<ItemStack> trimTo(int slotCount) {
        return this.resize.trimTo(slotCount);
    }

    public ItemStack get(int i) {
        return this.slots.get(i);
    }

    public void set(int i, ItemStack itemStack) {
        this.slots.set(i, this.flow.clampedToCapacity(itemStack));
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

    public ItemStack insert(ItemStack itemStack) {
        return this.flow.insert(itemStack);
    }

    public ItemStack extract(int i, int wanted) {
        return this.flow.extract(i, wanted);
    }

    public List<ItemStack> overflow() {
        return this.flow.overflow();
    }

    /** The whole content cut into stacks a hand, an item entity or a save file can hold. */
    public List<ItemStack> splitForVanilla() {
        return CrateVanillaStacks.splitAll(this.slots);
    }
}
