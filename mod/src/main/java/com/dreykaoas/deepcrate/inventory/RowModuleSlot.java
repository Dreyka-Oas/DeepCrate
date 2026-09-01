package com.dreykaoas.deepcrate.inventory;

import com.dreykaoas.deepcrate.DeepCrate;
import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.api.RowModule;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The row modules, under the capacity module: a stack of them, one row of nine slots each. */
public class RowModuleSlot extends Slot {
    private static final Identifier EMPTY_ICON = Identifier.fromNamespaceAndPath(DeepCrate.MOD_ID, "container/slot/row_module");

    public RowModuleSlot(Container container, int i, int j, int k) {
        super(container, i, j, k);
    }

    @Override
    public Identifier getNoItemIcon() {
        return EMPTY_ICON;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return DeepCrateApi.rowModuleFor(itemStack) != null;
    }

    @Override
    public int getMaxStackSize() {
        return RowModule.STACK_LIMIT;
    }
}
