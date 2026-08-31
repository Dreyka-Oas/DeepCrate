package com.dreykaoas.deepcrate.init;

import com.dreykaoas.deepcrate.DeepCrate;
import com.dreykaoas.deepcrate.api.DeepCrateAddon;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Runs the other mods that declare a {@code "deepcrate"} entry point, right after the six shipped
 * tiers are in place and before anything reads a registry.
 */
public final class AddonInit {
    private AddonInit() {}

    public static void register() {
        for (DeepCrateAddon addon : FabricLoader.getInstance().getEntrypoints(DeepCrate.MOD_ID, DeepCrateAddon.class)) {
            addon.onDeepCrateInit();
        }
    }
}
