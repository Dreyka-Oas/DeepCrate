package com.dreykaoas.deepcrate.config;

import com.dreykaoas.deepcrate.config.bounds.CrateBounds;
import com.dreykaoas.deepcrate.config.bounds.ScreenBounds;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * Pure data: one range per option, keyed by lower-cased name to match how the schema finds a field.
 *
 * Nothing decides here. {@link ConfigBounds} looks a name up and applies what it finds, which is what
 * lets the table grow without the clamp growing with it.
 */
final class ConfigBoundsTable {
    private static final Map<String, Range> BOUNDS = new HashMap<>();

    static {
        CrateBounds.register(ConfigBoundsTable::b);
        ScreenBounds.register(ConfigBoundsTable::b);
    }

    private ConfigBoundsTable() {}

    record Range(double min, double max) {}

    /** Null for an option nothing bounds, which passes through the clamp untouched. */
    static @Nullable Range get(String name) {
        return BOUNDS.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * One more group, for a holder that joined the schema at run time.
     *
     * The mirror image of {@code ConfigSchema.registerHolder}: an option and its range always travel
     * together, so an addon that adds the first without the second gets no clamp at all.
     */
    static void registerGroup(Consumer<BoundsRegistrar> group) {
        group.accept(ConfigBoundsTable::b);
    }

    private static void b(String name, double min, double max) {
        BOUNDS.put(name.toLowerCase(Locale.ROOT), new Range(min, max));
    }
}
