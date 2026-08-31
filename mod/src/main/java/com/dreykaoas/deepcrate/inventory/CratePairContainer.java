package com.dreykaoas.deepcrate.inventory;

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
}
