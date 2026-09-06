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
    /**
     * Settings holders and clamp groups, and nothing else.
     *
     * It runs before the file is read, which is before a single registry is filled. Reaching
     * {@code RegistryInit} from here runs its class initialiser ahead of that read and freezes
     * {@code rowModuleStackLimit} at its default, so the option would look like it works and do
     * nothing.
     */
    default void onDeepCrateConfig() {}

    void onDeepCrateInit();
}
