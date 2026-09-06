package oas.dreyka.deepcrate.config;

/**
 * How a group in {@code config.bounds} hands its clamp ranges to the table.
 *
 * A callback rather than a shared map, so no group can read or overwrite another's entries, and the
 * table's own record stays out of sight.
 */
@FunctionalInterface
public interface BoundsRegistrar {
    /** An inclusive range for one option. Name matching is case-insensitive. */
    void b(String name, double min, double max);
}
