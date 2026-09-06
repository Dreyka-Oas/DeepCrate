package oas.dreyka.deepcrate.inventory.slot;

import oas.dreyka.deepcrate.inventory.CrateStorage;
import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A crate slot: it accepts what the crate's module allows, never hands out more than a hand can
 * carry, and disappears when its page is not the one being shown.
 */
public class DeepCrateSlot extends Slot {
    private final int page;

    private boolean visible = true;

    public DeepCrateSlot(Container container, int i, int j, int k, int page) {
        super(container, i, j, k);
        this.page = page;
    }

    public int page() {
        return this.page;
    }

    /**
     * Slots of other pages sit on the same coordinates as the visible ones and are hidden by
     * {@link #isActive()} alone; Slot.x and Slot.y are final, so nothing can move them aside. The
     * game filters on isActive for drawing, hovering and clicking, so this is safe on its own, but an
     * inventory sorter reading coordinates without checking isActive will see stacked slots. That is
     * what {@code DeepCrateMenu.isSlotOnCurrentPage} is public for.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isActive() {
        return this.visible;
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        // Slot's own version takes the smaller of the container limit and the item limit, which would
        // pull every insertion back down to 64. An item that does not stack at all keeps its one.
        return itemStack.getMaxStackSize() > 1 ? this.getMaxStackSize() : itemStack.getMaxStackSize();
    }

    @Override
    public Optional<ItemStack> tryRemove(int i, int j, Player player) {
        int hand = Math.min(CrateStorage.VANILLA_LIMIT, Math.max(1, this.getItem().getMaxStackSize()));
        return super.tryRemove(Math.min(i, hand), Math.min(j, hand), player);
    }
}
