package oas.dreyka.deepcrate.init;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.api.DeepCrateAddon;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Runs the other mods that declare a {@code "deepcrate"} entry point, in two passes: their settings
 * before the file is read, then their tiers and modules once the six shipped ones are in place and
 * before anything reads a registry.
 */
public final class AddonInit {
    /** Held between the two passes, so an addon keeping state across them is the same object twice. */
    private static List<DeepCrateAddon> addons = List.of();

    private AddonInit() {}

    public static void registerConfig() {
        addons = FabricLoader.getInstance().getEntrypoints(DeepCrate.MOD_ID, DeepCrateAddon.class);
        for (DeepCrateAddon addon : addons) {
            addon.onDeepCrateConfig();
        }
    }

    public static void register() {
        for (DeepCrateAddon addon : addons) {
            addon.onDeepCrateInit();
        }
    }
}
