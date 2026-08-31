package com.dreykaoas.deepcrate.api;

import com.dreykaoas.deepcrate.DeepCrate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/** What a crate tier is, what a module is, and how to look either one up. */
public final class DeepCrateApi {
    /** A slot with no module in the crate, which is what the rest of the game holds. */
    public static final int BASE_CAPACITY = 64;

    /**
     * Ceiling on what a module may raise a slot to.
     *
     * Counts themselves travel as variable-length integers, but the menu tells the client its current
     * capacity through a data slot, and that packet writes a short.
     */
    public static final int MAX_CAPACITY = Short.MAX_VALUE;

    /**
     * Whether automation may go past a vanilla stack.
     *
     * Lithium replaces the hopper wholesale and keeps its own copy of the target inventory. Against a
     * container that answers more than 64 it takes items out of the hopper and never writes them in:
     * measured, eight blocks of dirt destroyed per run. Telling automation 64 in that case costs the
     * feature and keeps the items; the player's own hands are unaffected, they go through the menu.
     */
    public static final boolean AUTOMATION_LIMITED = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("lithium");

    private static final List<Runnable> TIER_LISTENERS = new ArrayList<>();
    private static final Map<Identifier, CrateTier> TIERS = new HashMap<>();
    private static final Map<Block, CrateTier> TIERS_BY_BLOCK = new HashMap<>();
    private static final List<CrateModule> MODULES = new ArrayList<>();

    private DeepCrateApi() {}

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

    public static CrateModule registerModule(CrateModule crateModule) {
        if (crateModule.capacity() > MAX_CAPACITY) {
            throw new IllegalArgumentException(
                "Crate module " + crateModule.id() + " asks for " + crateModule.capacity() + ", above the " + MAX_CAPACITY + " ceiling"
            );
        }

        for (CrateModule existing : MODULES) {
            if (existing.id().equals(crateModule.id())) {
                throw new IllegalStateException("Crate module " + crateModule.id() + " registered twice");
            }
        }

        MODULES.add(crateModule);
        // Highest capacity first, so a stack matching two tags gets the better of the two.
        MODULES.sort((a, b) -> Integer.compare(b.capacity(), a.capacity()));
        DeepCrate.LOGGER.info("[DeepCrate] module {}: {} per slot", crateModule.id(), crateModule.capacity());
        return crateModule;
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

    public static List<CrateModule> modules() {
        return Collections.unmodifiableList(MODULES);
    }

    public static @Nullable CrateModule moduleFor(ItemStack itemStack) {
        for (CrateModule crateModule : MODULES) {
            if (crateModule.matches(itemStack)) {
                return crateModule;
            }
        }

        return null;
    }

    /** What one slot holds, given whatever sits in the module slot. */
    public static int capacityOf(ItemStack moduleStack) {
        CrateModule crateModule = moduleFor(moduleStack);
        return crateModule == null ? BASE_CAPACITY : crateModule.capacity();
    }

    public static CrateLayout layoutFor(CrateTier crateTier, int rows) {
        return CrateLayoutCallback.EVENT.invoker().layout(crateTier, rows, CrateLayout.balanced(rows));
    }
}
