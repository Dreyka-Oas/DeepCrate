package oas.dreyka.deepcrate.config.access;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.config.io.ConfigIo;
import oas.dreyka.deepcrate.config.schema.ConfigPrimitive;
import oas.dreyka.deepcrate.config.schema.ConfigSchema;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import oas.dreyka.deepcrate.config.bounds.ConfigBounds;

/**
 * The options as a list something can show and edit, over the same fields the file writes.
 *
 * Reading them through the schema rather than naming them keeps a new option out of this class
 * entirely: a field added to a holder appears in the snapshot, in the packet and on the screen with
 * nothing else written. Editing goes back out through {@link ConfigAccess}, so a value typed by a
 * player crosses exactly the parse and the clamp a value read from disk crosses.
 */
public final class ConfigRuntime {
    /**
     * Keyed lower-case, the way the schema and the bounds table both match a name.
     *
     * Filled on the boot thread and read from the server thread, so it is swapped in whole behind a
     * volatile field rather than filled in place: a map still being written is a map another thread
     * may see half of.
     */
    private static volatile Map<String, String> defaults = Map.of();

    private ConfigRuntime() {}

    /**
     * What the fields hold before the file is read, which is the only moment they hold their Java
     * initialisers.
     *
     * Once only: a reload runs {@code load()} again over values a player has already changed, and
     * capturing then would make the current value the default and leave the reset control with nothing
     * to reset to.
     */
    public static synchronized void captureDefaults() {
        if (!defaults.isEmpty()) {
            return;
        }

        Map<String, String> captured = new LinkedHashMap<>();
        for (Field option : ConfigSchema.all()) {
            String value = read(option);
            if (value != null) {
                captured.put(option.getName().toLowerCase(Locale.ROOT), value);
            }
        }

        defaults = Map.copyOf(captured);
    }

    /** Schema order, which is the order the file uses and the order the screen draws. */
    public static List<ConfigOption> snapshot() {
        List<ConfigOption> options = new ArrayList<>();
        for (Field option : ConfigSchema.all()) {
            String value = read(option);
            ConfigPrimitive kind = ConfigPrimitive.of(option.getType());
            if (value == null || kind == null) {
                continue;
            }

            options.add(new ConfigOption(
                option.getName(),
                ConfigSchema.categoryOf(option),
                kind,
                value,
                ConfigBounds.rangeOf(option.getName())
            ));
        }

        return options;
    }

    /**
     * False when the name is nothing the schema has or the value is something its type cannot hold,
     * exactly as a refused line in the file is false.
     *
     * Synchronised because two operators may edit at the same moment and the save rewrites the file
     * whole: interleaved, one write would carry half of what the other just applied.
     */
    public static synchronized boolean set(String name, String raw) {
        if (!ConfigAccess.apply(name, raw)) {
            return false;
        }

        // Only on success, so a refused edit never rewrites a file that has not changed.
        ConfigIo.save();
        return true;
    }

    /** Null for a name nothing knows, and for an option whose field could not be read at capture. */
    public static @Nullable String defaultOf(String name) {
        return defaults.get(name.toLowerCase(Locale.ROOT));
    }

    private static @Nullable String read(Field option) {
        try {
            return String.valueOf(option.get(null));
        } catch (IllegalAccessException unreadable) {
            DeepCrate.LOGGER.error("[DeepCrate] option {} could not be read", option.getName(), unreadable);
            return null;
        }
    }
}
