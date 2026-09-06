package oas.dreyka.deepcrate.config;

import oas.dreyka.deepcrate.config.io.ConfigIo;

/**
 * The settings, at {@code config/oas/deepcrate.json}.
 *
 * An option is a public static field on a holder under {@code config.domain}, enumerated by
 * reflection, so adding one is adding a field. Every holder has to be registered before this runs:
 * the read drops a name it does not recognise and the rewrite that follows deletes the line.
 */
public final class DeepCrateConfig {
    private DeepCrateConfig() {}

    public static void load() {
        ConfigIo.load();
    }
}
