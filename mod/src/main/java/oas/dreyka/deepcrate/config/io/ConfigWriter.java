package oas.dreyka.deepcrate.config.io;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.config.schema.ConfigPrimitive;
import oas.dreyka.deepcrate.config.schema.ConfigSchema;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;

/** Every option back to disk, grouped under its category, written whole and moved into place. */
public final class ConfigWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigWriter() {}

    public static synchronized void save(Path path) {
        // Categories in alphabetical order, options in schema order inside one. The file is rewritten
        // at every launch, and an undecided order would produce a fresh diff every time, which teaches
        // people to stop reading the diffs.
        TreeMap<String, JsonObject> byCategory = new TreeMap<>();
        for (Field option : ConfigSchema.all()) {
            write(option, byCategory.computeIfAbsent(ConfigSchema.categoryOf(option), category -> new JsonObject()));
        }

        JsonObject file = new JsonObject();
        for (Map.Entry<String, JsonObject> entry : byCategory.entrySet()) {
            file.add(entry.getKey(), entry.getValue());
        }

        writeAtomically(path, GSON.toJson(file));
    }

    private static void write(Field option, JsonObject category) {
        try {
            switch (ConfigPrimitive.of(option.getType())) {
                case BOOL -> category.add(option.getName(), new JsonPrimitive(option.getBoolean(null)));
                case INT -> category.add(option.getName(), new JsonPrimitive(option.getInt(null)));
                case LONG -> category.add(option.getName(), new JsonPrimitive(option.getLong(null)));
                case DOUBLE -> category.add(option.getName(), new JsonPrimitive(option.getDouble(null)));
                case FLOAT -> category.add(option.getName(), new JsonPrimitive(option.getFloat(null)));
                case null -> { /* The schema never yields a field of another type. */ }
            }
        } catch (IllegalAccessException unreadable) {
            // Leaving it out makes the option vanish from the file and come back as a default on the
            // next load, so the administrator watches a setting disappear with no reason given.
            DeepCrate.LOGGER.error("[DeepCrate] option {} could not be written to disk", option.getName(), unreadable);
        }
    }

    /**
     * A temporary file then a rename, rather than a write in place.
     *
     * Writing over the real file empties it before the new content lands. That window is short, but a
     * full disk turns it into a certainty: the truncation works, the write does not, and the next
     * launch reads a half file. A rename is atomic, so a reader sees the old file or the new one.
     */
    private static void writeAtomically(Path path, String content) {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        boolean moved = false;
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temporary, content);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException notAtomic) {
                // Some filesystems refuse it outright. A plain replacement still beats a truncation.
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }

            moved = true;
        } catch (IOException failed) {
            DeepCrate.LOGGER.warn("[DeepCrate] settings could not be saved: {}", failed.toString());
        } finally {
            if (!moved) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // A stray .tmp costs nothing; the next save writes over it.
                }
            }
        }
    }
}
