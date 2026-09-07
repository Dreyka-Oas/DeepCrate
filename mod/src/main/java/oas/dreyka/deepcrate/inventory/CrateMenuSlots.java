package oas.dreyka.deepcrate.inventory;

import oas.dreyka.deepcrate.api.CrateLayout;
import oas.dreyka.deepcrate.api.module.CrateModuleSlot;
import oas.dreyka.deepcrate.inventory.module.ModuleSlot;
import oas.dreyka.deepcrate.inventory.slot.DeepCrateSlot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

/**
 * The crate screen's own cells: the module row, the crate grid and the player's inventory, built
 * once when the screen opens, and which crate cells stay visible as the player flips pages
 * afterwards. Built around the menu's own reference, the same pattern {@code CrateModuleHolder} uses
 * for a block entity, reached only through the package-private forwarders DeepCrateMenu exposes for
 * addSlot and addStandardInventorySlots, both protected on AbstractContainerMenu, a different
 * package this class is not a subclass of.
 */
final class CrateMenuSlots {
    private final DeepCrateMenu menu;
    private final List<DeepCrateSlot> crateSlots = new ArrayList<>();
    private int page;

    CrateMenuSlots(DeepCrateMenu menu) {
        this.menu = menu;
    }

    void buildModuleSlots(Container moduleContainer, List<CrateModuleSlot> kinds) {
        for (int cell = 0; cell < kinds.size(); cell++) {
            this.menu.addOne(new ModuleSlot(
                moduleContainer,
                cell,
                CratePanelGeometry.MODULE_X,
                CratePanelGeometry.MODULE_Y + cell * CratePanelGeometry.MODULE_SPACING,
                kinds.get(cell)
            ));
        }
    }

    void buildCrateSlots(Container container, int columns, CrateLayout crateLayout, int crateLeft) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            int row = slot / columns;
            int column = slot % columns;
            DeepCrateSlot deepCrateSlot = new DeepCrateSlot(
                container,
                slot,
                crateLeft + column * CratePanelGeometry.CELL,
                CratePanelGeometry.GRID_TOP + row % crateLayout.rowsPerPage() * 18,
                row / crateLayout.rowsPerPage()
            );
            this.crateSlots.add(deepCrateSlot);
            this.menu.addOne(deepCrateSlot);
        }
    }

    void buildPlayerInventorySlots(Inventory inventory, int x, int y) {
        this.menu.addPlayerSlots(inventory, x, y);
    }

    int page() {
        return this.page;
    }

    /** Re-marks which crate cells are active for the given page: the "reconstruction" that follows the initial build. */
    void setPage(int page, int pageCount) {
        this.page = Math.floorMod(page, pageCount);

        for (DeepCrateSlot deepCrateSlot : this.crateSlots) {
            deepCrateSlot.setVisible(deepCrateSlot.page() == this.page);
        }
    }
}
