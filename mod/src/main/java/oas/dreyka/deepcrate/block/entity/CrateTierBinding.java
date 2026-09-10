package oas.dreyka.deepcrate.block.entity;

import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.inventory.container.CrateStorage;

/** Keeps a crate's storage grown to the size its tier and row modules call for. */
public final class CrateTierBinding {
    private CrateTierBinding() {}

    /**
     * The tier a crate belongs to, read from its block rather than held in a field, so the block keeps
     * the one-argument constructor {@code simpleCodec} needs. A crate whose block was never registered
     * has no size, no capacity and no name to fall back on, so it throws rather than guessing.
     */
    public static CrateTier required(DeepCrateBlockEntity crate) {
        CrateTier crateTier = DeepCrateApi.tierOf(crate.getBlockState().getBlock());
        if (crateTier == null) {
            throw new IllegalStateException("No crate tier registered for " + crate.getBlockState().getBlock());
        }

        return crateTier;
    }

    /**
     * Grows a crate to the size its tier and its row modules call for. Not done once and cached: the
     * row count changes while the game runs, and at load time the block state is not known yet
     * because loadAdditional runs before the block entity is bound to a level.
     */
    public static void align(DeepCrateBlockEntity crate) {
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
