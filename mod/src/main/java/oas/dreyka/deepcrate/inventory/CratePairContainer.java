package oas.dreyka.deepcrate.inventory;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Two crates seen as one.
 *
 * CompoundContainer already answers the first half for {@code getMaxStackSize()}, which is where the
 * shared module lives. What it leaves at the Container default is {@code getMaxStackSize(ItemStack)},
 * and that default folds back to the item's own limit of 64.
 */
public class CratePairContainer extends CompoundContainer {
    private final Container holder;

    public CratePairContainer(Container container, Container container2) {
        super(container, container2);
        this.holder = container;
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return this.holder.getMaxStackSize(itemStack);
    }

    /**
     * Both halves answer the same, because both follow the same module and the same tier. Asking the
     * first is what CompoundContainer does everywhere else, and it saves resolving which half a slot
     * index falls in.
     */
    @Override
    public boolean canPlaceItem(int i, ItemStack itemStack) {
        return this.holder.canPlaceItem(0, itemStack);
    }
}
