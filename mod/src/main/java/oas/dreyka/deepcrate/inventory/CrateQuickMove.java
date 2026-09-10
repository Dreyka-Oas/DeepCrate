package oas.dreyka.deepcrate.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Moving a stack across the crate screen with a shift-click or a number-key swap: a module heads to
 * its own cell first, everything else crosses to whichever side the stack did not start on. Built
 * around the menu's own reference, the same pattern {@code CrateModuleHolder} uses for a block
 * entity, reached only through the package-private forwarder DeepCrateMenu exposes for
 * moveItemStackTo, protected on AbstractContainerMenu, a different package this class is not a
 * subclass of.
 */
final class CrateQuickMove {
    private final DeepCrateMenu menu;

    CrateQuickMove(DeepCrateMenu menu) {
        this.menu = menu;
    }

    /**
     * Whether a swap out of a crate slot has to be turned away: a number key or F would move a whole
     * crate slot into one hotbar slot, where a count above the item's own limit cannot legally live.
     */
    boolean refusesSwap(int i, ClickType clickType) {
        int crateStart = this.menu.crateSlotStart();
        if (clickType != ClickType.SWAP || i < crateStart || i >= crateStart + this.menu.crateSlotCount()) {
            return false;
        }

        ItemStack itemStack = this.menu.slots.get(i).getItem();
        return itemStack.getCount() > itemStack.getMaxStackSize();
    }

    ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.menu.slots.get(i);
        if (slot == null || !slot.hasItem()) {
            return itemStack;
        }

        ItemStack itemStack2 = slot.getItem();
        itemStack = itemStack2.copy();
        int crateEnd = this.menu.crateSlotStart() + this.menu.crateSlotCount();

        if (i < crateEnd) {
            // Out of the crate, one hand-sized stack per click; doClick loops for the rest.
            ItemStack itemStack3 = itemStack2.split(Math.min(itemStack2.getCount(), CrateStorage.VANILLA_LIMIT));
            boolean moved = this.menu.moveOne(itemStack3, crateEnd, this.menu.slots.size(), true);
            itemStack2.grow(itemStack3.getCount());
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else if (this.moveModuleToItsSlot(itemStack2)) {
            // Nothing else to do: the module found its own slot.
        } else if (!this.menu.moveOne(itemStack2, this.menu.crateSlotStart(), crateEnd, false)) {
            // Deliberately every crate slot, not only the visible page: a player shift-clicking a
            // stack expects it stored, not refused because the right page is not open.
            return ItemStack.EMPTY;
        }

        if (itemStack2.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return itemStack;
    }

    /**
     * Sends a shift-clicked module to its own slot. A capacity module only goes there while that slot
     * is free; row modules pile up to their stack limit. Anything left over is stored like any other
     * item rather than refused.
     */
    private boolean moveModuleToItsSlot(ItemStack itemStack) {
        for (int i = 0; i < this.menu.crateSlotStart(); i++) {
            Slot slot = this.menu.getSlot(i);
            if (!slot.mayPlace(itemStack) || slot.getItem().getCount() >= slot.getMaxStackSize()) {
                continue;
            }

            if (this.menu.moveOne(itemStack, i, i + 1, false)) {
                slot.setChanged();
                return true;
            }
        }

        return false;
    }
}
