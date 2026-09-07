package oas.dreyka.deepcrate.api.registry;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.api.Registrations;
import oas.dreyka.deepcrate.api.module.CrateModuleSlot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Module slot kinds a crate offers, kept in the order they are drawn. */
public final class ModuleSlotRegistry {
    private static final List<CrateModuleSlot> MODULE_SLOTS = new ArrayList<>();

    private ModuleSlotRegistry() {}

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
}
