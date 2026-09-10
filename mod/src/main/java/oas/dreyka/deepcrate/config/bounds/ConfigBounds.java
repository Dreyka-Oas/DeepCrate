package oas.dreyka.deepcrate.config.bounds;

import oas.dreyka.deepcrate.config.bounds.ConfigBoundsTable.Range;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import oas.dreyka.deepcrate.inventory.container.CrateStorage;

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
            return (int) clampToRange(whole, range);
        }
        if (value instanceof Long whole) {
            return (long) clampToRange(whole, range);
        }
        if (value instanceof Float fraction) {
            return isFinite(value) ? (float) clampToRange(fraction, range) : (float) range.min();
        }
        if (value instanceof Double fraction) {
            return isFinite(value) ? clampToRange(fraction, range) : range.min();
        }

        return value;
    }

    /** The one bound applied to every numeric kind, each caller only differing in how it casts back. */
    private static double clampToRange(double value, Range range) {
        return Math.max(range.min(), Math.min(value, range.max()));
    }

    /** True for anything that is not a non-finite float or double, which is every other value here. */
    private static boolean isFinite(Object value) {
        if (value instanceof Double fraction) {
            return Double.isFinite(fraction);
        }
        if (value instanceof Float fraction) {
            return Float.isFinite(fraction);
        }

        return true;
    }

    /**
     * The range an option is clamped to, or null for a boolean and for anything nothing bounds.
     *
     * Handed out as a {@link ConfigRange} rather than the table's own record: the table stays
     * package-private, and this class is the one door into it for reading as well as for clamping.
     */
    public static @Nullable ConfigRange rangeOf(String name) {
        Range range = ConfigBoundsTable.get(name);
        return range == null ? null : new ConfigRange(range.min(), range.max());
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
        if (!isFinite(value)) {
            throw new IllegalArgumentException("non-finite value for unbounded option " + name + ": " + value);
        }
    }
}
