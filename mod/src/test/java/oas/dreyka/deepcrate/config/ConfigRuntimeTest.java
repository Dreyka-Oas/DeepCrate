package oas.dreyka.deepcrate.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oas.dreyka.deepcrate.config.domain.CrateConfig;
import oas.dreyka.deepcrate.config.schema.ConfigSchema;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The edit path, with no game running. The save inside {@code set} finds no config directory here and
 * says so in the log, which is exactly what it does on a machine whose disk refuses the write: the
 * value still lands on the field, and that is what these tests read.
 */
class ConfigRuntimeTest {
    private final Map<String, Object> held = new LinkedHashMap<>();

    /** The options are static fields, so a test leaving one changed decides what the next one reads. */
    @BeforeEach
    void hold() throws Exception {
        ConfigRuntime.captureDefaults();
        for (Field option : ConfigSchema.all()) {
            this.held.put(option.getName(), option.get(null));
        }
    }

    @AfterEach
    void putBack() throws Exception {
        for (Field option : ConfigSchema.all()) {
            option.set(null, this.held.get(option.getName()));
        }
    }

    @Test
    void everyOptionIsInTheSnapshotInSchemaOrder() {
        List<String> expected = new ArrayList<>();
        for (Field option : ConfigSchema.all()) {
            expected.add(option.getName());
        }

        List<String> snapshot = new ArrayList<>();
        for (ConfigOption configOption : ConfigRuntime.snapshot()) {
            snapshot.add(configOption.name());
        }

        // The screen draws them in the order they arrive, so a shuffle moves the row under the
        // player's cursor between one opening and the next.
        assertEquals(expected, snapshot);
    }

    @Test
    void aSnapshotCarriesTheCategoryTheValueAndTheRange() {
        ConfigOption configOption = ConfigRuntime.snapshot().getFirst();

        assertEquals("baseCapacity", configOption.name());
        assertEquals("crate", configOption.category());
        assertEquals(String.valueOf(CrateConfig.baseCapacity), configOption.value());
        assertEquals(new ConfigRange(1, 32767), configOption.range());
        assertEquals("deepcrate.option.base_capacity", configOption.labelKey());
    }

    @Test
    void aValueOutsideItsRangeComesBackClamped() {
        assertTrue(ConfigRuntime.set("baseCapacity", "99999"), "the line parses, so the edit is accepted and pulled back");

        assertEquals(32767, CrateConfig.baseCapacity);
        assertEquals("32767", ConfigRuntime.snapshot().getFirst().value());
    }

    @Test
    void aNameNothingKnowsIsRefused() {
        assertFalse(ConfigRuntime.set("crateColour", "blue"));
    }

    @Test
    void theDefaultIsTheJavaInitialiserAndNotWhatTheOptionHoldsNow() {
        assertTrue(ConfigRuntime.set("baseCapacity", "128"));

        assertEquals(128, CrateConfig.baseCapacity);
        // The reset control puts this back, so a default following the current value would leave it
        // resetting to whatever the player last typed.
        assertEquals("64", ConfigRuntime.defaultOf("baseCapacity"));
    }
}
