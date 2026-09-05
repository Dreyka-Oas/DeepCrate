package com.dreykaoas.deepcrate.inventory;

import com.dreykaoas.deepcrate.api.CrateLayout;
import com.dreykaoas.deepcrate.api.CrateModuleSlot;
import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.block.CrateDrops;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.dreykaoas.deepcrate.init.RegistryInit;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The crate screen: every slot of the crate exists from the start, but only the ones on the page
 * being shown are active.
 *
 * Paging is a client-side view and nothing more. The page number never reaches the server, so no
 * server-side index can be steered from outside, which is what would open the door to duplication.
 */
public class DeepCrateMenu extends AbstractContainerMenu {
    /** Border, cells, border: the 176 of a chest screen is 7 + 9 * 18 + 7. */
    public static final int PANEL_BORDER = 7;
    public static final int CELL = 18;
    /**
     * No panel is narrower than a chest's, whatever the crate is. The player's own nine columns are
     * drawn on it too, and they do not fit in less.
     */
    public static final int MIN_PANEL_WIDTH = 176;
    /** A player's inventory is nine wide, and a crate of another width does not change that. */
    public static final int COLUMNS_OF_A_PLAYER = 9;

    public static final int GRID_LEFT = 8;
    /** The chest's own grid position: the module lives outside the panel, so nothing is pushed down. */
    public static final int GRID_TOP = 18;
    /** Left of the panel, on its own tab, standing clear of it rather than glued to its edge. */
    public static final int MODULE_X = -25;
    public static final int MODULE_Y = 18;
    /** One cell under the next, eighteen pixels apart, as many as there are registered kinds. */
    public static final int MODULE_SPACING = 18;

    private final Container crate;
    private final Container moduleContainer;
    private final CrateLayout layout;
    private final int columns;
    private final List<DeepCrateSlot> crateSlots = new ArrayList<>();
    private final List<DeepCrateBlockEntity> crates;
    private final Player player;
    /**
     * How many crate slots this menu was built with. Read from the container each time, it would
     * follow a crate that grew under an open screen, and the slots past the old end are the player's
     * own inventory: a shift-click would then move a stack into a slot the menu draws elsewhere.
     */
    private final int crateSlotCount;
    /**
     * Where the crate's own slots begin, which is how many module cells were registered when this
     * menu was built. Read once: a mod loading a new cell while a screen is open must not move the
     * slots under it.
     */
    private final int crateSlotStart;

    private int page;
    private int capacity;
    private int rowModuleCount;

    /** Client side: everything comes from what the server sent when the crate opened. */
    public DeepCrateMenu(int i, Inventory inventory, CrateOpenData crateOpenData) {
        this(
            i,
            inventory,
            new CrateContainer(crateOpenData.slotCount(), crateOpenData.capacity()),
            List.of(),
            new CrateLayout(crateOpenData.rowsPerPage(), crateOpenData.pageCount()),
            crateOpenData.columns()
        );
    }

    public DeepCrateMenu(
        int i, Inventory inventory, Container container, List<DeepCrateBlockEntity> crates, CrateLayout crateLayout, int columns
    ) {
        super(RegistryInit.MENU, i);
        this.crate = container;
        this.crates = crates;
        this.layout = crateLayout;
        this.columns = columns;
        this.capacity = container.getMaxStackSize();
        this.crateSlotCount = container.getContainerSize();
        List<CrateModuleSlot> kinds = DeepCrateApi.moduleSlots();
        this.crateSlotStart = kinds.size();
        // Server side the slot reads the crate directly; client side there is no crate, so a plain
        // container stands in and only drives the predicted capacity.
        this.moduleContainer = crates.isEmpty()
            ? new SimpleContainer(kinds.size()) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    DeepCrateMenu.this.onModuleChanged();
                }
            }
            : new ModuleContainer(crates, this::onModuleChanged);

        container.startOpen(inventory.player);
        this.player = inventory.player;
        for (int cell = 0; cell < kinds.size(); cell++) {
            this.addSlot(new ModuleSlot(this.moduleContainer, cell, MODULE_X, MODULE_Y + cell * MODULE_SPACING, kinds.get(cell)));
        }

        int panelWidth = panelWidth(columns);
        int crateLeft = gridLeft(panelWidth, columns);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            int row = slot / columns;
            int column = slot % columns;
            DeepCrateSlot deepCrateSlot = new DeepCrateSlot(
                container,
                slot,
                crateLeft + column * CELL,
                GRID_TOP + row % crateLayout.rowsPerPage() * 18,
                row / crateLayout.rowsPerPage()
            );
            this.crateSlots.add(deepCrateSlot);
            this.addSlot(deepCrateSlot);
        }

        // The player keeps nine columns whatever the crate is: their inventory is not the crate's.
        // Both grids are centred, so neither a wide crate nor a narrow one reads as lopsided.
        int playerLeft = gridLeft(panelWidth, COLUMNS_OF_A_PLAYER);
        this.addStandardInventorySlots(inventory, playerLeft, GRID_TOP + crateLayout.rowsPerPage() * 18 + 13);

        // Another player inserting a module has to reach this screen too, and the opening payload is
        // only sent once. A data slot is the vanilla way of keeping one number in step.
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return DeepCrateMenu.this.capacity;
            }

            @Override
            public void set(int value) {
                DeepCrateMenu.this.capacity = value;
                if (DeepCrateMenu.this.crate instanceof CrateContainer crateContainer) {
                    crateContainer.setCapacity(value);
                }
            }
        });

        // Read before the first change comes through, otherwise the menu reopens itself the moment
        // anything else in it moves.
        this.rowModuleCount = DeepCrateApi.rowsAmong(this.moduleStacks());
        this.setPage(0);
    }

    public CrateLayout layout() {
        return this.layout;
    }

    public int crateSlotStart() {
        return this.crateSlotStart;
    }

    public int columns() {
        return this.columns;
    }

    public int panelWidth() {
        return panelWidth(this.columns);
    }

    public static int panelWidth(int columns) {
        return Math.max(MIN_PANEL_WIDTH, PANEL_BORDER * 2 + columns * CELL);
    }

    /**
     * Where the first slot of a run of {@code cells} columns sits, centred in the panel. The extra
     * pixel is the cell frame: a slot is drawn one in from the corner of its cell.
     */
    public static int gridLeft(int panelWidth, int cells) {
        return PANEL_BORDER + (panelWidth - PANEL_BORDER * 2 - cells * CELL) / 2 + 1;
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

    /**
     * Sends a shift-clicked module to its own slot. A capacity module only goes there while that slot
     * is free; row modules pile up to their stack limit. Anything left over is stored like any other
     * item rather than refused.
     */
    private boolean moveModuleToItsSlot(ItemStack itemStack) {
        for (int i = 0; i < this.crateSlotStart; i++) {
            Slot slot = this.getSlot(i);
            if (!slot.mayPlace(itemStack) || slot.getItem().getCount() >= slot.getMaxStackSize()) {
                continue;
            }

            if (this.moveItemStackTo(itemStack, i, i + 1, false)) {
                slot.setChanged();
                return true;
            }
        }

        return false;
    }

    /**
     * Puts the crate back in order, following the list the player's screen worked out. Client side
     * there is no crate to sort: the copy shown there is rewritten by the slot packets that follow.
     */
    public void sort(List<Item> order) {
        if (this.crates.isEmpty()) {
            return;
        }

        CrateSorter.arrange(this.crates.stream().map(DeepCrateBlockEntity::storage).toList(), order);
        for (DeepCrateBlockEntity deepCrateBlockEntity : this.crates) {
            deepCrateBlockEntity.setChanged();
        }

        this.broadcastChanges();
    }

    /** Whether a slot index belongs to the page currently shown. Read by inventory addons. */
    public boolean isSlotOnCurrentPage(int i) {
        Slot slot = this.slots.get(i);
        return !(slot instanceof DeepCrateSlot deepCrateSlot) || deepCrateSlot.page() == this.page;
    }

    /**
     * A number key or F would move a whole crate slot into one hotbar slot, where a count above the
     * item's own limit cannot legally live. Refused rather than silently truncated.
     */
    @Override
    public void clicked(int i, int j, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP && i >= this.crateSlotStart && i < this.crateSlotStart + this.crateSlotCount) {
            ItemStack itemStack = this.slots.get(i).getItem();
            if (itemStack.getCount() > itemStack.getMaxStackSize()) {
                return;
            }
        }

        super.clicked(i, j, clickType, player);
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
        int crateEnd = this.crateSlotStart + this.crateSlotCount;

        if (i < crateEnd) {
            // Out of the crate, one hand-sized stack per click; doClick loops for the rest.
            ItemStack itemStack3 = itemStack2.split(Math.min(itemStack2.getCount(), CrateStorage.VANILLA_LIMIT));
            boolean moved = this.moveItemStackTo(itemStack3, crateEnd, this.slots.size(), true);
            itemStack2.grow(itemStack3.getCount());
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else if (this.moveModuleToItsSlot(itemStack2)) {
            // Nothing else to do: the module found its own slot.
        } else if (!this.moveItemStackTo(itemStack2, this.crateSlotStart, crateEnd, false)) {
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
            if (anotherScreenIsOpen(deepCrateBlockEntity, player)) {
                continue;
            }

            List<ItemStack> spilled = new ArrayList<>(deepCrateBlockEntity.storage().overflow());
            // Row modules taken out shrink the crate the same way: what sat in the rows that are gone
            // goes to the ground rather than staying in a slot nobody can reach.
            spilled.addAll(deepCrateBlockEntity.trimToRows());
            if (spilled.isEmpty()) {
                continue;
            }

            for (ItemStack itemStack : spilled) {
                CrateDrops.dropWhole(player.level(), deepCrateBlockEntity.getBlockPos(), 1.0, itemStack);
            }

            deepCrateBlockEntity.setChanged();
        }
    }

    /**
     * Whether someone other than the player leaving still has this crate open. Cutting a crate back
     * under an open screen would leave that menu holding slots the crate no longer has, and its next
     * click would ask for an index past the end. The player closing is not counted: the game clears
     * their own menu only after this call returns.
     */
    private static boolean anotherScreenIsOpen(DeepCrateBlockEntity deepCrateBlockEntity, Player closing) {
        for (ContainerUser containerUser : deepCrateBlockEntity.getEntitiesWithContainerOpen()) {
            if (containerUser.getLivingEntity() != closing) {
                return true;
            }
        }

        return false;
    }

    private void onModuleChanged() {
        this.capacity = DeepCrateApi.capacityAmong(this.moduleStacks());
        this.reopenIfRowCountChanged();

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
    }

    /** The cells as a plain list, which is what the two api helpers walk. */
    private List<ItemStack> moduleStacks() {
        List<ItemStack> stacks = new ArrayList<>(this.moduleContainer.getContainerSize());
        for (int i = 0; i < this.moduleContainer.getContainerSize(); i++) {
            stacks.add(this.moduleContainer.getItem(i));
        }

        return stacks;
    }

    /**
     * A row module changes how many slots the screen has, and a menu's slot list is fixed once it is
     * built. So the crate is opened again, at the start of the next tick rather than inside the click
     * that caused it: closing a menu mid-click would put whatever the player is carrying on the
     * ground.
     */
    private void reopenIfRowCountChanged() {
        int rows = DeepCrateApi.rowsAmong(this.moduleStacks());
        if (rows == this.rowModuleCount) {
            return;
        }

        this.rowModuleCount = rows;
        if (this.crates.isEmpty() || !(this.player instanceof ServerPlayer serverPlayer) || !(this.player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        DeepCrateBlockEntity deepCrateBlockEntity = this.crates.get(0);
        serverLevel.getServer().execute(() -> {
            if (serverPlayer.containerMenu == this) {
                serverPlayer.openMenu(deepCrateBlockEntity);
            }
        });
    }
}
