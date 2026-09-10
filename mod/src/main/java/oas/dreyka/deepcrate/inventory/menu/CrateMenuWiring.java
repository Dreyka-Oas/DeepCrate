package oas.dreyka.deepcrate.inventory.menu;

import oas.dreyka.deepcrate.api.CrateLayout;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.api.module.CrateModuleSlot;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.inventory.menu.CrateModuleReaction;
import oas.dreyka.deepcrate.inventory.module.ModuleContainer;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import oas.dreyka.deepcrate.inventory.slot.CratePanelGeometry;
import oas.dreyka.deepcrate.inventory.container.CrateContainer;

/**
 * What a crate menu opens onto, and the wiring done once when it does: the module cells it watches,
 * the cells it draws, and the data slot that keeps its capacity in step with the other screens on the
 * same crate. Built around the menu's own reference, the same pattern {@code CrateModuleHolder} uses
 * for a block entity, reached through the package-private forwarder DeepCrateMenu exposes for
 * addDataSlot, protected on AbstractContainerMenu, a different package this class is not a subclass
 * of.
 */
final class CrateMenuWiring {
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
            ? new SimpleContainer(this.kinds.size()) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    menu.onModuleChanged();
                }
            }
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

        int panelWidth = CratePanelGeometry.panelWidth(this.columns);
        menuSlots.buildCrateSlots(this.crate, this.columns, this.layout, CratePanelGeometry.gridLeft(panelWidth, this.columns));

        // The player keeps nine columns whatever the crate is: their inventory is not the crate's.
        // Both grids are centred, so neither a wide crate nor a narrow one reads as lopsided.
        int playerLeft = CratePanelGeometry.gridLeft(panelWidth, CratePanelGeometry.COLUMNS_OF_A_PLAYER);
        menuSlots.buildPlayerInventorySlots(inventory, playerLeft, CratePanelGeometry.GRID_TOP + this.layout.rowsPerPage() * 18 + 13);

        // Another player inserting a module has to reach this screen too, and the opening payload is
        // only sent once. A data slot is the vanilla way of keeping one number in step.
        this.menu.addOneDataSlot(this.capacitySlot());

        // Read before the first change comes through, otherwise the menu reopens itself the moment
        // anything else in it moves.
        this.menu.setRowModuleCount(DeepCrateApi.rowsAmong(moduleReaction.moduleStacks()));
        this.menu.setPage(0);
    }

    private DataSlot capacitySlot() {
        return new DataSlot() {
            @Override
            public int get() {
                return CrateMenuWiring.this.menu.capacity();
            }

            @Override
            public void set(int value) {
                CrateMenuWiring.this.menu.setCapacity(value);
                if (CrateMenuWiring.this.crate instanceof CrateContainer crateContainer) {
                    crateContainer.setCapacity(value);
                }
            }
        };
    }

    Container crate() {
        return this.crate;
    }

    Container moduleContainer() {
        return this.moduleContainer;
    }

    List<DeepCrateBlockEntity> crates() {
        return this.crates;
    }

    CrateLayout layout() {
        return this.layout;
    }

    int columns() {
        return this.columns;
    }

    Player player() {
        return this.player;
    }

    int crateSlotCount() {
        return this.crateSlotCount;
    }

    int crateSlotStart() {
        return this.crateSlotStart;
    }
}
