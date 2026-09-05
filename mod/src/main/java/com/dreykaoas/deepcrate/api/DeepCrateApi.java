package com.dreykaoas.deepcrate.api;

import com.dreykaoas.deepcrate.DeepCrate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
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
    public static final boolean AUTOMATION_LIMITED = FabricLoader.getInstance().isModLoaded("lithium");

    private static final List<Runnable> TIER_LISTENERS = new ArrayList<>();
    private static final Map<Identifier, CrateTier> TIERS = new HashMap<>();
    private static final Map<Block, CrateTier> TIERS_BY_BLOCK = new HashMap<>();
    private static final List<CrateModule> MODULES = new ArrayList<>();
    private static final List<RowModule> ROW_MODULES = new ArrayList<>();
    private static final List<CrateModuleSlot> MODULE_SLOTS = new ArrayList<>();

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

        Registrations.addUnique(MODULES, crateModule, CrateModule::id, "Crate module");
        // Highest capacity first, so a stack matching two tags gets the better of the two.
        MODULES.sort((a, b) -> Integer.compare(b.capacity(), a.capacity()));
        DeepCrate.LOGGER.info("[DeepCrate] module {}: {} per slot", crateModule.id(), crateModule.capacity());
        return crateModule;
    }

    public static RowModule registerRowModule(RowModule rowModule) {
        Registrations.addUnique(ROW_MODULES, rowModule, RowModule::id, "Row module");
        DeepCrate.LOGGER.info("[DeepCrate] row module {}: {} rows each", rowModule.id(), rowModule.rows());
        return rowModule;
    }

    public static CrateModuleSlot registerModuleSlot(CrateModuleSlot crateModuleSlot) {
        Registrations.addUnique(MODULE_SLOTS, crateModuleSlot, CrateModuleSlot::id, "Module slot");
        MODULE_SLOTS.sort(Comparator.comparingInt(CrateModuleSlot::order));
        DeepCrate.LOGGER.info("[DeepCrate] module slot {}: up to {} at a time", crateModuleSlot.id(), crateModuleSlot.stackLimit());
        return crateModuleSlot;
    }

    public static List<CrateModuleSlot> moduleSlots() {
        return Collections.unmodifiableList(MODULE_SLOTS);
    }

    public static @Nullable CrateModuleSlot moduleSlot(Identifier identifier) {
        for (CrateModuleSlot crateModuleSlot : MODULE_SLOTS) {
            if (crateModuleSlot.id().equals(identifier)) {
                return crateModuleSlot;
            }
        }

        return null;
    }

    public static List<RowModule> rowModules() {
        return Collections.unmodifiableList(ROW_MODULES);
    }

    public static @Nullable RowModule rowModuleFor(ItemStack itemStack) {
        for (RowModule rowModule : ROW_MODULES) {
            if (rowModule.matches(itemStack)) {
                return rowModule;
            }
        }

        return null;
    }

    /** How many rows a stack sitting in the row slot adds, which is why the stack counts. */
    public static int rowsOf(ItemStack rowModuleStack) {
        RowModule rowModule = rowModuleFor(rowModuleStack);
        return rowModule == null ? 0 : rowModule.rows() * rowModuleStack.getCount();
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

    /**
     * The strongest capacity any of these stacks asks for. A crate whose cells hold two capacity
     * modules takes the better of the two rather than adding them, which is the rule one cell already
     * followed between two tags.
     */
    public static int capacityAmong(Iterable<ItemStack> stacks) {
        int capacity = BASE_CAPACITY;
        for (ItemStack itemStack : stacks) {
            capacity = Math.max(capacity, capacityOf(itemStack));
        }

        return capacity;
    }

    /** Rows add up, because each row module is a row and two of them are two rows. */
    public static int rowsAmong(Iterable<ItemStack> stacks) {
        int rows = 0;
        for (ItemStack itemStack : stacks) {
            rows += rowsOf(itemStack);
        }

        return rows;
    }

    public static CrateLayout layoutFor(CrateTier crateTier, int rows) {
        CrateLayout crateLayout = CrateLayoutCallback.EVENT.invoker().layout(crateTier, rows, CrateLayout.balanced(rows));
        if (crateLayout.rowsPerPage() * crateLayout.pageCount() >= rows) {
            return crateLayout;
        }

        // An addon may shape the pages, but not hide rows: a layout that does not cover the crate
        // would leave the slots past its end drawn nowhere and reachable by nothing.
        DeepCrate.LOGGER.warn(
            "[DeepCrate] a layout of {}x{} cannot hold the {} rows of {}, falling back to the balanced one",
            crateLayout.rowsPerPage(), crateLayout.pageCount(), rows, crateTier.id()
        );
        return CrateLayout.balanced(rows);
    }
}
