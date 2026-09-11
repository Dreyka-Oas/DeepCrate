package oas.dreyka.deepcrate.inventory.menu;

import net.minecraft.world.inventory.DataSlot;
import oas.dreyka.deepcrate.inventory.container.CrateContainer;

/**
 * Keeps every screen on a crate in step on its capacity: another player inserting a module has to
 * reach this screen too, and the opening payload that would otherwise carry the number is only sent
 * once.
 */
final class CrateMenuCapacitySlot extends DataSlot {
    private final CrateMenuWiring wiring;

    CrateMenuCapacitySlot(CrateMenuWiring wiring) {
        this.wiring = wiring;
    }

    @Override
    public int get() {
        return this.wiring.menu().capacity();
    }

    @Override
    public void set(int value) {
        this.wiring.menu().setCapacity(value);
        if (this.wiring.crate() instanceof CrateContainer crateContainer) {
            crateContainer.setCapacity(value);
        }
    }
}
