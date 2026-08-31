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

    /**
     * The server is the only authority on what a crate slot holds. Between a module being pulled out
     * and the screen closing, a slot legitimately holds more than the current capacity, and
     * SimpleContainer would cut the incoming value down to it.
     */
    @Override
    public void setItem(int i, ItemStack itemStack) {
        this.getItems().set(i, itemStack);
        this.setChanged();
    }
}
