package oas.dreyka.deepcrate.inventory.menu;

import net.minecraft.world.SimpleContainer;

/**
 * The client-side stand-in for a crate's module cells: there is no crate to read on the client, so
 * this only drives the predicted capacity, telling the menu when a module cell changes.
 */
final class CrateMenuModuleContainer extends SimpleContainer {
    private final DeepCrateMenu menu;

    CrateMenuModuleContainer(int size, DeepCrateMenu menu) {
        super(size);
        this.menu = menu;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.menu.onModuleChanged();
    }
}
