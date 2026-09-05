package com.dreykaoas.deepcrate.inventory.module;

import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.api.module.CrateModuleSlot;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The module cells, reading and writing the crate itself rather than a copy.
 *
 * A copy would be a duplication bug: two players opening the same crate would each hold their own
 * module, and the one who takes it out leaves the other with a phantom that can still be dropped into
 * the world.
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
        return DeepCrateApi.moduleSlots().size();
    }

    @Override
    public boolean isEmpty() {
        return this.crates.isEmpty() || this.crates.get(0).modules().isEmpty();
    }

    @Override
    public ItemStack getItem(int i) {
        Identifier identifier = idAt(i);
        return this.crates.isEmpty() || identifier == null ? ItemStack.EMPTY : this.crates.get(0).modules().get(identifier);
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
        Identifier identifier = idAt(i);
        if (!this.crates.isEmpty() && identifier != null) {
            this.crates.get(0).setModuleIn(identifier, itemStack);
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
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.setItem(i, ItemStack.EMPTY);
        }
    }

    /**
     * The widest cell of the lot. A slot answers its own limit through {@link ModuleSlot}; this is
     * what the container promises, and promising less than a cell allows would clamp it.
     */
    @Override
    public int getMaxStackSize() {
        int limit = 1;
        for (CrateModuleSlot crateModuleSlot : DeepCrateApi.moduleSlots()) {
            limit = Math.max(limit, crateModuleSlot.stackLimit());
        }

        return limit;
    }

    private static @Nullable Identifier idAt(int i) {
        List<CrateModuleSlot> kinds = DeepCrateApi.moduleSlots();
        return i < 0 || i >= kinds.size() ? null : kinds.get(i).id();
    }
}
