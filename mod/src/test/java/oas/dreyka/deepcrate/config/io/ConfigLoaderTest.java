package oas.dreyka.deepcrate.config.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oas.dreyka.deepcrate.config.domain.CrateConfig;
import oas.dreyka.deepcrate.config.domain.ScreenConfig;
import oas.dreyka.deepcrate.config.schema.ConfigSchema;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The read path, against a temporary directory. Reachable with no game running because the loader
 * takes its path: ConfigIo is the only class that asks FabricLoader where the file lives.
 */
class ConfigLoaderTest {
    @TempDir Path directory;

    private final Map<String, Object> held = new LinkedHashMap<>();

    /** The options are static fields, so a test leaving one changed decides what the next one reads. */
    @BeforeEach
    void hold() throws Exception {
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
    void anAbsentFileIsWrittenWithTheDefaults() throws Exception {
        Path file = this.directory.resolve("deepcrate.json");

        assertNull(ConfigLoader.load(file), "there was no file to have anything wrong with it");

        assertTrue(Files.exists(file));
        assertTrue(Files.readString(file).contains("baseCapacity"), "the fresh file must hold the schema");
        assertEquals(64, CrateConfig.baseCapacity);
    }

    @Test
    void aValueOutsideItsRangeIsClampedOnDiskToo() throws Exception {
        Path file = write("{\"crate\":{\"baseCapacity\":99999}}");

        ConfigLoader.load(file);

        assertEquals(32767, CrateConfig.baseCapacity);
        // Without this half the refused 99999 is still in the file and comes back at the next launch,
        // and the administrator watches a setting that never takes.
        assertTrue(Files.readString(file).contains("32767"));
    }

    @Test
    void anUnparseableFileIsSetAsideAndItsBytesSurvive() throws Exception {
        Path file = write("{ this is not json");

        assertNull(ConfigLoader.load(file), "what runs now is the defaults, so there is nothing to warn about");

        assertTrue(Files.exists(file), "a fresh default file must be written in its place");
        assertTrue(Files.readString(archive()).contains("this is not json"), "the archive must be the original bytes, not a rewrite");
        assertEquals(64, CrateConfig.baseCapacity);
    }

    @Test
    void aFileWhoseKeysAreAllStrangersIsSetAside() throws Exception {
        Path file = write("{\"Nonsense\": 1, \"AlsoNonsense\": 2}");

        assertNull(ConfigLoader.load(file));

        assertTrue(Files.readString(archive()).contains("AlsoNonsense"));
    }

    @Test
    void aMisspelledOptionIsReadAndRewrittenUnderItsRealName() throws Exception {
        Path file = write("{\"crate\":{\"baseCapcity\":128}}");

        ConfigLoader.load(file);

        assertEquals(128, CrateConfig.baseCapacity, "the value on the misspelled line must reach the real option");
        String rewritten = Files.readString(file);
        assertTrue(rewritten.contains("baseCapacity"));
        assertFalse(rewritten.contains("baseCapcity"), "the typo must be gone from the file");
    }

    @Test
    void anOptionTheFileDoesNotCarryComesBackWithItsDefault() throws Exception {
        Path file = write("{\"crate\":{\"baseCapacity\":128}}");

        ConfigLoader.load(file);

        assertEquals(999, ScreenConfig.abbreviateAbove);
        assertTrue(
            Files.readString(file).contains("abbreviateAbove"), "an option added by an update has to appear by itself, or nobody knows it exists"
        );
    }

    private Path write(String json) throws Exception {
        Path file = this.directory.resolve("deepcrate.json");
        Files.writeString(file, json);
        return file;
    }

    private Path archive() throws Exception {
        try (Stream<Path> listing = Files.list(this.directory)) {
            return listing.filter(path -> path.getFileName().toString().contains(".old-")).findFirst().orElseThrow();
        }
    }
}
