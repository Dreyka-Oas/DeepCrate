package com.dreykaoas.deepcrate.config;

import com.dreykaoas.deepcrate.config.ConfigBoundsTable.Range;
import java.util.function.Consumer;

/**
 * Pulling a value written by hand back into the range the code can survive.
 *
 * The schema is reflective and knows nothing about what a number means, so a file saying
 * {@code baseCapacity: 0} would reach {@code CrateStorage}'s constructor and take down the chunk of
 * every crate loaded after it. Clamping sits on the one road every value travels.
 */
public final class ConfigBounds {
    private ConfigBounds() {}

    /**
     * A value already inside its range comes back untouched, and so does a boolean or an option
     * nothing bounds.
     *
     * A non-finite double or float never gets through. With a range it is pulled to the lower end;
     * without one there is no in-range value to pick, so the assignment is refused and the field
     * keeps whatever it already held.
     */
    public static Object clamp(String name, Object value) {
        Range range = ConfigBoundsTable.get(name);
        if (range == null) {
            rejectNonFinite(name, value);
            return value;
        }
        if (value instanceof Integer whole) {
            return (int) Math.max(range.min(), Math.min((double) whole, range.max()));
        }
        if (value instanceof Long whole) {
            return (long) Math.max(range.min(), Math.min((double) whole, range.max()));
        }
        if (value instanceof Float fraction) {
            return Float.isFinite(fraction)
                ? (float) Math.max(range.min(), Math.min((double) fraction, range.max()))
                : (float) range.min();
        }
        if (value instanceof Double fraction) {
            return Double.isFinite(fraction) ? Math.max(range.min(), Math.min(fraction, range.max())) : range.min();
        }

        return value;
    }

    /**
     * Registers a bounds group defined outside this package, which an addon does alongside its holder.
     *
     * The table and its hook stay package-private on purpose; this is the one door into them.
     */
    public static void registerGroup(Consumer<BoundsRegistrar> group) {
        ConfigBoundsTable.registerGroup(group);
    }

    private static void rejectNonFinite(String name, Object value) {
        if (value instanceof Double fraction && !Double.isFinite(fraction)) {
            throw new IllegalArgumentException("non-finite value for unbounded option " + name + ": " + fraction);
        }
        if (value instanceof Float fraction && !Float.isFinite(fraction)) {
            throw new IllegalArgumentException("non-finite value for unbounded option " + name + ": " + fraction);
        }
    }
}
