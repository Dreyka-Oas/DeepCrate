package com.dreykaoas.deepcrate.inventory;

import com.dreykaoas.deepcrate.api.RowModule;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The two module slots, reading and writing the crate itself rather than a copy.
 *
 * A copy would be a duplication bug: two players opening the same crate would each hold their own
 * module, and the one who takes it out leaves the other with a phantom that can still be dropped
 * into the world.
 */
public class ModuleContainer implements Container {
    public static final int CAPACITY_SLOT = 0;
    public static final int ROWS_SLOT = 1;

    private final List<DeepCrateBlockEntity> crates;
    private final Runnable onChanged;

    public ModuleContainer(List<DeepCrateBlockEntity> crates, Runnable onChanged) {
        this.crates = crates;
        this.onChanged = onChanged;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return this.getItem(CAPACITY_SLOT).isEmpty() && this.getItem(ROWS_SLOT).isEmpty();
    }

    @Override
    public ItemStack getItem(int i) {
        if (this.crates.isEmpty()) {
            return ItemStack.EMPTY;
        }

        DeepCrateBlockEntity holder = this.crates.get(0);
        return i == ROWS_SLOT ? holder.rowModules() : holder.module();
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        ItemStack itemStack = this.getItem(i);
        if (itemStack.isEmpty() || j <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack taken = itemStack.split(j);
        this.setItem(i, itemStack);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        ItemStack itemStack = this.getItem(i);
        this.setItem(i, ItemStack.EMPTY);
        return itemStack;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        if (!this.crates.isEmpty()) {
            DeepCrateBlockEntity holder = this.crates.get(0);
            if (i == ROWS_SLOT) {
                holder.setRowModules(itemStack);
            } else {
                holder.setModule(itemStack);
            }
        }

        this.setChanged();
    }

    @Override
    public void setChanged() {
        this.onChanged.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.crates.isEmpty() || this.crates.get(0).stillValid(player);
    }

    @Override
    public void clearContent() {
        this.setItem(CAPACITY_SLOT, ItemStack.EMPTY);
        this.setItem(ROWS_SLOT, ItemStack.EMPTY);
    }

    @Override
    public int getMaxStackSize() {
        return RowModule.STACK_LIMIT;
    }
}
