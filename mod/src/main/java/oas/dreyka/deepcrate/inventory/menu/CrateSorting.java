package oas.dreyka.deepcrate.inventory.menu;

import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.inventory.CrateSorter;
import oas.dreyka.deepcrate.inventory.DeepCrateMenu;
import java.util.List;
import net.minecraft.world.item.Item;

/**
 * Putting the crates a menu opened onto back in order, and telling every screen on them about it.
 * Reached only through the accessors DeepCrateMenu exposes, because this class sits in the
 * inventory.menu subpackage rather than in inventory.
 */
public final class CrateSorting {
    private CrateSorting() {}

    /**
     * Client side there is no crate to sort: the copy shown there is rewritten by the slot packets
     * that follow.
     */
    public static void sort(DeepCrateMenu menu, List<Item> order) {
        if (menu.crates().isEmpty()) {
            return;
        }

        CrateSorter.arrange(menu.crates().stream().map(DeepCrateBlockEntity::storage).toList(), order);
        for (DeepCrateBlockEntity deepCrateBlockEntity : menu.crates()) {
            deepCrateBlockEntity.setChanged();
        }

        menu.broadcastChanges();
    }
}
