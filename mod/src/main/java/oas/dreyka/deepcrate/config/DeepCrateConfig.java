package oas.dreyka.deepcrate.config;

import oas.dreyka.deepcrate.config.io.ConfigIo;
import oas.dreyka.deepcrate.config.access.ConfigRuntime;

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
        // Before the file is touched: right now the fields still hold their Java initialisers, and
        // those are the values the reset control puts back once the file has overwritten them.
        ConfigRuntime.captureDefaults();
        ConfigIo.load();
    }
}
