package oas.dreyka.deepcrate.api;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.api.module.CrateModule;
import oas.dreyka.deepcrate.api.module.CrateModuleSlot;
import oas.dreyka.deepcrate.api.module.RowModule;
import oas.dreyka.deepcrate.api.registry.ModuleRegistry;
import oas.dreyka.deepcrate.api.registry.ModuleSlotRegistry;
import oas.dreyka.deepcrate.api.registry.RowModuleRegistry;
import oas.dreyka.deepcrate.api.registry.TierRegistry;
import oas.dreyka.deepcrate.config.domain.CrateConfig;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * What a crate tier is, what a module is, and how to look either one up. A facade over the four
 * registries below: an addon that already calls a method here keeps calling the same one, on the
 * same class, with the same signature.
 */
public final class DeepCrateApi {
    public static final int MAX_CAPACITY = ModuleRegistry.MAX_CAPACITY;

    /** The mod list is closed before an entry point runs and never moves again, so this one is decided once. */
    private static final boolean LITHIUM_PRESENT = FabricLoader.getInstance().isModLoaded("lithium");

    private DeepCrateApi() {}

    /**
     * Whether automation may go past a vanilla stack, which it may not while lithium is installed and
     * {@code CrateConfig.limitAutomationWithLithium} is on. That field carries the measurement behind
     * the rule, and reading it at the call is what leaves nothing to refresh after the file is read.
     */
    public static boolean automationLimited() {
        return CrateConfig.limitAutomationWithLithium && LITHIUM_PRESENT;
    }

    public static CrateTier registerTier(CrateTier crateTier) {
        return TierRegistry.registerTier(crateTier);
    }

    public static CrateModule registerModule(CrateModule crateModule) {
        return ModuleRegistry.registerModule(crateModule);
    }

    public static RowModule registerRowModule(RowModule rowModule) {
        return RowModuleRegistry.registerRowModule(rowModule);
    }

    public static CrateModuleSlot registerModuleSlot(CrateModuleSlot crateModuleSlot) {
        return ModuleSlotRegistry.registerModuleSlot(crateModuleSlot);
    }

    public static List<CrateModuleSlot> moduleSlots() {
        return ModuleSlotRegistry.moduleSlots();
    }

    public static @Nullable CrateModuleSlot moduleSlot(Identifier identifier) {
        return ModuleSlotRegistry.moduleSlot(identifier);
    }

    public static List<RowModule> rowModules() {
        return RowModuleRegistry.rowModules();
    }

    public static @Nullable RowModule rowModuleFor(ItemStack itemStack) {
        return RowModuleRegistry.rowModuleFor(itemStack);
    }

    public static int rowsOf(ItemStack rowModuleStack) {
        return RowModuleRegistry.rowsOf(rowModuleStack);
    }

    /** Called after every tier registration, including an addon's. */
    public static void onTierRegistered(Runnable runnable) {
        TierRegistry.onTierRegistered(runnable);
    }

    public static @Nullable CrateTier tier(Identifier identifier) {
        return TierRegistry.tier(identifier);
    }

    public static @Nullable CrateTier tierOf(Block block) {
        return TierRegistry.tierOf(block);
    }

    public static List<CrateTier> tiers() {
        return TierRegistry.tiers();
    }

    public static List<CrateModule> modules() {
        return ModuleRegistry.modules();
    }

    public static @Nullable CrateModule moduleFor(ItemStack itemStack) {
        return ModuleRegistry.moduleFor(itemStack);
    }

    public static int capacityOf(ItemStack moduleStack) {
        return ModuleRegistry.capacityOf(moduleStack);
    }

    public static int capacityAmong(Iterable<ItemStack> stacks) {
        return ModuleRegistry.capacityAmong(stacks);
    }

    public static int rowsAmong(Iterable<ItemStack> stacks) {
        return RowModuleRegistry.rowsAmong(stacks);
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
