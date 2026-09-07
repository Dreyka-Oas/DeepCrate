package oas.dreyka.deepcrate.block;

import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.inventory.CrateStorage;

/** Keeps a crate's storage grown to the size its tier and row modules call for. */
final class CrateTierBinding {
    private CrateTierBinding() {}

    /**
     * Grows a crate to the size its tier and its row modules call for. Not done once and cached: the
     * row count changes while the game runs, and at load time the block state is not known yet
     * because loadAdditional runs before the block entity is bound to a level.
     */
    static void align(DeepCrateBlockEntity crate) {
        CrateTier crateTier = DeepCrateApi.tierOf(crate.getBlockState().getBlock());
        if (crateTier == null) {
            return;
        }

        CrateStorage storage = crate.unalignedStorage();
        storage.setTier(crateTier);
        int target = crateTier.slotCount() + crate.extraRows() * crateTier.columns();
        if (target > storage.size()) {
            storage.grow(target);
        }
    }
}
