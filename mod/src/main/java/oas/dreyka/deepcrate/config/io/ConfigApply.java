package oas.dreyka.deepcrate.config.io;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.config.ConfigAccess;
import oas.dreyka.deepcrate.config.schema.ConfigSchema;
import com.google.gson.JsonElement;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

/**
 * The flattened file onto the live options, one schema field at a time.
 *
 * The loop walks the fields and not the file, so it only ever looks at names the schema has, which is
 * why the structure check has to run first to notice anything the file carries and the schema does not.
 */
final class ConfigApply {
    private ConfigApply() {}

    static void applyAll(Map<String, JsonElement> values, Path path) {
        int applied = 0;
        int ignored = 0;
        for (Field option : ConfigSchema.all()) {
            JsonElement written = values.get(option.getName());
            if (written == null) {
                continue;
            }

            // One bad line must not cost every option after it: an object or a null where a number was
            // expected throws, and an exception escaping this loop would leave the rest at their
            // defaults, which the write below would then make permanent.
            if (!written.isJsonPrimitive() || !ConfigAccess.apply(option.getName(), written.getAsString())) {
                ignored++;
                continue;
            }

            applied++;
        }

        if (ignored > 0) {
            DeepCrate.LOGGER.warn(
                "[DeepCrate] settings read from {} ({} applied, {} IGNORED: wrong type or unreadable value)", path, applied, ignored
            );
            return;
        }

        DeepCrate.LOGGER.info("[DeepCrate] settings read from {} ({} options)", path, applied);
    }
}
