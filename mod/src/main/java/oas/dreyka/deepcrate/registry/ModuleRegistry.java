package oas.dreyka.deepcrate.registry;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.api.module.CrateModule;
import oas.dreyka.deepcrate.config.domain.CrateConfig;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Capacity modules: registration, the ceiling they may not cross, and matching a stack to one. */
public final class ModuleRegistry {
    /**
     * Ceiling on what a module may raise a slot to.
     *
     * Counts themselves travel as variable-length integers, but the menu tells the client its current
     * capacity through a data slot, and that packet writes a short.
     */
    public static final int MAX_CAPACITY = Short.MAX_VALUE;

    private static final ModuleList<CrateModule> MODULES = new ModuleList<>(CrateModule::id, CrateModule::matches, "Crate module");

    private ModuleRegistry() {}

    public static CrateModule registerModule(CrateModule crateModule) {
        if (crateModule.capacity() > MAX_CAPACITY) {
            throw new IllegalArgumentException(
                "Crate module " + crateModule.id() + " asks for " + crateModule.capacity() + ", above the " + MAX_CAPACITY + " ceiling"
            );
        }

        // Highest capacity first, so a stack matching two tags gets the better of the two.
        CrateModule registered = MODULES.register(crateModule, (a, b) -> Integer.compare(b.capacity(), a.capacity()));
        DeepCrate.LOGGER.info("[DeepCrate] module {}: {} per slot", crateModule.id(), crateModule.capacity());
        return registered;
    }

    public static List<CrateModule> modules() {
        return MODULES.all();
    }

    public static @Nullable CrateModule moduleFor(ItemStack itemStack) {
        return MODULES.find(itemStack);
    }

    /** What one slot holds, given whatever sits in the module slot. */
    public static int capacityOf(ItemStack moduleStack) {
        CrateModule crateModule = moduleFor(moduleStack);
        return crateModule == null ? CrateConfig.baseCapacity : crateModule.capacity();
    }

    /**
     * The strongest capacity any of these stacks asks for. A crate whose cells hold two capacity
     * modules takes the better of the two; it does not add them, the same rule one cell already
     * followed between two tags.
     */
    public static int capacityAmong(Iterable<ItemStack> stacks) {
        return MODULES.among(stacks, CrateConfig.baseCapacity, ModuleRegistry::capacityOf, Math::max);
    }
}
