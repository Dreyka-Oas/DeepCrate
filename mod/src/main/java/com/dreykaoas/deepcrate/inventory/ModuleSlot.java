package com.dreykaoas.deepcrate.inventory;

import com.dreykaoas.deepcrate.DeepCrate;
import com.dreykaoas.deepcrate.api.DeepCrateApi;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The one slot that takes a capacity module, on its own tab to the left of the screen. */
public class ModuleSlot extends Slot {
    private static final Identifier EMPTY_ICON = Identifier.fromNamespaceAndPath(DeepCrate.MOD_ID, "container/slot/module");

    public ModuleSlot(Container container, int i, int j, int k) {
        super(container, i, j, k);
    }

    @Override
    public Identifier getNoItemIcon() {
        return EMPTY_ICON;
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
