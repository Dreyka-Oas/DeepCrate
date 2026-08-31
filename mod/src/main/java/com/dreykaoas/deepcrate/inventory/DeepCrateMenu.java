package com.dreykaoas.deepcrate.inventory;

import com.dreykaoas.deepcrate.init.RegistryInit;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class DeepCrateMenu extends AbstractContainerMenu {
    private static final int ROWS = 3;

    private final Container crate;

    public DeepCrateMenu(int i, Inventory inventory) {
        this(i, inventory, new CrateContainer());
    }

    public DeepCrateMenu(int i, Inventory inventory, Container container) {
        super(RegistryInit.MENU, i);
        checkContainerSize(container, CrateStorage.SLOT_COUNT);
        this.crate = container;
        container.startOpen(inventory.player);

        for (int j = 0; j < ROWS; j++) {
            for (int k = 0; k < 9; k++) {
                this.addSlot(new DeepCrateSlot(container, k + j * 9, 8 + k * 18, 18 + j * 18));
            }
        }

        this.addStandardInventorySlots(inventory, 8, 18 + ROWS * 18 + 13);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.crate.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        var slot = this.slots.get(i);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (i < CrateStorage.SLOT_COUNT) {
                // A crate slot can hold more than the hand it feeds, so hand out one vanilla stack per
                // click and let doClick's loop come back for the rest.
                ItemStack itemStack3 = itemStack2.split(Math.min(itemStack2.getCount(), CrateStorage.VANILLA_LIMIT));
                boolean bl = this.moveItemStackTo(itemStack3, CrateStorage.SLOT_COUNT, this.slots.size(), true);
                itemStack2.grow(itemStack3.getCount());
                if (!bl) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 0, CrateStorage.SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.crate.stopOpen(player);
    }

    public Container getContainer() {
        return this.crate;
    }
}
