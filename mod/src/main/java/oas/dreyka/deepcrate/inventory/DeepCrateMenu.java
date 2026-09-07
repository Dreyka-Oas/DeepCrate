package oas.dreyka.deepcrate.inventory;

import oas.dreyka.deepcrate.api.CrateLayout;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.api.module.CrateModuleSlot;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.init.RegistryInit;
import oas.dreyka.deepcrate.inventory.menu.CrateMenuCleanup;
import oas.dreyka.deepcrate.inventory.menu.CrateModuleReaction;
import oas.dreyka.deepcrate.inventory.module.ModuleContainer;
import oas.dreyka.deepcrate.inventory.slot.DeepCrateSlot;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
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
    // Delegated to CratePanelGeometry: DeepCrateScreen and the gametests read these names under
    // DeepCrateMenu, so the constants stay here even though the measurements live elsewhere.
    public static final int PANEL_BORDER = CratePanelGeometry.PANEL_BORDER;
    public static final int CELL = CratePanelGeometry.CELL;
    public static final int MIN_PANEL_WIDTH = CratePanelGeometry.MIN_PANEL_WIDTH;
    public static final int COLUMNS_OF_A_PLAYER = CratePanelGeometry.COLUMNS_OF_A_PLAYER;
    public static final int GRID_LEFT = CratePanelGeometry.GRID_LEFT;
    public static final int GRID_TOP = CratePanelGeometry.GRID_TOP;
    public static final int MODULE_X = CratePanelGeometry.MODULE_X;
    public static final int MODULE_Y = CratePanelGeometry.MODULE_Y;
    public static final int MODULE_SPACING = CratePanelGeometry.MODULE_SPACING;

    private final Container crate;
    private final Container moduleContainer;
    private final CrateLayout layout;
    private final int columns;
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

    /**
     * Builds this menu's cells when it opens and keeps them lined up with the page currently shown.
     * Built around this menu's own reference, reached through the package-private forwarders below
     * for addSlot and addStandardInventorySlots, both protected on AbstractContainerMenu.
     */
    private final CrateMenuSlots menuSlots = new CrateMenuSlots(this);
    /**
     * Quick-moves a stack across this menu on a shift-click or a swap. Built around this menu's own
     * reference, reached through the package-private forwarder below for moveItemStackTo, protected
     * on AbstractContainerMenu.
     */
    private final CrateQuickMove quickMove = new CrateQuickMove(this);
    /**
     * Delegates for module-change reactions and end-of-screen cleanup, built around this menu's own
     * reference on the same pattern {@code CrateModuleHolder} uses for a block entity. Reached only
     * through the accessors below, because inventory.menu is a different package from this one.
     */
    private final CrateModuleReaction moduleReaction = new CrateModuleReaction(this);
    private final CrateMenuCleanup menuCleanup = new CrateMenuCleanup(this);

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
        this.menuSlots.buildModuleSlots(this.moduleContainer, kinds);

        int panelWidth = panelWidth(columns);
        int crateLeft = gridLeft(panelWidth, columns);
        this.menuSlots.buildCrateSlots(container, columns, crateLayout, crateLeft);

        // The player keeps nine columns whatever the crate is: their inventory is not the crate's.
        // Both grids are centred, so neither a wide crate nor a narrow one reads as lopsided.
        int playerLeft = gridLeft(panelWidth, COLUMNS_OF_A_PLAYER);
        this.menuSlots.buildPlayerInventorySlots(inventory, playerLeft, GRID_TOP + crateLayout.rowsPerPage() * 18 + 13);

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
        this.rowModuleCount = DeepCrateApi.rowsAmong(this.moduleReaction.moduleStacks());
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
        return CratePanelGeometry.panelWidth(columns);
    }

    public static int gridLeft(int panelWidth, int cells) {
        return CratePanelGeometry.gridLeft(panelWidth, cells);
    }

    public Container getContainer() {
        return this.crate;
    }

    public int page() {
        return this.menuSlots.page();
    }

    public int capacity() {
        return this.capacity;
    }

    /** Public because CrateModuleReaction and CrateMenuCleanup sit in the inventory.menu subpackage rather than in this class's own package. */
    public List<DeepCrateBlockEntity> crates() {
        return this.crates;
    }

    /** Public for the same reason as {@link #crates()}. */
    public Container moduleContainer() {
        return this.moduleContainer;
    }

    /** Public for the same reason as {@link #crates()}. */
    public Player player() {
        return this.player;
    }

    /** Public for the same reason as {@link #crates()}. */
    public int rowModuleCount() {
        return this.rowModuleCount;
    }

    public void setRowModuleCount(int rowModuleCount) {
        this.rowModuleCount = rowModuleCount;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setPage(int page) {
        this.menuSlots.setPage(page, this.layout.pageCount());
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
        return !(slot instanceof DeepCrateSlot deepCrateSlot) || deepCrateSlot.page() == this.menuSlots.page();
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
        return this.quickMove.quickMoveStack(player, i);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.menuCleanup.afterRemoved(player);
    }

    private void onModuleChanged() {
        this.moduleReaction.onModuleChanged();
    }

    // One-line forwarders to protected AbstractContainerMenu methods: CrateMenuSlots and
    // CrateQuickMove share this package but are not subclasses of AbstractContainerMenu, so protected
    // access does not carry over to them.
    void addOne(Slot slot) {
        this.addSlot(slot);
    }

    void addPlayerSlots(Inventory inventory, int x, int y) {
        this.addStandardInventorySlots(inventory, x, y);
    }

    boolean moveOne(ItemStack itemStack, int start, int end, boolean reverse) {
        return this.moveItemStackTo(itemStack, start, end, reverse);
    }

    int crateSlotCount() {
        return this.crateSlotCount;
    }
}
