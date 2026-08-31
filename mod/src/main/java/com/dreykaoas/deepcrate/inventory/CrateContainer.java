package com.dreykaoas.deepcrate.inventory;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * The client side stand-in for a crate, filled by the slot packets the server sends. Only the two
 * limits differ from a plain container: without them every incoming stack would be cut to 64 on
 * arrival.
 */
public class CrateContainer extends SimpleContainer {
    private int capacity;

    public CrateContainer(int slotCount, int capacity) {
        super(slotCount);
        this.capacity = capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int getMaxStackSize() {
        return this.capacity;
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return this.capacity;
    }
}
