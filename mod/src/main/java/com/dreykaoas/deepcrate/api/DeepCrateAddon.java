package com.dreykaoas.deepcrate.api;

/**
 * Entry point another mod declares under {@code "deepcrate"} in its {@code fabric.mod.json} to add
 * its own tiers and modules.
 *
 * It runs while DeepCrate itself initialises, before any registry is read, so a tier registered
 * here is indistinguishable from one of the six shipped tiers.
 */
@FunctionalInterface
public interface DeepCrateAddon {
    void onDeepCrateInit();
}
