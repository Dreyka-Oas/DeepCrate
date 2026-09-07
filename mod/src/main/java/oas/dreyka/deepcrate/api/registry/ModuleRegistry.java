package oas.dreyka.deepcrate.api.registry;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.api.Registrations;
import oas.dreyka.deepcrate.api.module.CrateModule;
import oas.dreyka.deepcrate.config.domain.CrateConfig;
import java.util.ArrayList;
import java.util.Collections;
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

    private static final List<CrateModule> MODULES = new ArrayList<>();

    private ModuleRegistry() {}

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
        return crateModule == null ? CrateConfig.baseCapacity : crateModule.capacity();
    }

    /**
     * The strongest capacity any of these stacks asks for. A crate whose cells hold two capacity
     * modules takes the better of the two rather than adding them, which is the rule one cell already
     * followed between two tags.
     */
    public static int capacityAmong(Iterable<ItemStack> stacks) {
        int capacity = CrateConfig.baseCapacity;
        for (ItemStack itemStack : stacks) {
            capacity = Math.max(capacity, capacityOf(itemStack));
        }

        return capacity;
    }
}
