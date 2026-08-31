package com.dreykaoas.deepcrate.inventory;

import com.dreykaoas.deepcrate.api.CrateLayout;
import com.dreykaoas.deepcrate.api.CrateTier;
import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.dreykaoas.deepcrate.init.RegistryInit;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The crate screen: every slot of the crate exists from the start, but only the ones on the page
 * being shown are active.
 *
 * Paging is a client-side view and nothing more. The page number never reaches the server, so no
 * server-side index can be steered from outside, which is what would open the door to duplication.
 */
public class DeepCrateMenu extends AbstractContainerMenu {
    public static final int GRID_LEFT = 8;
    public static final int GRID_TOP = 36;
    public static final int MODULE_X = 8;
    public static final int MODULE_Y = 18;

    private final Container crate;
    private final Container moduleContainer;
    private final CrateLayout layout;
    private final List<DeepCrateSlot> crateSlots = new ArrayList<>();
    private final List<DeepCrateBlockEntity> crates;

    private int page;
    private int capacity;

    /** Client side: everything comes from what the server sent when the crate opened. */
    public DeepCrateMenu(int i, Inventory inventory, CrateOpenData crateOpenData) {
        this(
            i,
            inventory,
            new CrateContainer(crateOpenData.slotCount(), crateOpenData.capacity()),
            List.of(),
            new CrateLayout(crateOpenData.rowsPerPage(), crateOpenData.pageCount())
        );
    }

    public DeepCrateMenu(int i, Inventory inventory, Container container, List<DeepCrateBlockEntity> crates, CrateLayout crateLayout) {
        super(RegistryInit.MENU, i);
        this.crate = container;
        this.crates = crates;
        this.layout = crateLayout;
        this.capacity = container.getMaxStackSize();
        this.moduleContainer = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                DeepCrateMenu.this.onModuleChanged();
            }
        };

        if (!crates.isEmpty()) {
            this.moduleContainer.setItem(0, crates.get(0).module());
        }

        container.startOpen(inventory.player);
        this.addSlot(new ModuleSlot(this.moduleContainer, 0, MODULE_X, MODULE_Y));

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            int row = slot / CrateTier.COLUMNS;
            int column = slot % CrateTier.COLUMNS;
            DeepCrateSlot deepCrateSlot = new DeepCrateSlot(
                container,
                slot,
                GRID_LEFT + column * 18,
                GRID_TOP + row % crateLayout.rowsPerPage() * 18,
                row / crateLayout.rowsPerPage()
            );
            this.crateSlots.add(deepCrateSlot);
            this.addSlot(deepCrateSlot);
        }

        this.addStandardInventorySlots(inventory, GRID_LEFT, GRID_TOP + crateLayout.rowsPerPage() * 18 + 13);
        this.setPage(0);
    }

    public CrateLayout layout() {
        return this.layout;
    }

    public Container getContainer() {
        return this.crate;
    }

    public int page() {
        return this.page;
    }

    public int capacity() {
        return this.capacity;
    }

    public void setPage(int page) {
        this.page = Math.floorMod(page, this.layout.pageCount());

        for (DeepCrateSlot deepCrateSlot : this.crateSlots) {
            deepCrateSlot.setVisible(deepCrateSlot.page() == this.page);
        }
    }

    /** Whether a slot index belongs to the page currently shown. Read by inventory addons. */
    public boolean isSlotOnCurrentPage(int i) {
        Slot slot = this.slots.get(i);
        return !(slot instanceof DeepCrateSlot deepCrateSlot) || deepCrateSlot.page() == this.page;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.crate.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot == null || !slot.hasItem()) {
            return itemStack;
        }

        ItemStack itemStack2 = slot.getItem();
        itemStack = itemStack2.copy();
        int crateEnd = 1 + this.crate.getContainerSize();

        if (i < crateEnd) {
            // Out of the crate, one hand-sized stack per click; doClick loops for the rest.
            ItemStack itemStack3 = itemStack2.split(Math.min(itemStack2.getCount(), CrateStorage.VANILLA_LIMIT));
            boolean moved = this.moveItemStackTo(itemStack3, crateEnd, this.slots.size(), true);
            itemStack2.grow(itemStack3.getCount());
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else if (this.getSlot(0).mayPlace(itemStack2)) {
            if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(itemStack2, 1, crateEnd, false)) {
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.crate.stopOpen(player);

        // A module pulled out leaves slots above the new capacity; the excess goes to the ground, as
        // asked. Server side only: the client copy would drop a second set of ghosts.
        if (player.level().isClientSide()) {
            return;
        }

        for (DeepCrateBlockEntity deepCrateBlockEntity : this.crates) {
            List<ItemStack> spilled = deepCrateBlockEntity.storage().overflow();
            if (spilled.isEmpty()) {
                continue;
            }

            for (ItemStack itemStack : spilled) {
                Containers.dropItemStack(
                    player.level(),
                    deepCrateBlockEntity.getBlockPos().getX(),
                    deepCrateBlockEntity.getBlockPos().getY() + 1,
                    deepCrateBlockEntity.getBlockPos().getZ(),
                    itemStack
                );
            }

            deepCrateBlockEntity.setChanged();
        }
    }

    private void onModuleChanged() {
        ItemStack itemStack = this.moduleContainer.getItem(0);
        this.capacity = DeepCrateApi.capacityOf(itemStack);

        if (this.crates.isEmpty()) {
            if (this.crate instanceof CrateContainer crateContainer) {
                crateContainer.setCapacity(this.capacity);
            }

            return;
        }

        // Both halves follow the one module, so a hopper reaching the far half sees the same limit.
        for (DeepCrateBlockEntity deepCrateBlockEntity : this.crates) {
            deepCrateBlockEntity.storage().setCapacity(this.capacity);
        }

        this.crates.get(0).setModule(itemStack);
    }
}
