package com.dreykaoas.deepcrate.config.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dreykaoas.deepcrate.config.schema.ConfigSchema;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigWriterTest {
    @TempDir Path directory;

    @Test
    void everyOptionReachesTheFileOnceUnderACategory() throws Exception {
        Path file = this.directory.resolve("deepcrate.json");

        ConfigWriter.save(file);

        List<String> written = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : JsonParser.parseString(Files.readString(file)).getAsJsonObject().entrySet()) {
            assertTrue(entry.getValue().isJsonObject(), entry.getKey() + " is written outside a category object");
            written.addAll(entry.getValue().getAsJsonObject().keySet());
        }

        List<String> expected = new ArrayList<>();
        for (Field option : ConfigSchema.all()) {
            expected.add(option.getName());
        }

        assertEquals(expected.size(), written.size(), "an option written twice or not at all: " + written);
        assertTrue(written.containsAll(expected), "missing from the file: " + written);
    }

    @Test
    void theTemporaryFileIsNotLeftBehind() throws Exception {
        Path file = this.directory.resolve("deepcrate.json");

        ConfigWriter.save(file);

        assertFalse(Files.exists(this.directory.resolve("deepcrate.json.tmp")), "the write goes through a temporary file and has to move it");
    }

    @Test
    void twoWritesInARowGiveTheSameBytes() throws Exception {
        Path first = this.directory.resolve("first.json");
        Path second = this.directory.resolve("second.json");

        ConfigWriter.save(first);
        ConfigWriter.save(second);

        // The file is rewritten at every launch. An undecided order would show a fresh diff every
        // time, which teaches people to stop reading the diffs.
        assertEquals(Files.readString(first), Files.readString(second));
    }
}
