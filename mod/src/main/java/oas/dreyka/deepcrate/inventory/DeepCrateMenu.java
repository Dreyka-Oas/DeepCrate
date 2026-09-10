package oas.dreyka.deepcrate.inventory;

import oas.dreyka.deepcrate.api.CrateLayout;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.init.RegistryInit;
import oas.dreyka.deepcrate.inventory.menu.CrateMenuCleanup;
import oas.dreyka.deepcrate.inventory.menu.CrateModuleReaction;
import oas.dreyka.deepcrate.inventory.menu.CrateSorting;
import java.util.List;
import net.minecraft.world.Container;
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
    /**
     * The halves of this menu, each built around its own reference, the same pattern
     * {@code CrateModuleHolder} uses for a block entity. The two in the inventory.menu subpackage go
     * through the public accessors below; the two sharing this package go through the package-private
     * forwarders at the end of the class.
     */
    private final CrateMenuSlots menuSlots = new CrateMenuSlots(this);
    private final CrateQuickMove quickMove = new CrateQuickMove(this);
    private final CrateModuleReaction moduleReaction = new CrateModuleReaction(this);
    private final CrateMenuCleanup menuCleanup = new CrateMenuCleanup(this);
    private final CrateMenuWiring wiring;

    private int capacity;
    private int rowModuleCount;

    /** Client side: everything comes from what the server sent when the crate opened. */
    public DeepCrateMenu(int i, Inventory inventory, CrateOpenData crateOpenData) {
        this(
            i, inventory, CrateMenuWiring.clientCrate(crateOpenData), List.of(), CrateMenuWiring.clientLayout(crateOpenData),
            crateOpenData.columns()
        );
    }

    public DeepCrateMenu(
        int i, Inventory inventory, Container container, List<DeepCrateBlockEntity> crates, CrateLayout crateLayout, int columns
    ) {
        super(RegistryInit.MENU, i);
        this.capacity = container.getMaxStackSize();
        this.wiring = new CrateMenuWiring(this, inventory, container, crates, crateLayout, columns);
        this.wiring.open(inventory, this.menuSlots, this.moduleReaction);
    }

    public CrateLayout layout() {
        return this.wiring.layout();
    }

    public int crateSlotStart() {
        return this.wiring.crateSlotStart();
    }

    public int columns() {
        return this.wiring.columns();
    }

    public int panelWidth() {
        return CratePanelGeometry.panelWidth(this.wiring.columns());
    }

    public Container getContainer() {
        return this.wiring.crate();
    }

    public int page() {
        return this.menuSlots.page();
    }

    public int capacity() {
        return this.capacity;
    }

    /** Public because the classes under inventory.menu sit in a subpackage rather than in this class's own package. */
    public List<DeepCrateBlockEntity> crates() {
        return this.wiring.crates();
    }

    /** Public for the same reason as {@link #crates()}. */
    public Container moduleContainer() {
        return this.wiring.moduleContainer();
    }

    /** Public for the same reason as {@link #crates()}. */
    public Player player() {
        return this.wiring.player();
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
        this.menuSlots.setPage(page, this.wiring.layout().pageCount());
    }

    /** Puts the crate back in order, following the list the player's screen worked out. */
    public void sort(List<Item> order) {
        CrateSorting.sort(this, order);
    }

    /** Whether a slot index belongs to the page currently shown. Read by inventory addons. */
    public boolean isSlotOnCurrentPage(int i) {
        return this.menuSlots.isOnCurrentPage(this.slots.get(i));
    }

    @Override
    public void clicked(int i, int j, ClickType clickType, Player player) {
        if (this.quickMove.refusesSwap(i, clickType)) {
            return;
        }

        super.clicked(i, j, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.wiring.crate().stillValid(player);
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

    void onModuleChanged() {
        this.moduleReaction.onModuleChanged();
    }

    // One-line forwarders to protected AbstractContainerMenu methods: the classes sharing this
    // package are not subclasses of it, so protected access does not carry over to them.
    void addOne(Slot slot) {
        this.addSlot(slot);
    }

    void addPlayerSlots(Inventory inventory, int x, int y) {
        this.addStandardInventorySlots(inventory, x, y);
    }

    void addOneDataSlot(DataSlot dataSlot) {
        this.addDataSlot(dataSlot);
    }

    boolean moveOne(ItemStack itemStack, int start, int end, boolean reverse) {
        return this.moveItemStackTo(itemStack, start, end, reverse);
    }

    int crateSlotCount() {
        return this.wiring.crateSlotCount();
    }
}
