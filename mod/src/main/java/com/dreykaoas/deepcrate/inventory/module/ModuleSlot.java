package com.dreykaoas.deepcrate.inventory.module;

import com.dreykaoas.deepcrate.api.module.CrateModuleSlot;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One cell of the module tab, taking whatever its registered kind allows. */
public class ModuleSlot extends Slot {
    private final CrateModuleSlot kind;

    public ModuleSlot(Container container, int i, int j, int k, CrateModuleSlot crateModuleSlot) {
        super(container, i, j, k);
        this.kind = crateModuleSlot;
    }

    @Override
    public Identifier getNoItemIcon() {
        return this.kind.emptyIcon();
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return this.kind.accepts(itemStack);
    }

    @Override
    public int getMaxStackSize() {
        return this.kind.stackLimit();
    }
}
