package oas.dreyka.deepcrate.api.registry;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.api.CrateTier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/** Crate tiers: registration and the two lookups an addon needs to find one back. */
public final class TierRegistry {
    private static final List<Runnable> TIER_LISTENERS = new ArrayList<>();
    private static final Map<Identifier, CrateTier> TIERS = new HashMap<>();
    private static final Map<Block, CrateTier> TIERS_BY_BLOCK = new HashMap<>();

    private TierRegistry() {}

    public static CrateTier registerTier(CrateTier crateTier) {
        CrateTier previous = TIERS.put(crateTier.id(), crateTier);
        if (previous != null) {
            throw new IllegalStateException("Crate tier " + crateTier.id() + " registered twice");
        }

        TIERS_BY_BLOCK.put(crateTier.block(), crateTier);
        // An addon's block joins the shared block entity type here rather than at build time; without
        // it the game refuses to attach a block entity to that block and the crate breaks on placement.
        for (Runnable listener : TIER_LISTENERS) {
            listener.run();
        }

        DeepCrate.LOGGER.info("[DeepCrate] tier {}: {} rows", crateTier.id(), crateTier.rows());
        return crateTier;
    }

    /** Called after every tier registration, including an addon's. */
    public static void onTierRegistered(Runnable runnable) {
        TIER_LISTENERS.add(runnable);
    }

    public static @Nullable CrateTier tier(Identifier identifier) {
        return TIERS.get(identifier);
    }

    public static @Nullable CrateTier tierOf(Block block) {
        return TIERS_BY_BLOCK.get(block);
    }

    public static List<CrateTier> tiers() {
        return List.copyOf(TIERS.values());
    }
}
