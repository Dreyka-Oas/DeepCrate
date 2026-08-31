package com.dreykaoas.deepcrate.inventory;

import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The single module slot, reading and writing the crate itself rather than a copy.
 *
 * A copy would be a duplication bug: two players opening the same crate would each hold their own
 * module, and the one who takes it out leaves the other with a phantom that can still be dropped
 * into the world.
 */
public class ModuleContainer implements Container {
    private final List<DeepCrateBlockEntity> crates;
    private final Runnable onChanged;

    public ModuleContainer(List<DeepCrateBlockEntity> crates, Runnable onChanged) {
        this.crates = crates;
        this.onChanged = onChanged;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.getItem(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int i) {
        return this.crates.isEmpty() ? ItemStack.EMPTY : this.crates.get(0).module();
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        ItemStack itemStack = this.getItem(0);
        if (itemStack.isEmpty() || j <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack taken = itemStack.split(j);
        this.setItem(0, itemStack);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        ItemStack itemStack = this.getItem(0);
        this.setItem(0, ItemStack.EMPTY);
        return itemStack;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        if (!this.crates.isEmpty()) {
            this.crates.get(0).setModule(itemStack);
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
        this.setItem(0, ItemStack.EMPTY);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
