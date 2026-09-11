package oas.dreyka.deepcrate.inventory.menu;

import oas.dreyka.deepcrate.api.CrateLayout;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.api.module.CrateModuleSlot;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.inventory.module.ModuleContainer;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import oas.dreyka.deepcrate.inventory.slot.CratePanelGeometry;
import oas.dreyka.deepcrate.inventory.container.CrateContainer;
import oas.dreyka.deepcrate.inventory.menu.reaction.CrateModuleReaction;

/**
 * What a crate menu opens onto, and the wiring done once when it does: the module cells it watches,
 * the cells it draws, and the data slot that keeps its capacity in step with the other screens on the
 * same crate. Built around the menu's own reference, the same pattern {@code CrateModuleHolder} uses
 * for a block entity. Public because a menu's own public accessors forward to this class rather than
 * duplicate it.
 */
public final class CrateMenuWiring {
    private final DeepCrateMenu menu;
    private final List<CrateModuleSlot> kinds = DeepCrateApi.moduleSlots();
    private final Container crate;
    private final Container moduleContainer;
    private final List<DeepCrateBlockEntity> crates;
    private final CrateLayout layout;
    private final int columns;
    private final Player player;
    /**
     * How many crate slots the menu was built with. Read from the container each time, it would
     * follow a crate that grew under an open screen, and the slots past the old end are the player's
     * own inventory: a shift-click would then move a stack into a slot the menu draws elsewhere.
     */
    private final int crateSlotCount;
    /**
     * Where the crate's own slots begin, which is how many module cells were registered when the menu
     * was built. Read once: a mod loading a new cell while a screen is open must not move the slots
     * under it.
     */
    private final int crateSlotStart;

    CrateMenuWiring(
        DeepCrateMenu menu, Inventory inventory, Container container, List<DeepCrateBlockEntity> crates, CrateLayout crateLayout, int columns
    ) {
        this.menu = menu;
        this.crate = container;
        this.crates = crates;
        this.layout = crateLayout;
        this.columns = columns;
        this.player = inventory.player;
        this.crateSlotCount = container.getContainerSize();
        this.crateSlotStart = this.kinds.size();
        // Server side the slot reads the crate directly; client side there is no crate, so a plain
        // container stands in and only drives the predicted capacity.
        this.moduleContainer = crates.isEmpty()
            ? new CrateMenuModuleContainer(this.kinds.size(), menu)
            : new ModuleContainer(crates, menu::onModuleChanged);
    }

    static CrateContainer clientCrate(CrateOpenData crateOpenData) {
        return new CrateContainer(crateOpenData.slotCount(), crateOpenData.capacity());
    }

    static CrateLayout clientLayout(CrateOpenData crateOpenData) {
        return new CrateLayout(crateOpenData.rowsPerPage(), crateOpenData.pageCount());
    }

    void open(Inventory inventory, CrateMenuSlots menuSlots, CrateModuleReaction moduleReaction) {
        this.crate.startOpen(inventory.player);
        menuSlots.buildModuleSlots(this.moduleContainer, this.kinds);
        menuSlots.buildCrateSlots(this.crate, this.columns, this.layout, CratePanelGeometry.gridLeft(this.panelWidth(), this.columns));

        // The player keeps nine columns whatever the crate is: their inventory is not the crate's.
        // Both grids are centred, so neither a wide crate nor a narrow one reads as lopsided.
        int playerLeft = CratePanelGeometry.gridLeft(this.panelWidth(), CratePanelGeometry.COLUMNS_OF_A_PLAYER);
        menuSlots.buildPlayerInventorySlots(inventory, playerLeft, CratePanelGeometry.GRID_TOP + this.layout.rowsPerPage() * 18 + 13);

        // Another player inserting a module has to reach this screen too, and the opening payload is
        // only sent once. A data slot is the vanilla way of keeping one number in step.
        this.menu.addOneDataSlot(new CrateMenuCapacitySlot(this));

        // Read before the first change comes through, otherwise the menu reopens itself the moment
        // anything else in it moves.
        this.menu.setRowModuleCount(DeepCrateApi.rowsAmong(moduleReaction.moduleStacks()));
        this.menu.setPage(0);
    }

    DeepCrateMenu menu() {
        return this.menu;
    }

    public Container crate() {
        return this.crate;
    }

    public Container moduleContainer() {
        return this.moduleContainer;
    }

    public List<DeepCrateBlockEntity> crates() {
        return this.crates;
    }

    public CrateLayout layout() {
        return this.layout;
    }

    public int columns() {
        return this.columns;
    }

    public int panelWidth() {
        return CratePanelGeometry.panelWidth(this.columns);
    }

    public Player player() {
        return this.player;
    }

    public int crateSlotCount() {
        return this.crateSlotCount;
    }

    public int crateSlotStart() {
        return this.crateSlotStart;
    }
}
