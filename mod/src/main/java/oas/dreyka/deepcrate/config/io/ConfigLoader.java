package oas.dreyka.deepcrate.config.io;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.config.io.diag.ConfigDrift;
import oas.dreyka.deepcrate.config.io.diag.ConfigDriftReport;
import oas.dreyka.deepcrate.config.io.diag.ConfigQuarantine;
import oas.dreyka.deepcrate.config.io.diag.ConfigStructure;
import oas.dreyka.deepcrate.config.schema.ConfigSchema;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Reading the file into the live options, and saying what is wrong with its shape.
 *
 * The path arrives as a parameter rather than being resolved here, which is what makes the whole read
 * exercisable against a temporary directory with no game running.
 */
public final class ConfigLoader {
    private ConfigLoader() {}

    /** The structure report, or nothing when the file was absent or had to be set aside. */
    public static ConfigDrift.@Nullable Report load(Path path) {
        if (!Files.exists(path)) {
            DeepCrate.LOGGER.info("[DeepCrate] no settings file, writing the defaults to {}", path);
            ConfigWriter.save(path);
            return null;
        }

        ConfigDrift.Report report;
        try {
            JsonObject file = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            report = ConfigStructure.check(file, optionNames());
            if (report.unusable()) {
                if (ConfigQuarantine.moveAside(path, report.keysInFile() + " keys, none of them an option")) {
                    ConfigWriter.save(path);
                }

                // Deliberately not the report: it describes a file that has just been moved away. What
                // runs now is the plain defaults, so there is nothing for the operator notice to raise,
                // and grumbling all session about a file that no longer exists is worse than silence.
                return null;
            }

            ConfigDriftReport.emit(report, path);
            ConfigApply.applyAll(flatten(file, report), path);
        } catch (RuntimeException | IOException unreadable) {
            // Never fall through to a save here: what sits in memory is the defaults, and writing them
            // would destroy the settings at the very moment they failed to be read. Move the file
            // aside so its bytes survive, and only then write a fresh one.
            if (ConfigQuarantine.moveAside(path, unreadable.toString())) {
                ConfigWriter.save(path);
            }

            return null;
        }

        // The read worked, so the file is written back whole: an option added by an update appears by
        // itself with its default, and a value the clamp pulled back is now the value on disk.
        ConfigWriter.save(path);
        return report;
    }

    /**
     * Flattens the categories one level down, so an option written at the root and one written inside
     * its category resolve through the same loop. A name written both ways keeps the copy inside the
     * category, since that is where the file puts it.
     *
     * The misspellings the check settled move onto their real name here, before the apply. The apply
     * walks the schema, so without this the value would be dropped and the write would delete the line.
     */
    private static Map<String, JsonElement> flatten(JsonObject file, ConfigDrift.Report report) {
        Map<String, JsonElement> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : file.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                values.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, JsonElement> entry : file.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                for (Map.Entry<String, JsonElement> inner : entry.getValue().getAsJsonObject().entrySet()) {
                    values.put(inner.getKey(), inner.getValue());
                }
            }
        }

        for (ConfigDrift.Rename rename : report.renamed()) {
            JsonElement carried = values.remove(rename.from());
            if (carried != null) {
                values.put(rename.to(), carried);
            }
        }

        return values;
    }

    private static Set<String> optionNames() {
        Set<String> names = new HashSet<>();
        for (Field option : ConfigSchema.all()) {
            names.add(option.getName());
        }

        return names;
    }
}
