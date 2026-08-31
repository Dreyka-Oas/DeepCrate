package com.dreykaoas.deepcrate.inventory;

import com.dreykaoas.deepcrate.api.DeepCrateApi;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The one slot that takes a capacity module, top left of the screen. */
public class ModuleSlot extends Slot {
    public ModuleSlot(Container container, int i, int j, int k) {
        super(container, i, j, k);
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return DeepCrateApi.moduleFor(itemStack) != null;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
